# Coverage reflection tooling

Three deterministic, **read-only** measurement tools, retrofitted from the
allegro-sdk seed (SEED-LESSONS §23–25). They exist because a facade-method
self-count lies: two facade methods over one endpoint self-count as two, a
wired-but-untested operation reads as "done", and "40/40 endpoints" says
nothing about how many *fields* the SDK actually maps.

**These are aids, never gates.** They do not run in the build, block a merge,
or gate a PR. They give an honest ratio so a session report can state the REAL
number instead of a green one — and they are the instrument for the coverage
push: run them, read the ABSENT lists, close them deliberately. They are
tracked project tooling (`tools/coverage/`), referenced from `CLAUDE.md`; they
are dev/CI scripts, not part of the published jar.

> Binding rule (fold into the coverage discipline): **measure at the OPERATION
> and FIELD level before you claim a bucket done, and state your REAL ratio in
> every session report.** Honesty over a green number.

Run everything from the repo root. All three need the project compiled first
(`./gradlew :ksef-client:compileJava` — the field tool reads bytecode, not
source).

---

## 1. `endpoint-coverage.py` — BREADTH (operation reachability)

Of the operations the KSeF OpenAPI spec declares, how many does the SDK
actually **drive through a passing test**?

```
python3 tools/coverage/endpoint-coverage.py            # full table
python3 tools/coverage/endpoint-coverage.py --absent   # hide OK rows
```

- **Denominator:** every `(METHOD, path)` in `ksef-client/openapi/open-api.json`.
- **DRIVEN** (authoritative): a WireMock stub/verify in `src/test` drives that
  `METHOD+path` through the SDK. A request the SDK provably sends implies the
  call site exists. Resolves test-side path constants and the server `/v2`
  prefix; resolves one-hop stub helpers (`stubGrant(PATH_X)`) **keeping the
  verb**, so a constant only ever passed to a GET helper never marks a POST op
  driven.
- **WIRED:** a `PATH_*` constant in `internal/client/*Impl` composes the path.
  Prefix-resolved and deliberately loose — its only job is to split UNTESTED
  (wired, no test) from ABSENT (no call site). It never promotes anything to OK.

Per op: **OK** (driven) / **UNTESTED** (wired, not driven) / **ABSENT** (neither).

**Precision:** trust OK. WIRED is prefix-based, so the UNTESTED/ABSENT split is
a prompt to look, not a verdict. Errs pessimistic (verb-precise DRIVEN under-
counts through indirection it cannot follow) rather than inflating OK.

_Last run: 77/78 driven (98%). The one gap — `POST /invoices/exports` — is
real: the export **start** is never exercised, only `GET /exports/{ref}`._

---

## 2. `field-coverage.py` — DEPTH (field completeness) — KSeF-specific

Operation reachability is **not** field completeness. The SDK can drive every
endpoint and still map a fraction of the fields each body carries. This is the
tool that would have caught the FA(3) line-item drop.

```
python3 tools/coverage/field-coverage.py fa3           # FA(3) invoice tree
python3 tools/coverage/field-coverage.py fa2           # FA(2) invoice tree
python3 tools/coverage/field-coverage.py rest          # OpenAPI *Raw types
python3 tools/coverage/field-coverage.py fa3 --all     # also list OK accessors
```

- **Denominator:** every public no-arg getter (`getX`/`isX`) on the model
  classes in scope, read with `javap` — **not grep**. Scoped by package so the
  ~2500-class UBL/PEF world stays out of the FA trees.
- **Numerator:** every `(ownerType, getter)` the compiled `ksef-client` SDK
  actually **invokes**, from `javap -c -p` disassembly (constant-pool method
  refs).
- **ABSENT = denominator − numerator:** model accessors the SDK never reads.
  Each line is an explicit keep/defer decision instead of a silent drop.

The **XML denominator (fa3/fa2) is the KSeF-specific part** — allegro is pure
REST/JSON; KSeF adds the JAXB invoice trees, and that is exactly where fields
go missing.

**Precision — UPPER BOUND.** The denominator counts every generated accessor,
including branches KSeF never populates and shared types over-counted across
contexts. A field reported ABSENT is a real un-read accessor; the *ratio* is a
ceiling. **The ABSENT list is the payload, not the percent.** Confirm the scope
package before trusting a number.

_Last run: FA(3) 58/319 leaves (18%), FA(2) 56/302 (18%), REST 275/644 (42%).
Validation: `getP11A` and `getDaneFaKorygowanej` now show OK — the tool
reflects the fix that closed the drop._

---

## 3. `live-e2e.sh` — live write→read (fail-closed)

Green WireMock is not merge proof: a mock asserts the author's *guess* of the
wire shape. This drives a real round trip through the SDK against
`api-demo.ksef.mf.gov.pl` — open session, **send** one FA(3) invoice, **read it
back** by its KSeF number — and proves the bytes survive both directions.

```
tools/coverage/live-e2e.sh            # preflight + confirm prompt
tools/coverage/live-e2e.sh --yes      # skip the prompt (CI / agent)
```

- **Fail-closed:** no demo-env credentials → **BLOCK (exit 3)**, a distinct
  outcome from a real failure. Sandbox unavailable is never silently a pass.
- **Write→read asserted:** a green run that skipped the send is not proof.
  Success requires the send marker **and** the read-back-by-KSeF-number marker,
  on top of the demo's own aggregate exit code.
- **Side effects acknowledged:** FULL sends a **real** invoice to the demo env
  and the server imposes a ~30–60s per-NIP session cooldown ("run once per
  NIP"). The send is gated behind explicit `--yes` / `KSEF_E2E_CONFIRM=1`.

Credentials come from `ksef-credentials.properties` (repo root, gitignored) or
`KSEF_NIP` + one of `KSEF_TOKEN` / `KSEF_TOKEN_READ` / `KSEF_CERT_PATH`.

Exit codes: `0` = write→read PROVEN, `1` = ran but not proven, `3` = BLOCKED.

Wraps the existing `ksef-demo` FULL runner — it adds the fail-closed
discipline, not a second copy of the flow.
