#!/usr/bin/env python3
# Copyright (c) 2026 Tomasz Zurawski
# SPDX-License-Identifier: AGPL-3.0-only
#
# endpoint-coverage.py -- BREADTH coverage reflection aid (read-only).
#
# Answers ONE question honestly: of the operations the KSeF OpenAPI spec
# declares, how many does the SDK actually DRIVE through a passing test, how
# many are merely WIRED (a call site exists but no test exercises them), and
# how many are ABSENT (no call site at all)?
#
# It is a REFLECTION AID, never a merge gate. A facade-method self-count lies:
# two facade methods over one endpoint self-count as two, and a wired-but-
# untested op reads as "done". This tool measures at the OPERATION level so a
# session report can state the REAL ratio instead of a green number.
#
# Denominator: every (METHOD, path) in ksef-client/openapi/open-api.json.
# Numerator, two independent signals:
#   DRIVEN  -- a WireMock stub/verify URL in ksef-client/src/test drives that
#              METHOD+path through the SDK. This is the AUTHORITATIVE signal;
#              a request the SDK provably sent implies the call site exists.
#   WIRED   -- a call site in internal/client/*Impl composes a path (from the
#              ApiPaths + PATH_* constants) under that verb. Prefix-resolved,
#              so it is DELIBERATELY loose: its only job is to separate
#              UNTESTED (wired, no test) from ABSENT (not wired at all).
#
# Classification per spec op:
#   OK       -- DRIVEN (a passing test exercises it)
#   UNTESTED -- WIRED but not DRIVEN
#   ABSENT   -- neither
#
# Precision bounds (documented, not hidden):
#   * WIRED is prefix-based. A bare base constant used directly with a verb can
#     over-mark siblings as wired; conversely a path built entirely from a local
#     variable this resolver cannot follow can under-mark. Both only shift the
#     UNTESTED/ABSENT split, never OK. Trust OK; treat UNTESTED/ABSENT as a
#     prompt to look, not a verdict.
#   * Query strings are stripped before matching; the spec path is the unit.

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SPEC = REPO / "ksef-client" / "openapi" / "open-api.json"
CLIENT_MAIN = REPO / "ksef-client" / "src" / "main" / "java"
CLIENT_TEST = REPO / "ksef-client" / "src" / "test" / "java"
IMPL_DIR = CLIENT_MAIN / "io/github/mgrtomaszzurawski/ksef/sdk/internal/client"
APIPATHS = CLIENT_MAIN / ("io/github/mgrtomaszzurawski/ksef/sdk/internal/"
                          "runtime/transport/ApiPaths.java")

HTTP_METHODS = ("get", "post", "put", "delete", "patch")


def load_spec_ops():
    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    ops = []
    for path, methods in spec.get("paths", {}).items():
        for method, body in methods.items():
            if method.lower() in HTTP_METHODS:
                ops.append((method.upper(), path))
    return sorted(set(ops))


def op_regex(path):
    """Spec path template -> compiled regex matching a concrete request path."""
    escaped = re.escape(path)
    escaped = re.sub(r"\\\{[^/}]+\\\}", r"[^/]+", escaped)  # {param} -> segment
    return re.compile(r"^" + escaped + r"/?$")


def op_static_prefix(path):
    """The leading static portion of a spec path (up to the first {param})."""
    idx = path.find("{")
    return path if idx < 0 else path[:idx]


def resolve_apipaths_constants():
    """ApiPaths.NAME -> literal value, e.g. AUTH -> /auth."""
    const = {}
    text = APIPATHS.read_text(encoding="utf-8")
    for name, value in re.findall(
            r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]*)"', text):
        const[name] = value
    return const


def resolve_expr(expr, apipaths, local):
    """Resolve a Java string-concatenation expression to a static prefix.

    Understands: string literals, ApiPaths.NAME, bare PATH_* locals, and
    ApiPaths.subPath(base, segs...). Stops at the first token it cannot
    resolve and returns the static prefix accumulated so far (or None)."""
    expr = expr.strip()
    sub = re.match(r"ApiPaths\.subPath\((.*)\)$", expr, re.S)
    if sub:
        args = split_top_level_commas(sub.group(1))
        if not args:
            return None
        base = resolve_expr(args[0], apipaths, local)
        if base is None:
            return None
        out = base
        for seg in args[1:]:
            lit = re.match(r'"([^"]*)"$', seg.strip())
            if lit:
                out = out.rstrip("/") + "/" + lit.group(1)
            else:
                break  # dynamic segment -> prefix ends here
        return out
    parts = split_top_level_plus(expr)
    out = ""
    for part in parts:
        part = part.strip()
        lit = re.match(r'"([^"]*)"$', part)
        ap = re.match(r"ApiPaths\.(\w+)$", part)
        if lit:
            out += lit.group(1)
        elif ap and ap.group(1) in apipaths:
            out += apipaths[ap.group(1)]
        elif part in local:
            out += local[part]
        else:
            break  # unknown token -> prefix ends here
    return out or None


def split_top_level_commas(text):
    return _split_top_level(text, ",")


def split_top_level_plus(text):
    return _split_top_level(text, "+")


def _split_top_level(text, sep):
    out, depth, cur, in_str = [], 0, "", False
    i = 0
    while i < len(text):
        ch = text[i]
        if ch == '"' and (i == 0 or text[i - 1] != "\\"):
            in_str = not in_str
        if not in_str:
            if ch in "([":
                depth += 1
            elif ch in ")]":
                depth -= 1
            elif ch == sep and depth == 0:
                out.append(cur)
                cur = ""
                i += 1
                continue
        cur += ch
        i += 1
    if cur.strip():
        out.append(cur)
    return out


VERSION_PREFIX = re.compile(r"^/v\d+(?=/)")


def strip_version(path):
    """Test URLs carry the server '/v2' version segment; spec paths do not."""
    return VERSION_PREFIX.sub("", path)


def build_symbol_table(text, apipaths):
    """Resolve every String constant/local in a file to its static value.

    Handles forward references by iterating to a fixed point (a constant may
    be defined in terms of another declared later in the file)."""
    decls = re.findall(
        r'(?:static\s+final\s+String|String|var)\s+(\w+)\s*=\s*(.+?);',
        text, re.S)
    table = {}
    for _ in range(4):  # fixed point; depth of constant chains is shallow
        changed = False
        for name, expr in decls:
            if name in table:
                continue
            resolved = resolve_expr(expr, apipaths, table)
            if resolved:
                table[name] = resolved
                changed = True
        if not changed:
            break
    return table


def collect_wired(apipaths):
    """Set of resolved path templates reachable from any *Impl.

    Path-only (no verb): KSeF composes paths through helper methods that take
    the path as a parameter, so the verb is often lost at the http call site.
    WIRED only splits UNTESTED from ABSENT, so path granularity is enough."""
    wired = set()
    for java in IMPL_DIR.rglob("*.java"):
        text = java.read_text(encoding="utf-8")
        table = build_symbol_table(text, apipaths)
        for name, value in table.items():
            if name.startswith("PATH_") and value.startswith("/"):
                wired.add(value.rstrip("/"))
    return wired


MATCHER = re.compile(
    r"([A-Za-z]+)\s*\(\s*(?:WireMock\.)?"
    r"(url(?:Path)?(?:Equal|Matching)To)\s*\(")


def collect_driven(apipaths):
    """Set of (VERB, path) URLs each WireMock stub/verify drives through the SDK.

    Resolves identifier arguments against the test file's constant table
    (225 of 289 matchers reference constants, not literals) and strips the
    server '/v2' version prefix so paths align with the spec."""
    driven = set()
    for java in CLIENT_TEST.rglob("*.java"):
        text = java.read_text(encoding="utf-8")
        table = build_symbol_table(text, apipaths)
        for m in MATCHER.finditer(text):
            verb = verb_token(m.group(1))
            if verb is None:
                continue
            is_regex = "Matching" in m.group(2)
            arg = extract_balanced_arg(text, m.end())
            resolved = resolve_matcher_arg(arg, apipaths, table)
            if resolved:
                path = strip_version(resolved.split("?", 1)[0])
                driven.add((verb, path, is_regex))
        driven |= resolve_stub_helpers(text, apipaths, table)
    return driven


def resolve_stub_helpers(text, apipaths, table):
    """One-hop resolution of stub/verify helper methods.

    KSeF parametrises dispatch tests through helpers like
    ``stubGrantEndpoint(String path){ post(urlEqualTo(path)); }`` and calls
    them ``stubGrantEndpoint(PATH_SUBUNITS_GRANTS)``. The matcher argument is
    the unresolvable parameter, but the concrete path is at the call site --
    with the verb carried by the helper body. This keeps verb precision, so a
    constant only ever passed to a GET helper never marks a POST op driven."""
    helpers = {}  # name -> (paramIndex, VERB, is_regex)
    sig = re.compile(r"(?:private|public|protected|static|final|void|\s)+"
                     r"(\w+)\s*\(([^)]*)\)\s*\{")
    for msig in sig.finditer(text):
        name, params = msig.group(1), msig.group(2)
        param_names = [p.strip().split()[-1]
                       for p in params.split(",") if p.strip()]
        body = extract_block(text, msig.end())
        for m in MATCHER.finditer(body):
            verb = verb_token(m.group(1))
            if verb is None:
                continue
            arg = extract_balanced_arg(body, m.end()).strip()
            if arg in param_names:
                helpers[name] = (param_names.index(arg), verb,
                                 "Matching" in m.group(2))
                break
    resolved = set()
    for name, (idx, verb, is_regex) in helpers.items():
        for call in re.finditer(r"\b" + re.escape(name) + r"\s*\(", text):
            args_str = extract_balanced_arg_full(text, call.end())
            args = split_top_level_commas(args_str)
            if len(args) <= idx:
                continue
            value = resolve_matcher_arg(args[idx].strip(), apipaths, table)
            if value and value.startswith("/"):
                resolved.add((verb, strip_version(value.split("?", 1)[0]),
                              is_regex))
    return resolved


def extract_block(text, start):
    """Body of a method, balanced on braces, starting just after its '{'."""
    depth, i, in_str = 1, start, False
    while i < len(text) and depth > 0:
        ch = text[i]
        if ch == '"' and text[i - 1] != "\\":
            in_str = not in_str
        if not in_str:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
        i += 1
    return text[start:i - 1]


def extract_balanced_arg_full(text, start):
    """All arguments of a call, balanced on parens, starting after its '('."""
    depth, i, in_str = 1, start, False
    while i < len(text) and depth > 0:
        ch = text[i]
        if ch == '"' and text[i - 1] != "\\":
            in_str = not in_str
        if not in_str:
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    break
        i += 1
    return text[start:i]


def verb_token(token):
    low = token.lower().replace("requestedfor", "")
    for verb in ("get", "post", "put", "delete", "patch"):
        if low == verb:
            return verb.upper()
    return None


def resolve_matcher_arg(arg, apipaths, table):
    lit = re.match(r'\s*"([^"]*)"\s*$', arg)
    if lit:
        return lit.group(1)
    return resolve_expr(arg, apipaths, table)


def extract_balanced_arg(text, start):
    """First argument of the url*To(...) matcher, balanced on parens."""
    depth, i, in_str = 1, start, False
    while i < len(text) and depth > 0:
        ch = text[i]
        if ch == '"' and text[i - 1] != "\\":
            in_str = not in_str
        if not in_str:
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    break
        i += 1
    inner = text[start:i]
    return split_top_level_commas(inner)[0] if inner.strip() else ""


def driven_match(method, rx, static, driven):
    for verb, url, is_regex in driven:
        if verb != method:
            continue
        url = url.rstrip("/")
        if is_regex:
            # matcher arg is a regex; a static-prefix literal match is enough
            if static and url.startswith(static):
                return True
        elif rx.match(url) or url == static:
            return True
    return False


def wired_match(static, wired):
    for path in wired:
        if static == path or static.startswith(path + "/") or \
                (static and path.startswith(static + "/")) or path == static:
            return True
    return False


def classify(ops, driven, wired):
    rows = []
    for method, path in ops:
        rx = op_regex(path)
        static = op_static_prefix(path).rstrip("/")
        if driven_match(method, rx, static, driven):
            state = "OK"
        elif wired_match(static, wired):
            state = "UNTESTED"
        else:
            state = "ABSENT"
        rows.append((state, method, path))
    return rows


def main():
    ops = load_spec_ops()
    apipaths = resolve_apipaths_constants()
    driven = collect_driven(apipaths)
    wired = collect_wired(apipaths)
    rows = classify(ops, driven, wired)

    order = {"OK": 0, "UNTESTED": 1, "ABSENT": 2}
    rows.sort(key=lambda r: (order[r[0]], r[1], r[2]))
    counts = {"OK": 0, "UNTESTED": 0, "ABSENT": 0}
    for state, _m, _p in rows:
        counts[state] += 1

    want_absent = "--absent" in sys.argv
    for state, method, path in rows:
        if want_absent and state == "OK":
            continue
        print(f"{state:9} {method:6} {path}")

    total = len(rows)
    ok = counts["OK"]
    print()
    print(f"BREADTH  {ok}/{total} operations DRIVEN by a test "
          f"({100 * ok // total if total else 0}%)  |  "
          f"UNTESTED {counts['UNTESTED']}  ABSENT {counts['ABSENT']}")
    print("Reflection aid, not a gate. Trust OK; UNTESTED/ABSENT is a prompt "
          "to look (WIRED is prefix-resolved, see header).")


if __name__ == "__main__":
    main()
