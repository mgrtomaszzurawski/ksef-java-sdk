#!/usr/bin/env python3
# Copyright (c) 2026 Tomasz Zurawski
# SPDX-License-Identifier: AGPL-3.0-only
#
# field-coverage.py -- DEPTH coverage reflection aid (read-only).
#
# Operation reachability (see endpoint-coverage.py) is NOT field completeness.
# The SDK can DRIVE every endpoint and still map only a fraction of the fields
# each request/response body carries. This tool measures that DEPTH: of the
# accessors the generated model tree exposes, how many does the SDK actually
# read?
#
# It is the tool that would have caught the FA(3) line-item drop: before the
# fix, the mapper read getP7/getP11/getP12 but never getP11A or
# getDaneFaKorygowanej, so those accessors sat in the denominator with zero
# call sites -- exactly the ABSENT-leaf list this prints.
#
# Denominator: every public no-arg getter (getX/isX) declared on the model
#   classes in scope, read from bytecode with `javap` -- NOT grep.
#     rest : OpenAPI *Raw types      (client.model)
#     fa3  : JAXB FA(3) invoice tree (xml.fa3)     <- KSeF-specific
#     fa2  : JAXB FA(2) invoice tree (xml.fa2)     <- KSeF-specific
#   Scoping by package keeps the ~2500-class UBL/PEF world out of the FA trees.
#
# Numerator: every (ownerType, getter) actually invoked anywhere in the
#   compiled ksef-client SDK, read from `javap -c -p` disassembly
#   (constant-pool Methodref / InterfaceMethodref lines).
#
# ABSENT = denominator - numerator: model accessors the SDK never reads. Each
#   is a keep/defer decision ("we map a subset"), made explicit instead of
#   silently dropped.
#
# UPPER BOUND: the denominator counts every generated accessor, including ones
#   for schema branches KSeF never populates and shared types over-counted
#   across contexts. A field reported ABSENT is a real un-read accessor; the
#   ratio itself is a ceiling. Confirm the scope package before trusting a
#   number, and read the ABSENT list -- that is the payload, not the percent.

import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
XML_CLASSES = REPO / "ksef-xml-models" / "build" / "classes" / "java" / "main"
REST_CLASSES = REPO / "ksef-rest-models" / "build" / "classes" / "java" / "main"
SDK_CLASSES = REPO / "ksef-client" / "build" / "classes" / "java" / "main"

XML_PREFIX = "io/github/mgrtomaszzurawski/ksef/xml/"
REST_PREFIX = "io/github/mgrtomaszzurawski/ksef/client/model/"

SCOPES = {
    "fa3": (XML_CLASSES / "io/github/mgrtomaszzurawski/ksef/xml/fa3", XML_PREFIX),
    "fa2": (XML_CLASSES / "io/github/mgrtomaszzurawski/ksef/xml/fa2", XML_PREFIX),
    "rest": (REST_CLASSES / "io/github/mgrtomaszzurawski/ksef/client/model",
             REST_PREFIX),
}

GETTER = ("get", "is")


def die(msg):
    print(msg, file=sys.stderr)
    sys.exit(2)


def javap(args, files):
    """Run javap over a batch of .class files, chunked to bound arg length."""
    out = []
    files = [str(f) for f in files]
    for i in range(0, len(files), 120):
        chunk = files[i:i + 120]
        result = subprocess.run(["javap", *args, *chunk],
                                capture_output=True, text=True)
        out.append(result.stdout)
    return "\n".join(out)


def binary_name(class_file, classes_root):
    rel = class_file.relative_to(classes_root).with_suffix("")
    return str(rel)  # already uses '/' and '$' for nested types


def parse_return_type(descriptor):
    """'()Ljava/math/BigDecimal;' -> 'java/math/BigDecimal'; 'I' -> 'int' etc."""
    ret = descriptor.split(")", 1)[1]
    if ret.startswith("L") and ret.endswith(";"):
        return ret[1:-1]
    prims = {"I": "int", "J": "long", "Z": "boolean", "D": "double",
             "F": "float", "S": "short", "B": "byte", "C": "char", "V": "void"}
    if ret.startswith("["):
        return ret + " (array)"
    return prims.get(ret, ret)


def collect_denominator(scope_dir, classes_root):
    """(binaryClass, getter) -> returnType for every public getter in scope."""
    class_files = sorted(scope_dir.rglob("*.class"))
    if not class_files:
        die(f"No compiled classes under {scope_dir}\n"
            f"Build first:  ./gradlew :ksef-client:compileJava")
    disasm = javap(["-public", "-s"], class_files)
    denom = {}
    current = None
    pending_name = None
    for line in disasm.splitlines():
        stripped = line.strip()
        header = parse_class_header(stripped)
        if header:
            current = header
            continue
        if current is None:
            continue
        method = parse_public_getter(stripped)
        if method:
            pending_name = method
            continue
        if pending_name and stripped.startswith("descriptor:"):
            desc = stripped.split("descriptor:", 1)[1].strip()
            if desc.startswith("()"):  # no-arg getter only
                denom[(current, pending_name)] = parse_return_type(desc)
            pending_name = None
    return denom


def parse_class_header(line):
    """'public class io.github...Faktura$Fa {' -> binary name, else None."""
    for kw in ("class ", "interface "):
        if kw in line and line.rstrip().endswith("{"):
            after = line.split(kw, 1)[1]
            name = after.split("<", 1)[0].split("{", 1)[0].strip()
            name = name.split(" extends ")[0].split(" implements ")[0].strip()
            if name.startswith("io.github.mgrtomaszzurawski"):
                return name.replace(".", "/")
    return None


def parse_public_getter(line):
    """A public no-arg getX()/isX() signature line -> getter name, else None."""
    if not line.endswith(");") or "(" not in line:
        return None
    head, args = line[:-2].rsplit("(", 1)
    if args.strip():  # has parameters -> not a getter
        return None
    tokens = head.split()
    if len(tokens) < 2 or "public" not in tokens:
        return None
    name = tokens[-1]
    if name.startswith(GETTER) and name not in ("get", "is") \
            and name[0].islower():
        base = name[3:] if name.startswith("get") else name[2:]
        if base and base[0].isupper():
            return name
    return None


def collect_numerator(model_prefix):
    """Set of (ownerBinaryClass, method) the SDK invokes on model types."""
    class_files = sorted(SDK_CLASSES.rglob("*.class"))
    if not class_files:
        die(f"No compiled SDK classes under {SDK_CLASSES}\n"
            f"Build first:  ./gradlew :ksef-client:compileJava")
    disasm = javap(["-c", "-p"], class_files)
    used = set()
    for line in disasm.splitlines():
        marker = "// Method "
        if marker not in line:
            marker = "// InterfaceMethod "
            if marker not in line:
                continue
        ref = line.split(marker, 1)[1].strip()
        if "." not in ref or ":" not in ref:
            continue
        owner, rest = ref.split(".", 1)
        method = rest.split(":", 1)[0]
        if owner.startswith(model_prefix) and method.startswith(GETTER):
            used.add((owner, method))
    return used


def short(binary):
    return binary.split("/")[-1]


def main():
    scope = next((a for a in sys.argv[1:] if not a.startswith("-")), "fa3")
    if scope not in SCOPES:
        die(f"scope must be one of {', '.join(SCOPES)} (got '{scope}')")
    show_all = "--all" in sys.argv
    scope_dir, model_prefix = SCOPES[scope]
    classes_root = XML_CLASSES if model_prefix == XML_PREFIX else REST_CLASSES

    denom = collect_denominator(scope_dir, classes_root)
    used = collect_numerator(model_prefix)

    rows = []
    for (cls, getter), ret in denom.items():
        is_branch = ret.startswith(model_prefix.rstrip("/")) or \
            ret == "java/util/List"
        covered = (cls, getter) in used
        rows.append((covered, is_branch, cls, getter, ret))

    covered_n = sum(1 for r in rows if r[0])
    total = len(rows)
    absent = sorted((r for r in rows if not r[0]),
                    key=lambda r: (r[2], r[3]))

    print(f"# DEPTH field coverage -- scope '{scope}' "
          f"({scope_dir.relative_to(REPO)})")
    print()
    for covered, is_branch, cls, getter, ret in absent:
        kind = "branch" if is_branch else "leaf"
        print(f"ABSENT  {kind:6} {short(cls)}.{getter}()  -> {short(ret)}")
    if show_all:
        print()
        for covered, is_branch, cls, getter, ret in rows:
            if covered:
                print(f"OK      {'branch' if is_branch else 'leaf':6} "
                      f"{short(cls)}.{getter}()")

    leaves = [r for r in rows if not r[1]]
    leaves_cov = sum(1 for r in leaves if r[0])
    print()
    pct = (100 * covered_n // total) if total else 0
    lpct = (100 * leaves_cov // len(leaves)) if leaves else 0
    print(f"DEPTH  scope '{scope}':  {covered_n}/{total} accessors read by the "
          f"SDK ({pct}%)  |  leaves {leaves_cov}/{len(leaves)} ({lpct}%)  |  "
          f"ABSENT {total - covered_n}")
    print("UPPER BOUND -- the ABSENT list is the payload, not the percent. "
          "Each line is a keep/defer decision. Reflection aid, not a gate.")


if __name__ == "__main__":
    main()
