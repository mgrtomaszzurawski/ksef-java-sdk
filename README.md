# KSeF Java SDK

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)]()

OpenAPI-first Java SDK for the Polish National e-Invoicing System ([KSeF](https://www.podatki.gov.pl/ksef/)) REST API v2.

Generated from the official [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs) OpenAPI specification, with a hand-written ergonomic overlay that hides the protocol details (challenge-response auth, AES-256-CBC session encryption, XAdES-BASELINE-B signing, retry-with-backoff, batch upload + polling).

> **Status:** SDK has reached `1.0.0` source-tree readiness. Maven Central
> publication is gated on a clean release-profile dry run; until the artifact
> is staged on Central, install locally with `mvn install` to use it.

## What you can do

- Send invoices online, one at a time (`KsefSession`)
- Send invoices in batch via async ZIP packages (`KsefBatchSession`)
- Query invoice metadata + filter, lazily paginated
  (`streamInvoicesByMetadata`)
- Download invoice content + UPO by KSeF number
- Incremental sync of newly-permanently-stored invoices using a HWM
  cursor (`InvoiceSyncClient`)
- Manage permissions, KSeF tokens, certificates (enrol / revoke / query)
- Generate KOD I + KOD II invoice-verification QR codes
- Authenticate as: NIP / EU-entity (VAT-UE) / Peppol provider / sub-unit
  via tokens, raw certificates, or PKCS#12 keystores (XAdES-BASELINE-B)

See the working examples in [`examples/`](examples/) — read them, adapt
them, do not run them blindly against PROD (several have destructive
side effects on KSeF).

## Table of contents

- [Repository structure](#repository-structure)
- [Quick start](#quick-start)
  - [Send an invoice](#send-an-invoice)
  - [Batch invoice upload](#batch-invoice-upload)
  - [Authentication options](#authentication-options)
- [Environment configuration](#environment-configuration)
- [Domain operations](#domain-operations)
- [Examples](#examples)
- [How this compares to the official SDK](#how-this-compares-to-the-official-sdk)
- [Sample app (`ksef-demo`)](#sample-app-ksef-demo)
- [Known KSeF gotchas](#known-ksef-gotchas)
- [Logging](#logging)
- [Architecture](#architecture)
- [License](#license)
- [Status](#status)

## Repository structure

This repo is a Maven multi-module reactor. Only one module ships:

| Module | Role | Published? |
|---|---|---|
| `ksef-client` | The SDK library — the only module consumers depend on | ✅ Maven Central |
| `ksef-demo` | Live-validation demo runner against KSeF DEMO/TEST environments | ❌ dev-only |
| `ksef-examples` | Compile-gate for the JBang-style scripts in `examples/`; ensures example code tracks the public API | ❌ dev-only |
| `ksef-jpms-consumer` | Compile-gate proving a JPMS named-module consumer can use the SDK without seeing `internal/*` packages (per ADR-028) | ❌ dev-only |

Consumers depend on `ksef-client` only; the other three modules exist to keep the SDK honest during development and never ship to Maven Central.

## Quick start

### Maven

```xml
<dependency>
    <groupId>io.github.mgrtomaszzurawski</groupId>
    <artifactId>ksef-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Send an invoice

```java
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;

byte[] invoiceXml = ...; // your FA(3) invoice

try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.TEST)
        .credentials(new KsefTokenCredentials("your-ksef-token", "1234567890"))
        .build()) {

    client.authenticate();

    try (KsefSession session = client.openSession(FormCode.FA3)) {
        session.send(invoiceXml);
    }
}
```

> **Form-code per environment:** DEMO and PROD accept `FA(3)`, `FA_PEF(3)`,
> `FA_KOR_PEF(3)` only. TEST additionally accepts `FA(2)` for backward
> compatibility (`FormCode.FA2`). See `ksef-docs/srodowiska.md`.

### Batch invoice upload

```java
List<byte[]> invoiceXmls = List.of(invoice1Xml, invoice2Xml, invoice3Xml);
try (KsefBatchSession batch = client.openBatchSession(
        FormCode.FA3, invoiceXmls, BatchSessionOptions.online())) {
    batch.uploadParts();
    // close() returns when the server reaches a terminal state (UPO ready,
    // schema rejection, etc.); throws KsefSessionTerminalFailureException on
    // non-200 terminal states or KsefSessionPollingTimeoutException if the
    // session never reaches a terminal state within the polling budget.
}
```

### Authentication options

The SDK supports three credential types:

```java
// 1. KSeF token (simplest — pre-issued token, encrypted in transit)
new KsefTokenCredentials(tokenString, nip);

// 2. PKCS#12 keystore (XAdES-BASELINE-B signing)
new KsefPkcs12Credentials(Path.of("cert.p12"), "passphrase".toCharArray(), nip);

// 3. Raw certificate + private key
new KsefCertificateCredentials(x509Cert, privateKey, nip);
```

`client.authenticate()` blocks while the SDK runs the challenge-response
flow. It can throw `KsefAuthException` (bad token / signature),
`KsefServerException` (5xx during challenge or redeem), or
`KsefNetworkException` (TLS / IO). Wrap calls accordingly when you have
a clear recovery path; otherwise let the exception propagate so the
caller (CLI, request handler, scheduled job) sees a typed failure.

### EU-entity (VAT-UE) authentication

EU entities authenticate with a self-signed X.509 certificate whose
subject CN matches the desired `NipVatUe` identifier. Use
`KsefCertificateCredentials` with a `NipVatUe`-context
`KsefIdentifier`; the SDK's XAdES auth flow handles the rest. The
EU-entity permission grant builders
(`EuEntityPermissionGrantBuilder`, `EuEntityAdminPermissionGrantBuilder`)
require a NipVatUe-context credentials chain and refuse to operate
under a NIP-context token.

## Environment configuration

| Environment | URL |
|-------------|-----|
| `KsefEnvironment.TEST` | `https://api-test.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.DEMO` | `https://api-demo.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.PROD` | `https://api.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.custom(url)` | Self-hosted / staging |

### Builder contract

| Setter | Required? | Default if optional |
|---|---|---|
| `.environment(KsefEnvironment)` | **required** | – |
| `.credentials(KsefCredentials)` | **required** | – |
| `.connectTimeout(Duration)` | optional | 10 s |
| `.readTimeout(Duration)` | optional | 30 s |
| `.retryPolicy(RetryPolicy)` | optional | `enabled=true`, `maxAttempts=3`, `EXPONENTIAL` backoff, `retryOn5xx=true`, `retryOn429=true`, `retryPost=false`, `maxRetryAfterSeconds=60` |
| `.features(FeaturePolicy)` | optional | `UpoVersion.DEFAULT`, `problemDetails=true`, `xadesStrict=false` |

`build()` throws `NullPointerException` if `environment` or
`credentials` is missing.

```java
KsefClient.builder()
    .environment(KsefEnvironment.TEST)
    .credentials(new KsefTokenCredentials(token, nip))
    .connectTimeout(Duration.ofSeconds(10))   // TCP connect
    .readTimeout(Duration.ofSeconds(30))      // per-request response wait
    .retryPolicy(RetryPolicy.builder()
            .maxAttempts(3)
            .backoffStrategy(RetryPolicy.BackoffStrategy.EXPONENTIAL)
            .retryOn5xx(true)
            .retryOn429(true)
            .build())
    .build();
```

`RetryPolicy` defaults to `retryPost=false` — POST requests are NOT
retried automatically, since most KSeF write endpoints are not
idempotent. Override with `.retryPost(true)` only for endpoints you
know to be idempotent.

`FeaturePolicy` defaults to `xadesStrict=false` — KSeF accepts a
broader range of XAdES profiles than `BASELINE-B` strict. Switch to
`enforceXadesCompliance(true)` if your cert chain has been validated
against the strict profile and you want server-side enforcement.

## Domain operations

`KsefClient` is the single entry point. Domain operations are reached via accessors:

- `client.invoices()` — query metadata, export packages, retrieve by KSeF number
- `client.permissions()` — grant/revoke/query/stream for persons, entities, EU entities, subunits, proxies, plus `*AndAwait` helpers
- `client.tokens()` — KSeF API token lifecycle
- `client.certificates()` — enrollment, revocation, query
- `client.peppol()` — Peppol provider directory query
- `client.limits()` — context + subject limits
- `client.rateLimits()` — current rate limits
- `client.testData()` — test-environment helpers (subjects, persons, permissions)

Plus session-level helpers on `KsefClient` itself:

- `client.authenticate()` / `client.reauthenticate()` / `client.terminateAuth()`
- `client.openSession(FormCode)` / `client.openBatchSession(...)`
- `client.invoiceSync(IncrementalSyncPlan, CheckpointStore, InvoiceSink)` —
  HWM-based incremental sync with content download and per-invoice sink
- `client.streamSessions(filter)` — paginate online + batch sessions
- `client.listAuthSessions()` / `client.terminateAuthSession(ref)` /
  `client.refreshAuthToken()`
- `client.publicKeyCertificates()` — fetch KSeF symmetric-key + token
  encryption public keys
- `client.lastChallengeClientIp()` — IP returned by `/auth/challenge`,
  input to `AuthorizationPolicy` IP-pinning

## Examples

The [`examples/`](examples/) directory has eight standalone
`.java` files showing the most common KSeF SDK use cases. They are
**reference code, not runnable scripts** — read them, adapt them. Each
file's header docstring states *what it shows*, the *side effects on
KSeF*, and the *inputs the snippet expects*.

| File | Shows |
|---|---|
| [`SendOnlineInvoice.java`](examples/SendOnlineInvoice.java) | Online session, send one invoice, retrieve UPO |
| [`BatchInvoiceUpload.java`](examples/BatchInvoiceUpload.java) | Batch session, upload pre-built parts, poll until terminal |
| [`QueryInvoiceMetadata.java`](examples/QueryInvoiceMetadata.java) | Lazy paginator across a date range using `streamInvoicesByMetadata` |
| [`IncrementalSync.java`](examples/IncrementalSync.java) | HWM-based incremental sync with checkpoint persistence |
| [`GrantAndRevokePermission.java`](examples/GrantAndRevokePermission.java) | Grant / query / revoke a person permission |
| [`EnrollAndRevokeCertificate.java`](examples/EnrollAndRevokeCertificate.java) | Enrol from CSR, poll for serial, revoke |
| [`Handle401Refresh.java`](examples/Handle401Refresh.java) | Auto re-auth on token expiry |
| [`QrCodeGeneration.java`](examples/QrCodeGeneration.java) | KOD I verification QR code (no API call) |
| [`QrCertificateGeneration.java`](examples/QrCertificateGeneration.java) | KOD II offline-certificate verification QR with PKCS#12 signing (per ADR-019) |

See [`examples/README.md`](examples/README.md) for the full list and notes.

## How this compares to the official SDK

| Aspect | Official `CIRFMF/ksef-client-java` | This project |
|--------|------------------------------------|-------------|
| REST models | 276 hand-written POJOs | Generated from OpenAPI spec (`*Raw` types kept internal) |
| XML invoice models | JAXB from XSD | JAXB from XSD (same approach) |
| HTTP client | Single god-class `DefaultKsefClient` | Per-domain clients reached via `KsefClient.<feature>()` |
| Retry | None | Configurable `RetryPolicy` with backoff + jitter |
| Pagination | None | Lazy `Stream<T>` paginators (`streamInvoicesByMetadata`, `streamPersons`, ...) — AWS-SDK-style |
| Exceptions | Basic `ApiException` | Typed hierarchy (`KsefAuthException`, `KsefServerException`, `KsefRateLimitException`, …) |
| Build tool | Gradle | Maven |
| Java version | 11 source, 21 toolchain | 17+ |
| Distribution | GitHub Packages only | Maven Central (release-profile staging dry-run pending) |
| JPMS | None | Named module with strict export boundaries |

## Sample app (`ksef-demo`)

The SDK ships with a live-validation runner under
[`ksef-demo/`](ksef-demo/) that exercises every domain against the KSeF
demo and test environments. Modes, credentials properties, certificate
quota gating, and troubleshooting are documented in
[`ksef-demo/README.md`](ksef-demo/README.md).

## Known KSeF gotchas

KSeF has a few server behaviours that diverge from spec or are
worth knowing when integrating: session cooldown after termination,
asynchronous authentication, certificate quota, retention expiry as
HTTP 410, etc. The SDK either handles them transparently or surfaces
them as typed exceptions — the curated list lives in
[`KNOWN-SERVER-BEHAVIORS.md`](KNOWN-SERVER-BEHAVIORS.md).

## Logging

The SDK uses SLF4J. No logging backend is bundled — your application picks the implementation (Logback, Log4j2, `slf4j-simple`, etc.).

**Default level should be `WARN`** for the SDK logger. All SDK loggers live under `io.github.mgrtomaszzurawski.ksef.sdk.*`. Per AWS/Azure SDK guidelines, leaving the SDK at `WARN` keeps production logs quiet; consumers turn on `DEBUG` only when diagnosing a specific problem.

```xml
<!-- logback.xml — show only terminal failures by default -->
<logger name="io.github.mgrtomaszzurawski.ksef.sdk" level="WARN"/>

<!-- Diagnostic mode: log every HTTP request/response + per-call entry -->
<logger name="io.github.mgrtomaszzurawski.ksef.sdk" level="DEBUG"/>
```

What you'll see at `DEBUG`: HTTP method + URI, response status + elapsed ms, per-domain operation entry. Bodies are never logged (they carry NIPs, PESELs, JWT tokens, AES keys, full invoice XML — `RODO` violation if leaked).

## Concurrency and rate limits

`KsefClient` is thread-safe — one instance can serve N concurrent threads. The underlying JDK `HttpClient` pools connections; `SessionContext` uses `volatile` + `synchronized` on mutations.

KSeF rate limits are **per (context + IP)** — multiple JVMs / pods on different IPs are billed independently. Current per-operation limits (e.g. 8 req/s for `/invoices/exports`, 16 req/s for `/invoices/query/metadata`) are returned by:

```java
ApiRateLimits limits = client.rateLimits().getRateLimits();
// limits.invoiceMetadata().perSecond() etc.
```

The SDK reacts to HTTP 429 + `Retry-After` automatically (`RetryPolicy.retryOn429=true` default, exponential backoff with jitter). It does **not** proactively throttle outbound requests — a producer firing thousands of parallel calls (e.g. `parallelStream()` over a large filter) will hit 429s and retry-storm before reaching the per-second limit.

For high-concurrency producers, throttle at the call site:

```java
Semaphore slot = new Semaphore(8);  // ~match invoiceMetadata.perSecond
client.invoices().streamInvoicesByMetadata(filter)
    .parallel()
    .forEach(invoice -> {
        slot.acquireUninterruptibly();
        try { processInvoice(invoice); }
        finally { slot.release(); }
    });
```

For `InvoiceSyncClient` incremental sync: spec recommends a **15 minute interval** between runs (`przyrostowe-pobieranie-faktur.md`) — schedule via cron / queue worker, not a tight loop.

## Architecture

See [`ADR/`](ADR/) — architectural decision records covering generation strategy, package structure, single-entry-point design, encryption semantics, batch assembly modes, transport-level URI redaction, JPMS public-API defense, XXE hardening, etc. Layered package layout, encryption flow, and JPMS boundaries are documented in the ADR set; key entry points are ADR-005, ADR-008, ADR-011, ADR-012, ADR-016.

## License

Project source code is [AGPL-3.0-only](LICENSE.txt) — strong-copyleft license. Suitable for SaaS / internal-tool use; commercial closed-source integration requires a separate agreement. Solo-maintained SDK, tested against the KSeF demo environment; provided without warranty (see LICENSE).

Bundled official KSeF OpenAPI/XSD files (`ksef-client/openapi/open-api.json`, `ksef-client/xsd/**`) remain under their original MIT license — see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Status

- ✅ All 11 KSeF API domains covered (101 OK / 0 FAIL / 15 SKIP across DEMO + TEST live demo runs; SKIPs are documented per-runner and reflect domain-content limitations such as the PEPPOL accepted-UBL fixture)
- ✅ 660+ unit + integration tests across 71 test classes (WireMock-mocked HTTP, full transport coverage)
- ✅ JaCoCo coverage gate green at `INSTRUCTION ≥ 0.75`, `METHOD ≥ 0.80` (`mvn verify` → `target/site/jacoco/`); per-builder 100% method gate is tracked separately
- 🚧 JSpecify null-safety annotations (ADR-017)
- 🚧 Maven Central first publish (release-profile dry run pending)

- Release history: [`CHANGELOG.md`](CHANGELOG.md)
- Architectural decisions: [`ADR/`](ADR/)
