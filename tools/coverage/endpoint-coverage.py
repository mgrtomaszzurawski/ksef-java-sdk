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

# Constant chains in a source file are shallow; a handful of resolution passes
# reaches a fixed point even with forward references.
MAX_RESOLUTION_PASSES = 4
PERCENT_SCALE = 100


def load_spec_ops():
    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    operations = []
    for path, methods in spec.get("paths", {}).items():
        for method, body in methods.items():
            if method.lower() in HTTP_METHODS:
                operations.append((method.upper(), path))
    return sorted(set(operations))


def op_regex(path):
    """Spec path template -> compiled regex matching a concrete request path."""
    escaped = re.escape(path)
    escaped = re.sub(r"\\\{[^/}]+\\\}", r"[^/]+", escaped)  # {param} -> segment
    return re.compile(r"^" + escaped + r"/?$")


def op_static_prefix(path):
    """The leading static portion of a spec path (up to the first {param})."""
    brace_index = path.find("{")
    return path if brace_index < 0 else path[:brace_index]


def resolve_apipaths_constants():
    """ApiPaths.NAME -> literal value, e.g. AUTH -> /auth."""
    constants = {}
    text = APIPATHS.read_text(encoding="utf-8")
    for name, value in re.findall(
            r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]*)"', text):
        constants[name] = value
    return constants


def resolve_expr(expression, apipaths, symbols):
    """Resolve a Java string-concatenation expression to a static prefix.

    Understands: string literals, ApiPaths.NAME, bare PATH_* locals, and
    ApiPaths.subPath(base, segs...). Stops at the first token it cannot
    resolve and returns the static prefix accumulated so far (or None)."""
    expression = expression.strip()
    sub_call = re.match(r"ApiPaths\.subPath\((.*)\)$", expression, re.S)
    if sub_call:
        arguments = split_top_level_commas(sub_call.group(1))
        if not arguments:
            return None
        base = resolve_expr(arguments[0], apipaths, symbols)
        if base is None:
            return None
        resolved = base
        for segment in arguments[1:]:
            literal = re.match(r'"([^"]*)"$', segment.strip())
            if literal:
                resolved = resolved.rstrip("/") + "/" + literal.group(1)
            else:
                break  # dynamic segment -> prefix ends here
        return resolved
    resolved = ""
    for part in split_top_level_plus(expression):
        part = part.strip()
        literal = re.match(r'"([^"]*)"$', part)
        apipaths_ref = re.match(r"ApiPaths\.(\w+)$", part)
        if literal:
            resolved += literal.group(1)
        elif apipaths_ref and apipaths_ref.group(1) in apipaths:
            resolved += apipaths[apipaths_ref.group(1)]
        elif part in symbols:
            resolved += symbols[part]
        else:
            break  # unknown token -> prefix ends here
    return resolved or None


def split_top_level_commas(text):
    return _split_top_level(text, ",")


def split_top_level_plus(text):
    return _split_top_level(text, "+")


def _split_top_level(text, separator):
    segments, depth, current, in_string = [], 0, "", False
    index = 0
    while index < len(text):
        character = text[index]
        if character == '"' and (index == 0 or text[index - 1] != "\\"):
            in_string = not in_string
        if not in_string:
            if character in "([":
                depth += 1
            elif character in ")]":
                depth -= 1
            elif character == separator and depth == 0:
                segments.append(current)
                current = ""
                index += 1
                continue
        current += character
        index += 1
    if current.strip():
        segments.append(current)
    return segments


def scan_balanced(text, start, opener, closer):
    """Substring from `start` up to the closer that balances an already-consumed
    opener (exclusive of that closer). String literals are skipped."""
    depth, index, in_string = 1, start, False
    while index < len(text):
        character = text[index]
        if character == '"' and text[index - 1] != "\\":
            in_string = not in_string
        if not in_string:
            if character == opener:
                depth += 1
            elif character == closer:
                depth -= 1
                if depth == 0:
                    break
        index += 1
    return text[start:index]


def first_matcher_argument(text, start):
    """First argument of a url*To(...) matcher, balanced on parens."""
    inner = scan_balanced(text, start, "(", ")")
    return split_top_level_commas(inner)[0] if inner.strip() else ""


VERSION_PREFIX = re.compile(r"^/v\d+(?=/)")


def strip_version(path):
    """Test URLs carry the server '/v2' version segment; spec paths do not."""
    return VERSION_PREFIX.sub("", path)


def build_symbol_table(text, apipaths):
    """Resolve every String constant/local in a file to its static value.

    Handles forward references by iterating to a fixed point (a constant may
    be defined in terms of another declared later in the file)."""
    declarations = re.findall(
        r'(?:static\s+final\s+String|String|var)\s+(\w+)\s*=\s*(.+?);',
        text, re.S)
    symbols = {}
    for _pass in range(MAX_RESOLUTION_PASSES):
        changed = False
        for name, expression in declarations:
            if name in symbols:
                continue
            resolved = resolve_expr(expression, apipaths, symbols)
            if resolved:
                symbols[name] = resolved
                changed = True
        if not changed:
            break
    return symbols


def collect_wired(apipaths):
    """Set of resolved path templates reachable from any *Impl.

    Path-only (no verb): KSeF composes paths through helper methods that take
    the path as a parameter, so the verb is often lost at the http call site.
    WIRED only splits UNTESTED from ABSENT, so path granularity is enough."""
    wired = set()
    for java_file in IMPL_DIR.rglob("*.java"):
        text = java_file.read_text(encoding="utf-8")
        symbols = build_symbol_table(text, apipaths)
        for name, value in symbols.items():
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
    for java_file in CLIENT_TEST.rglob("*.java"):
        text = java_file.read_text(encoding="utf-8")
        symbols = build_symbol_table(text, apipaths)
        for match in MATCHER.finditer(text):
            verb = verb_token(match.group(1))
            if verb is None:
                continue
            is_regex = "Matching" in match.group(2)
            argument = first_matcher_argument(text, match.end())
            resolved = resolve_matcher_arg(argument, apipaths, symbols)
            if resolved:
                path = strip_version(resolved.split("?", 1)[0])
                driven.add((verb, path, is_regex))
        driven |= resolve_stub_helpers(text, apipaths, symbols)
    return driven


def resolve_stub_helpers(text, apipaths, symbols):
    """One-hop resolution of stub/verify helper methods.

    KSeF parametrises dispatch tests through helpers like
    ``stubGrantEndpoint(String path){ post(urlEqualTo(path)); }`` and calls
    them ``stubGrantEndpoint(PATH_SUBUNITS_GRANTS)``. The matcher argument is
    the unresolvable parameter, but the concrete path is at the call site --
    with the verb carried by the helper body. This keeps verb precision, so a
    constant only ever passed to a GET helper never marks a POST op driven."""
    helpers = {}  # name -> (paramIndex, VERB, is_regex)
    signature_re = re.compile(
        r"(?:private|public|protected|static|final|void|\s)+"
        r"(\w+)\s*\(([^)]*)\)\s*\{")
    for method_match in signature_re.finditer(text):
        name, params = method_match.group(1), method_match.group(2)
        param_names = [param.strip().split()[-1]
                       for param in params.split(",") if param.strip()]
        body = scan_balanced(text, method_match.end(), "{", "}")
        for match in MATCHER.finditer(body):
            verb = verb_token(match.group(1))
            if verb is None:
                continue
            argument = first_matcher_argument(body, match.end()).strip()
            if argument in param_names:
                helpers[name] = (param_names.index(argument), verb,
                                 "Matching" in match.group(2))
                break
    resolved = set()
    for name, (param_index, verb, is_regex) in helpers.items():
        for call_match in re.finditer(r"\b" + re.escape(name) + r"\s*\(", text):
            call_args = split_top_level_commas(
                scan_balanced(text, call_match.end(), "(", ")"))
            if len(call_args) <= param_index:
                continue
            value = resolve_matcher_arg(call_args[param_index].strip(),
                                        apipaths, symbols)
            if value and value.startswith("/"):
                resolved.add((verb, strip_version(value.split("?", 1)[0]),
                              is_regex))
    return resolved


def verb_token(token):
    lowered = token.lower().replace("requestedfor", "")
    for verb in ("get", "post", "put", "delete", "patch"):
        if lowered == verb:
            return verb.upper()
    return None


def resolve_matcher_arg(argument, apipaths, symbols):
    literal = re.match(r'\s*"([^"]*)"\s*$', argument)
    if literal:
        return literal.group(1)
    return resolve_expr(argument, apipaths, symbols)


def driven_match(method, op_pattern, static, driven):
    for verb, request_url, is_regex in driven:
        if verb != method:
            continue
        request_url = request_url.rstrip("/")
        if is_regex:
            # matcher arg is a regex; a static-prefix literal match is enough
            if static and request_url.startswith(static):
                return True
        elif op_pattern.match(request_url) or request_url == static:
            return True
    return False


def wired_match(static, wired):
    for path in wired:
        if static == path or static.startswith(path + "/") or \
                (static and path.startswith(static + "/")) or path == static:
            return True
    return False


def classify(operations, driven, wired):
    rows = []
    for method, path in operations:
        op_pattern = op_regex(path)
        static = op_static_prefix(path).rstrip("/")
        if driven_match(method, op_pattern, static, driven):
            state = "OK"
        elif wired_match(static, wired):
            state = "UNTESTED"
        else:
            state = "ABSENT"
        rows.append((state, method, path))
    return rows


def main():
    operations = load_spec_ops()
    apipaths = resolve_apipaths_constants()
    driven = collect_driven(apipaths)
    wired = collect_wired(apipaths)
    rows = classify(operations, driven, wired)

    order = {"OK": 0, "UNTESTED": 1, "ABSENT": 2}
    rows.sort(key=lambda row: (order[row[0]], row[1], row[2]))
    counts = {"OK": 0, "UNTESTED": 0, "ABSENT": 0}
    for state, _method, _path in rows:
        counts[state] += 1

    hide_ok = "--absent" in sys.argv
    for state, method, path in rows:
        if hide_ok and state == "OK":
            continue
        print(f"{state:9} {method:6} {path}")

    total = len(rows)
    ok_count = counts["OK"]
    print()
    percent = (PERCENT_SCALE * ok_count // total) if total else 0
    print(f"BREADTH  {ok_count}/{total} operations DRIVEN by a test "
          f"({percent}%)  |  "
          f"UNTESTED {counts['UNTESTED']}  ABSENT {counts['ABSENT']}")
    print("Reflection aid, not a gate. Trust OK; UNTESTED/ABSENT is a prompt "
          "to look (WIRED is prefix-resolved, see header).")


if __name__ == "__main__":
    main()
