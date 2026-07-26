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

GETTER_PREFIXES = ("get", "is")

# javap accepts many class files per invocation; batch to bound the arg list.
JAVAP_BATCH_SIZE = 120
PERCENT_SCALE = 100


def die(message):
    print(message, file=sys.stderr)
    sys.exit(2)


def run_javap(options, class_files):
    """Run javap over a batch of .class files; fail loudly on any javap error."""
    disassembly_chunks = []
    paths = [str(class_file) for class_file in class_files]
    for offset in range(0, len(paths), JAVAP_BATCH_SIZE):
        batch = paths[offset:offset + JAVAP_BATCH_SIZE]
        completed = subprocess.run(["javap", *options, *batch],
                                   capture_output=True, text=True)
        if completed.returncode != 0:
            die(f"javap failed (exit {completed.returncode}); coverage would be "
                f"understated. First error:\n{completed.stderr.strip()[:500]}")
        disassembly_chunks.append(completed.stdout)
    return "\n".join(disassembly_chunks)


def parse_return_type(descriptor):
    """'()Ljava/math/BigDecimal;' -> 'java/math/BigDecimal'; 'I' -> 'int' etc."""
    return_token = descriptor.split(")", 1)[1]
    if return_token.startswith("L") and return_token.endswith(";"):
        return return_token[1:-1]
    primitives = {"I": "int", "J": "long", "Z": "boolean", "D": "double",
                  "F": "float", "S": "short", "B": "byte", "C": "char",
                  "V": "void"}
    if return_token.startswith("["):
        return return_token + " (array)"
    return primitives.get(return_token, return_token)


def collect_denominator(scope_dir):
    """(binaryClass, getter) -> returnType for every public getter in scope."""
    class_files = sorted(scope_dir.rglob("*.class"))
    if not class_files:
        die(f"No compiled classes under {scope_dir}\n"
            f"Build first:  ./gradlew :ksef-client:compileJava")
    disassembly = run_javap(["-public", "-s"], class_files)
    getters = {}
    current_class = None
    pending_getter = None
    for raw_line in disassembly.splitlines():
        line = raw_line.strip()
        header = parse_class_header(line)
        if header:
            current_class = header
            continue
        if current_class is None:
            continue
        getter = parse_public_getter(line)
        if getter:
            pending_getter = getter
            continue
        if pending_getter and line.startswith("descriptor:"):
            descriptor = line.split("descriptor:", 1)[1].strip()
            if descriptor.startswith("()"):  # no-arg getter only
                getters[(current_class, pending_getter)] = \
                    parse_return_type(descriptor)
            pending_getter = None
    return getters


def parse_class_header(line):
    """'public class io.github...Faktura$Fa {' -> binary name, else None."""
    for keyword in ("class ", "interface "):
        if keyword in line and line.rstrip().endswith("{"):
            declared = line.split(keyword, 1)[1]
            name = declared.split("<", 1)[0].split("{", 1)[0].strip()
            name = name.split(" extends ")[0].split(" implements ")[0].strip()
            if name.startswith("io.github.mgrtomaszzurawski"):
                return name.replace(".", "/")
    return None


def parse_public_getter(line):
    """A public no-arg getX()/isX() signature line -> getter name, else None."""
    if not line.endswith(");") or "(" not in line:
        return None
    signature, params = line[:-2].rsplit("(", 1)
    if params.strip():  # has parameters -> not a getter
        return None
    tokens = signature.split()
    if len(tokens) < 2 or "public" not in tokens:
        return None
    name = tokens[-1]
    if name.startswith(GETTER_PREFIXES) and name not in GETTER_PREFIXES \
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
    disassembly = run_javap(["-c", "-p"], class_files)
    invoked = set()
    for line in disassembly.splitlines():
        marker = "// Method "
        if marker not in line:
            marker = "// InterfaceMethod "
            if marker not in line:
                continue
        reference = line.split(marker, 1)[1].strip()
        if "." not in reference or ":" not in reference:
            continue
        owner, remainder = reference.split(".", 1)
        method = remainder.split(":", 1)[0]
        if owner.startswith(model_prefix) and method.startswith(GETTER_PREFIXES):
            invoked.add((owner, method))
    return invoked


def simple_name(binary_name):
    return binary_name.split("/")[-1]


def main():
    scope = next((token for token in sys.argv[1:]
                  if not token.startswith("-")), "fa3")
    if scope not in SCOPES:
        die(f"scope must be one of {', '.join(SCOPES)} (got '{scope}')")
    show_all = "--all" in sys.argv
    scope_dir, model_prefix = SCOPES[scope]

    denominator = collect_denominator(scope_dir)
    invoked = collect_numerator(model_prefix)

    rows = []
    for (owner, getter), return_type in denominator.items():
        is_branch = return_type.startswith(model_prefix.rstrip("/")) or \
            return_type == "java/util/List"
        is_covered = (owner, getter) in invoked
        rows.append((is_covered, is_branch, owner, getter, return_type))

    covered_count = sum(1 for row in rows if row[0])
    total = len(rows)
    absent = sorted((row for row in rows if not row[0]),
                    key=lambda row: (row[2], row[3]))

    print(f"# DEPTH field coverage -- scope '{scope}' "
          f"({scope_dir.relative_to(REPO)})")
    print()
    for _covered, is_branch, owner, getter, return_type in absent:
        kind = "branch" if is_branch else "leaf"
        print(f"ABSENT  {kind:6} {simple_name(owner)}.{getter}()  "
              f"-> {simple_name(return_type)}")
    if show_all:
        print()
        for is_covered, is_branch, owner, getter, _return_type in rows:
            if is_covered:
                print(f"OK      {'branch' if is_branch else 'leaf':6} "
                      f"{simple_name(owner)}.{getter}()")

    leaves = [row for row in rows if not row[1]]
    covered_leaves = sum(1 for row in leaves if row[0])
    print()
    percent = (PERCENT_SCALE * covered_count // total) if total else 0
    leaf_percent = (PERCENT_SCALE * covered_leaves // len(leaves)) \
        if leaves else 0
    print(f"DEPTH  scope '{scope}':  {covered_count}/{total} accessors read by "
          f"the SDK ({percent}%)  |  leaves {covered_leaves}/{len(leaves)} "
          f"({leaf_percent}%)  |  ABSENT {total - covered_count}")
    print("UPPER BOUND -- the ABSENT list is the payload, not the percent. "
          "Each line is a keep/defer decision. Reflection aid, not a gate.")


if __name__ == "__main__":
    main()
