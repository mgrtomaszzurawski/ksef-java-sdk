# `ksef-demo` — live-validation runner

Dev-only Maven module that exercises every domain of the SDK against
the live KSeF demo and test environments. Each domain has a runner
(`runner/SecurityRunner`, `runner/AuthRunner`, `runner/PermissionRunner`,
…) that reports per-operation results to the console.

This module is not published to Maven Central
(`maven.deploy.skip=true`). It is the SDK's own integration harness.

## Running

The demo expects to be invoked from the **repo root** (the working
directory the Maven exec plugin uses by default for multi-module
projects). It reads `ksef-credentials.properties` from that root and
writes its run-state to `demo-state.json` in the same directory; both
files are gitignored.

```bash
# 1. Install the SDK module locally (one-time, or after each ksef-client edit).
mvn install -pl ksef-client -DskipTests -Dmaven.javadoc.skip=true

# 2. Make sure ksef-credentials.properties exists at the repo root
#    (see "Credentials file" below).

# 3. Run a demo mode:
mvn exec:java -pl ksef-demo -Ddemo.mode=AUTH_SAFE
```

## Modes

`DemoMode` controls which runners execute and which write operations
they exercise:

| Mode | What it does | Side effects |
|------|---|---|
| `READ_ONLY` | Unauthenticated operations only: `SecurityClient`, `QrCodeService`, `TestDataClient` (create+remove pairs). | None on PROD-like state. |
| `AUTH_SAFE` | Authenticate + every read query + reversible writes (token generate+revoke, permission grant+revoke, etc.). No invoice send. | Reverses every write before exit. |
| `FULL` | Authenticate + open session + send one invoice + close + poll UPO + export + everything `AUTH_SAFE` does. Run once per NIP. | Files a real legally-binding invoice on the target environment. |
| `CLEANUP` | Read `demo-state.json`, revoke orphaned tokens / permissions / certificates from previous runs, reset test-data limits. | Reverses any artefacts a prior crashed run left behind. |

Default mode if `-Ddemo.mode=` is omitted: `READ_ONLY`.

## Credentials file

Drop a `ksef-credentials.properties` at the repo root with at least:

```properties
# Required for token-based auth (most modes)
ksef.token=YOUR_PRE_ISSUED_KSEF_TOKEN
ksef.nip=1234567890
ksef.environment=https://api-demo.ksef.mf.gov.pl

# Optional — enables PKCS#12 / cert-based auth (required for the
# certificate-domain runners). Path is resolved relative to repo root.
ksef.cert.file=ksef-demo.p12
ksef.cert.password=YOUR_KEYSTORE_PASSWORD

# Optional — enables the test-env pass on FULL mode (TEST env probes
# form-code coverage that DEMO/PROD cannot run with token-context creds).
ksef.test.environment=https://api-test.ksef.mf.gov.pl
```

The runner reads these via `AppProperties` and maps them onto
`KsefTokenCredentials` (default) or `KsefPkcs12Credentials` (when
`ksef.cert.file` is present).

`.gitignore` excludes the file pattern; do not commit credentials.

## Certificate-domain runs

Certificate enrolment + revocation is gated separately because each KSeF
taxpayer NIP is capped at **12 enrolments per month, 6 active at a time**.
The `CertificateRunner` skips writes by default and runs only the
read-side probes (limits, query, enrollment-data).

To exercise enrol/revoke:

```bash
mvn exec:java -pl ksef-demo -Ddemo.mode=AUTH_SAFE -Ddemo.cert.test=true
```

Each run with `demo.cert.test=true` consumes one slot of the 12/month
quota. The runner reports remaining quota at start.

## Test-environment pass (`FULL` only)

When `ksef.test.environment` is set and the mode is `FULL`, the runner
opens a second pass against `api-test.ksef.mf.gov.pl` with a
self-signed certificate and a freshly-generated NIP. This covers the
form-code/auth-context combinations that the primary DEMO/PROD pass
cannot exercise with a real-NIP token (e.g. `FA(2)` with
NIP-context auth).

The randomised NIP and self-signed certificate are throw-away — KSeF
TEST auto-creates contexts on first XAdES auth signed by a CN that
matches the target identifier.

## State file (`demo-state.json`)

Tracks artefacts created by `AUTH_SAFE` and `FULL` runs that the runner
expects to clean up on the same process exit (tokens, permission grants,
certificate enrolments). On crash mid-run the file persists so that a
later `CLEANUP` mode run can finish the cleanup.

The file lives at the repo root next to `ksef-credentials.properties`
and is gitignored.

## Other entry points

- `ValidationProbe` — `mvn exec:java -pl ksef-demo
  -Dexec.mainClass=...sample.ValidationProbe` runs targeted spec-conformance
  probes against the server.
- `CertProbe` — `mvn exec:java -pl ksef-demo
  -Dexec.mainClass=...sample.CertProbe` exercises the cert-domain
  endpoints in isolation (still subject to the monthly quota).

## Troubleshooting

- **`Missing required property: ksef.token`** — the credentials file
  isn't at the working directory. Run from the repo root, or pass
  `-Dexec.workingdir=...`.
- **`KsefSessionCooldownException` on session open** — KSeF enforces a
  ~30–60 s cooldown after a previous online session terminates for the
  same NIP. Wait, then retry.
- **`KsefRateLimitException`** — environment per-NIP rate limit. Backoff
  per the `Retry-After` header; the SDK already retries 429 by default
  if `RetryPolicy.retryOn429` is on.
- **`certificates/enrollments` HTTP 500** — invalid `certificateType`
  enum. Use the typed `CertificateEnrollBuilder`; do not bypass it.

For server quirks beyond the demo runner see
[`KNOWN-SERVER-BEHAVIORS.md`](../KNOWN-SERVER-BEHAVIORS.md) at the repo
root.
