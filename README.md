# KSeF Java SDK

> **UNOFFICIAL PREVIEW (0.1.0-preview)**
>
> This is a **solo-developed, unofficial** Java SDK for the Polish KSeF
> e-invoicing system. **Not affiliated** with Ministerstwo Finansów,
> CIRFMF, or any institution operating KSeF.
>
> - Preview status — API may break between 0.x releases without notice
> - No commercial support, no SLA, no liability (AGPL-3.0 §15–16)
> - One-person project — issue response is best-effort
> - **For production invoice flows, prefer the official SDK at
>   [CIRFMF/ksef-client-java](https://github.com/CIRFMF/ksef-client-java).**

---

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)]()

OpenAPI-first Java SDK for the Polish National e-Invoicing System ([KSeF](https://www.podatki.gov.pl/ksef/)) REST API v2.

REST models are generated from the official [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs) OpenAPI specification; invoice XML models are generated from the published XSD schemas. A hand-written SDK overlay hides the protocol details (challenge-response auth, AES-256-CBC session encryption, XAdES-BASELINE-B signing, retry-with-backoff, batch upload + polling) behind a single facade.

> **Status:** `0.1.0-preview` published to Maven Central. API surface is
> EXPERIMENTAL per the `@API` annotation on `KsefClient`; `0.x` releases
> may break it. The eventual 1.0.0 cut will drop the marker and lock the
> contract. See [`CHANGELOG.md`](CHANGELOG.md) for the full release
> notes and the Known limitations list.

## What you can do

- Send invoices online, one at a time (`OnlineSession.sendInvoice`)
- Submit invoices in batch via the synchronous batch facade (encrypted ZIP, parallel upload, automatic UPO fetch)
- Query invoice metadata with filters, lazily paginated (`InvoiceArchive.streamByMetadata`)
- Retrieve a single invoice + UPO by KSeF number (`InvoiceArchive`)
- Incremental sync of newly-permanently-stored invoices via a HWM cursor (`InvoiceSync.asStream`)
- Manage permissions, KSeF tokens, certificates (enrol / revoke / query)
- Generate KOD I + KOD II invoice-verification QR codes
- Authenticate as NIP / EU-entity (VAT-UE) / Peppol provider / sub-unit
  via KSeF tokens, raw certificates, or PKCS#12 keystores (XAdES-BASELINE-B)

See the working examples in [`examples/`](examples/) — read them, adapt them, do not run them blindly against PROD (several have destructive side effects on KSeF).

## Table of contents

- [Repository structure](#repository-structure)
- [Importing in IntelliJ IDEA](#importing-in-intellij-idea)
- [Quick start](#quick-start)
  - [Coordinates](#coordinates)
  - [Send an invoice](#send-an-invoice)
  - [Batch invoice upload](#batch-invoice-upload)
  - [Authentication options](#authentication-options)
  - [EU-entity (VAT-UE) authentication](#eu-entity-vat-ue-authentication)
- [Environment configuration](#environment-configuration)
- [Builder contract](#builder-contract)
- [Domain operations](#domain-operations)
- [Examples](#examples)
- [How this compares to the official SDK](#how-this-compares-to-the-official-sdk)
- [Sample app (`ksef-demo`)](#sample-app-ksef-demo)
- [Known KSeF gotchas](#known-ksef-gotchas)
- [Logging](#logging)
- [Concurrency and rate limits](#concurrency-and-rate-limits)
- [Architecture](#architecture)
- [License](#license)
- [Status](#status)

## Repository structure

This repo is a **Gradle** multi-module reactor (Maven was removed per ADR-031 on 2026-05-10). Only one module ships to consumers:

| Module | Role | Published? |
|---|---|---|
| `ksef-client` | The SDK library — the only module consumers depend on | ✅ Maven Central |
| `ksef-xml-models` | JAXB-generated XML models (FA(2)/FA(3)/PEF/PEF_KOR/UPO/AUTH) — transitive dependency of `ksef-client` | ✅ Maven Central (transitive) |
| `ksef-rest-models` | OpenAPI-generated REST models (`*Raw` types) — transitive dependency of `ksef-client` | ✅ Maven Central (transitive) |
| `ksef-demo` | Live-validation demo runner against KSeF DEMO/TEST environments | ❌ dev-only |
| `ksef-examples` | Compile-gate for the JBang-style scripts in `examples/`; ensures example code tracks the public API | ❌ dev-only |
| `ksef-jpms-consumer` | Compile-gate proving a JPMS named-module consumer can use the SDK without seeing `internal/*` packages (per ADR-028) | ❌ dev-only |

Consumers depend on `ksef-client` only; `ksef-xml-models` and `ksef-rest-models` are pulled in automatically as transitive dependencies. The three dev-only modules exist to keep the SDK honest during development.

## Importing in IntelliJ IDEA

This project uses Gradle, not Maven. To open it in IntelliJ:

1. **File → Open → select `build.gradle.kts`** (the file at the repo root, not the folder).
2. Pick **"Open as Project"** at the prompt; trust the project.
3. Wait 3–5 minutes for the initial Gradle sync (it pulls plugins, generates JAXB + OpenAPI sources, and wires up the multi-module classpath).

Opening the folder via `File → Open Project` skips the Gradle import — sub-modules and generated sources stay unresolved. After a correct import you should see a **Gradle tool window** in the right sidebar; if it's missing you opened it as a plain folder.

## Quick start

### Coordinates

**Maven** (`pom.xml`):

```xml
<dependency>
    <groupId>io.github.mgrtomaszzurawski</groupId>
    <artifactId>ksef-client</artifactId>
    <version>0.1.0-preview</version>
</dependency>
```

**Gradle** (`build.gradle.kts`):

```kotlin
implementation("io.github.mgrtomaszzurawski:ksef-client:0.1.0-preview")
```

### Send an invoice

```java
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.OnlineSession;

byte[] invoiceXml = ...; // your FA(3) invoice XML bytes

try (KsefClient client = KsefClient.builder()
        .environment(KsefEnvironment.TEST)
        .credentials(new KsefTokenCredentials("your-ksef-token", "1234567890"))
        .build()) {

    // Authentication is lazy — opening a session triggers the
    // challenge-response flow on first call.
    OnlineSession session = client.invoices().sessions().online(FormCode.FA3);
    var submitted = session.sendInvoice(Invoice.fromXml(FormCode.FA3, invoiceXml));

    // Transition the session to terminal state and retrieve the UPO
    // (Urzędowe Poświadczenie Odbioru — KSeF acceptance receipt) for
    // the invoice we just submitted.
    ClosedSession closed = session.complete();
    byte[] upoXml = closed.cleared(submitted).upo().xmlBytes();
}
```

> **Form-code per environment:** DEMO and PROD accept `FA(3)`, `PEF(3)`,
> `PEF_KOR(3)`. TEST additionally accepts `FA(2)` for backward
> compatibility. Constants: `FormCode.FA2`, `FormCode.FA3`,
> `FormCode.PEF3`, `FormCode.PEF_KOR3`. See `ksef-docs/srodowiska.md`.

### Batch invoice upload

> **Threading warning:** This call blocks the calling thread for minutes to
> hours, depending on batch size and upload bandwidth. KSeF accepts batches
> up to 5 GB. Do not call from UI threads, HTTP request handlers, or
> reactive framework dispatch threads. Wrap with a dedicated executor.

```java
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import java.util.List;

List<Invoice> invoices = List.of(
        Invoice.fromXml(FormCode.FA3, invoice1Xml),
        Invoice.fromXml(FormCode.FA3, invoice2Xml),
        Invoice.fromXml(FormCode.FA3, invoice3Xml));

BatchResult<Invoice> result = client.invoices().sessions().batch()
        .submit(invoices, BatchOptions.defaults());

// By the time submit returns, every accepted invoice has its UPO downloaded.
System.out.println("Cleared: " + result.successfulCount()
        + " / Failed: " + result.failedCount());
```

For async use, wrap in a `CompletableFuture` against a dedicated executor:

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
CompletableFuture<BatchResult<Invoice>> future = CompletableFuture.supplyAsync(
        () -> client.invoices().sessions().batch()
                .submit(invoices, BatchOptions.defaults()),
        executor);
```

For file-only inputs (no pre-built `Invoice` objects):

```java
import java.nio.file.Path;

BatchResult<Invoice> result = client.invoices().sessions().batch()
        .submitFromFiles(FormCode.FA3, List.of(
                Path.of("inv-001.xml"),
                Path.of("inv-002.xml")), BatchOptions.defaults());
```

### Authentication options

The SDK supports three credential types, all under `sdk.config.credentials`:

```java
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefCertificateCredentials;

// 1. KSeF token (simplest — pre-issued token, encrypted in transit)
new KsefTokenCredentials("your-token", "1234567890");

// 2. PKCS#12 keystore (XAdES-BASELINE-B signing)
new KsefPkcs12Credentials(Path.of("cert.p12"), "passphrase".toCharArray(), "1234567890");

// 3. Raw X.509 certificate + private key
new KsefCertificateCredentials(x509Cert, privateKey, "1234567890");
```

The SDK authenticates lazily — the first endpoint call triggers the challenge-response flow. Exceptions thrown by that flow are typed: `KsefAuthException` (bad token / signature), `KsefServerException` (5xx during challenge or redeem), `KsefNetworkException` (TLS / IO). Wrap calls accordingly when you have a clear recovery path; otherwise let the exception propagate so the caller (CLI, request handler, scheduled job) sees a typed failure.

### EU-entity (VAT-UE) authentication

EU entities authenticate with a self-signed X.509 certificate whose subject CN matches the desired `NipVatUe` identifier. Use `KsefCertificateCredentials` with a `NipVatUe`-context `KsefIdentifier`; the SDK's XAdES auth flow handles the rest. The EU-entity permission grant builders (`EuEntityPermissionGrantBuilder`, `EuEntityAdminPermissionGrantBuilder`) require a NipVatUe-context credentials chain and refuse to operate under a NIP-context token.

`KsefIdentifier` factories:

```java
KsefIdentifier.nip("1234567890");                  // 10-digit Polish tax ID
KsefIdentifier.internalId("1234567890-12345");     // NIP-suffixed internal id
KsefIdentifier.nipVatUe("PL1234567890");           // EU VAT-UE format
KsefIdentifier.peppolId("0088:1234567890");        // Peppol participant id
```

## Environment configuration

| Environment | URL |
|-------------|-----|
| `KsefEnvironment.TEST` | `https://api-test.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.DEMO` | `https://api-demo.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.PROD` | `https://api.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.custom(url)` | Self-hosted / staging |

## Builder contract

| Setter | Required? | Default if optional |
|---|---|---|
| `.environment(KsefEnvironment)` | **required** | – |
| `.credentials(KsefCredentials)` | **required** | – |
| `.connectTimeout(Duration)` | optional | 10 s |
| `.readTimeout(Duration)` | optional | 30 s |
| `.retryPolicy(RetryPolicy)` | optional | `enabled=true`, `maxAttempts=3`, exponential backoff, `retryOn5xx=true`, `retryOn429=true`, `retryPost=false`, `maxRetryAfterSeconds=60` |
| `.features(FeaturePolicy)` | optional | `UpoVersion.DEFAULT`, `problemDetails=true`, `xadesStrict=false` |
| `.invoiceVerificationTimeout(Duration)` | optional | session-level default |
| `.offlineSigning(OfflineSigningProvider)` | optional | required only for KOD II / offline-cert flows |
| `.invoiceTypes(KsefInvoiceTypes)` | optional | built-in registry (FA(2)/FA(3)/PEF(3)/PEF_KOR(3)) |

`build()` throws `NullPointerException` if `environment` or `credentials` is missing.

```java
KsefClient.builder()
    .environment(KsefEnvironment.TEST)
    .credentials(new KsefTokenCredentials(token, nip))
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .retryPolicy(RetryPolicy.builder()
            .maxAttempts(3)
            .backoffStrategy(RetryPolicy.BackoffStrategy.EXPONENTIAL)
            .retryOn5xx(true)
            .retryOn429(true)
            .build())
    .build();
```

`RetryPolicy` defaults to `retryPost=false` — POST requests are NOT retried automatically, since most KSeF write endpoints are not idempotent. Override with `.retryPost(true)` only for endpoints you know to be idempotent.

`FeaturePolicy` defaults to `xadesStrict=false` — KSeF accepts a broader range of XAdES profiles than `BASELINE-B` strict. Switch to `enforceXadesCompliance(true)` if your cert chain has been validated against the strict profile and you want server-side enforcement.

## Domain operations

`KsefClient` is the single entry point. Domain operations reached via accessors:

- `client.invoices()` — the invoicing facade; see sub-accessors below
- `client.authSessions()` — explicit login (`ensureLoggedIn()`), list / terminate active auth sessions, and the diagnostic `lastChallengeClientIp()` hook for `AuthorizationPolicy` IP-pinning
- `client.tokens()` — KSeF API token lifecycle (generate, query, revoke)
- `client.permissions()` — grant / revoke / query / stream for persons, entities, EU entities, subunits, authorizations, plus `*AndAwait` async helpers
- `client.certificates()` — enrollment, revocation, query; certificate quota via `getLimits()` (12 enrolments / 6 active per taxpayer)
- `client.limits()` — `getContextLimits()` (session/batch caps), `getSubjectLimits()` (per-subject quotas), `getRateLimits()` (per-endpoint req/s)
- `client.peppol()` — Peppol provider directory query and stream
- `client.testData()` — TEST/DEMO-only test-data administration (subjects, persons, permissions, session and rate-limit overrides); every method throws `KsefUnsupportedEnvironmentException` when wired to `KsefEnvironment.PROD`
- `client.qrCode()` — KOD I + KOD II verification QR rendering
- `client.config()` — diagnostic snapshot of the active configuration; useful for multi-tenant orchestration

Invoice-flow entry points under `client.invoices()`:

- `client.invoices().sessions().online(FormCode)` — open an online session; per-invoice `sendInvoice` + UPO retrieval via the `complete()` → `ClosedSession` transition
- `client.invoices().sessions().batch().submit(List<Invoice>, BatchOptions)` — synchronous batch flow, returns `BatchResult` with cleared + failed breakdown
- `client.invoices().sessions().batch().submitFromFiles(FormCode, List<Path>, BatchOptions)` — file-streaming batch variant for inputs that do not fit in memory
- `client.invoices().archive().queryByMetadata(InvoiceQueryRequest)` — single-page metadata snapshot
- `client.invoices().archive().streamByMetadata(InvoiceQueryRequest)` — lazy paginator across permanent storage
- `client.invoices().archive().getByKsefNumber(KsefNumber)` — typed `InvoiceDocument` retrieval
- `client.invoices().sync().asStream(IncrementalSyncPlan, CheckpointStore)` — HWM-based incremental sync producing `Stream<DecryptedInvoice>`
- `client.invoices().offline()` — offline-mode invoice authoring (`OFFLINE_24`, `KSEF_UNAVAILABILITY`, `KSEF_EMERGENCY`)
- `client.invoices().export(...)` — long-running export jobs (kick off, poll, download)

## Examples

The [`examples/`](examples/) directory has **22 standalone `.java` files** showing the most common KSeF SDK use cases. They are **reference code, not runnable scripts** — read them, adapt them. Each file's header docstring states *what it shows*, the *side effects on KSeF*, and the *inputs the snippet expects*.

| File | Shows |
|---|---|
| [`SendOnlineInvoice.java`](examples/SendOnlineInvoice.java) | Online session, send one invoice, retrieve UPO via `ClosedSession.cleared(...)` |
| [`BatchInvoiceUpload.java`](examples/BatchInvoiceUpload.java) | Batch session via `sessions().batch().submit(...)`, async wrapper |
| [`IssueOfflineInvoice.java`](examples/IssueOfflineInvoice.java) | Offline-mode invoice authoring with KOD I + KOD II QR codes |
| [`Handle401Refresh.java`](examples/Handle401Refresh.java) | Automatic re-authentication on access-token expiry |
| [`QueryInvoiceMetadata.java`](examples/QueryInvoiceMetadata.java) | Lazy paginator across a date range with `streamByMetadata` |
| [`QueryInvoicesByMetadata.java`](examples/QueryInvoicesByMetadata.java) | Single-page metadata query with explicit pagination |
| [`GetInvoiceFromArchive.java`](examples/GetInvoiceFromArchive.java) | Retrieve a single invoice from the archive by KSeF number |
| [`IncrementalSync.java`](examples/IncrementalSync.java) | HWM-based incremental sync with `CheckpointStore` |
| [`ExportInvoiceArchive.java`](examples/ExportInvoiceArchive.java) | Asynchronous bulk archive export — submit, poll, download |
| [`RegisterCustomInvoiceType.java`](examples/RegisterCustomInvoiceType.java) | Register a custom `InvoiceDocument` type via `KsefInvoiceTypes` |
| [`GrantAndRevokePermission.java`](examples/GrantAndRevokePermission.java) | Grant / query / revoke a person permission via `*AndAwait` |
| [`GenerateAndRevokeToken.java`](examples/GenerateAndRevokeToken.java) | Generate KSeF API token, poll until Active, then revoke |
| [`EnrollAndRevokeCertificate.java`](examples/EnrollAndRevokeCertificate.java) | Enrol from CSR, poll for serial, revoke |
| [`ListAuthSessions.java`](examples/ListAuthSessions.java) | List the consumer's active auth sessions in the current KSeF |
| [`IpAllowlistOnCredentials.java`](examples/IpAllowlistOnCredentials.java) | Attach an IP allow-list `AuthorizationPolicy` to credentials |
| [`MultiTenantOrchestration.java`](examples/MultiTenantOrchestration.java) | Manage multiple `KsefClient` instances against different tenants |
| [`ConfigureFeaturePolicy.java`](examples/ConfigureFeaturePolicy.java) | Opt into RFC 7807 Problem Details and other `FeaturePolicy` knobs |
| [`QueryPeppolProviders.java`](examples/QueryPeppolProviders.java) | Query Peppol service providers registered in KSeF (single + stream) |
| [`QueryRateLimitsAndQuotas.java`](examples/QueryRateLimitsAndQuotas.java) | Query KSeF context/subject limits + per-endpoint rate limits |
| [`GenerateInvoiceQrCode.java`](examples/GenerateInvoiceQrCode.java) | Generate KOD I (online-invoice verification) QR from invoice |
| [`QrCodeGeneration.java`](examples/QrCodeGeneration.java) | KOD I verification QR rendering (no API call) |
| [`QrCertificateGeneration.java`](examples/QrCertificateGeneration.java) | KOD II offline-certificate verification QR with PKCS#12 signing |

See [`examples/README.md`](examples/README.md) for the long-form list with notes on inputs and KSeF side effects.

## How this compares to the official SDK

| Aspect | Official `CIRFMF/ksef-client-java` | This project |
|--------|------------------------------------|-------------|
| REST models | 276 hand-written POJOs | Generated from OpenAPI spec (`*Raw` types kept internal) |
| XML invoice models | JAXB from XSD | JAXB from XSD (same approach) |
| HTTP client | Single god-class `DefaultKsefClient` | Per-domain clients reached via `KsefClient.<feature>()` |
| Retry | None | Configurable `RetryPolicy` with backoff + jitter |
| Pagination | None | Lazy `Stream<T>` paginators (`streamByMetadata`, `streamPersons`, ...) — AWS-SDK-style |
| Exceptions | Basic `ApiException` | Typed hierarchy (`KsefAuthException`, `KsefServerException`, `KsefRateLimitException`, …) |
| Build tool | Gradle | Gradle |
| Java version | 11 source, 21 toolchain | 17+ |
| Distribution | GitHub Packages only | Maven Central |
| JPMS | None | Named module with strict export boundaries |

## Sample app (`ksef-demo`)

The SDK ships with a live-validation runner under [`ksef-demo/`](ksef-demo/) that exercises every domain against the KSeF demo and test environments. Modes, credentials properties, certificate quota gating, and troubleshooting are documented in [`ksef-demo/README.md`](ksef-demo/README.md).

## Known KSeF gotchas

KSeF has a few server behaviours that diverge from spec or are worth knowing when integrating: session cooldown after termination, asynchronous authentication, certificate quota, retention expiry as HTTP 410, etc. The SDK either handles them transparently or surfaces them as typed exceptions — the curated list lives in [`KNOWN-SERVER-BEHAVIORS.md`](KNOWN-SERVER-BEHAVIORS.md).

## Logging

The SDK uses SLF4J. No logging backend is bundled — your application picks the implementation (Logback, Log4j2, `slf4j-simple`, etc.).

**Default level should be `WARN`** for the SDK logger. All SDK loggers live under `io.github.mgrtomaszzurawski.ksef.sdk.*`. Per AWS/Azure SDK guidelines, leaving the SDK at `WARN` keeps production logs quiet; consumers turn on `DEBUG` only when diagnosing a specific problem.

```xml
<!-- logback.xml — show only terminal failures by default -->
<logger name="io.github.mgrtomaszzurawski.ksef.sdk" level="WARN"/>

<!-- Diagnostic mode: log every HTTP request/response + per-call entry -->
<logger name="io.github.mgrtomaszzurawski.ksef.sdk" level="DEBUG"/>
```

What you'll see at `DEBUG`: HTTP method + URI, response status + elapsed ms, per-domain operation entry. Bodies are never logged (they carry NIPs, PESELs, JWT tokens, AES keys, full invoice XML — RODO violation if leaked).

Note: building `KsefClient` emits one `WARN`-level log per construction in `0.1.0-preview` — a deliberate preview-status reminder that goes away with the `@API EXPERIMENTAL` marker at the 1.0.0 cut. Silence it by setting the `io.github.mgrtomaszzurawski.ksef.sdk.KsefClient` logger to `ERROR`.

## Concurrency and rate limits

`KsefClient` is thread-safe — one instance can serve N concurrent threads. The underlying JDK `HttpClient` pools connections; the session context uses `volatile` + `synchronized` on mutations.

KSeF rate limits are **per (context + IP)** — multiple JVMs / pods on different IPs are billed independently. Current per-operation limits are returned by:

```java
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;

ApiRateLimits limits = client.limits().getRateLimits();
int perSecond = limits.invoiceMetadata().perSecond();
```

The SDK reacts to HTTP 429 + `Retry-After` automatically (`RetryPolicy.retryOn429=true` default, exponential backoff with jitter). It does **not** proactively throttle outbound requests — a producer firing thousands of parallel calls (e.g. `parallelStream()` over a large filter) will hit 429s and retry-storm before reaching the per-second limit.

For high-concurrency producers, throttle at the call site:

```java
ApiRateLimits limits = client.limits().getRateLimits();
int permits = limits.invoiceMetadata().perSecond();
Semaphore slot = new Semaphore(permits);
client.invoices().archive().streamByMetadata(query)
    .parallel()
    .forEach(invoice -> {
        slot.acquireUninterruptibly();
        try { processInvoice(invoice); }
        finally { slot.release(); }
    });
```

For incremental sync (`client.invoices().sync().asStream(plan, store)` returning a lazy `Stream<DecryptedInvoice>`), the spec recommends a **15 minute interval** between runs (`przyrostowe-pobieranie-faktur.md`) — schedule via cron / queue worker, not a tight loop.

## Architecture

See [`ADR/`](ADR/) — architectural decision records covering generation strategy, package structure, single-entry-point design, encryption semantics, batch assembly modes, transport-level URI redaction, JPMS public-API defense, XXE hardening, etc. Layered package layout, encryption flow, and JPMS boundaries are documented in the ADR set; key entry points are ADR-005, ADR-008, ADR-011, ADR-012 (superseded by ADR-034), ADR-016, ADR-031.

## License

Project source code is [AGPL-3.0-only](LICENSE.txt) — strong-copyleft license. Suitable for SaaS / internal-tool use; commercial closed-source integration requires a separate agreement. Solo-maintained SDK, tested against the KSeF demo environment; provided without warranty (see LICENSE).

Bundled official KSeF OpenAPI/XSD files (`ksef-client/openapi/open-api.json`, `ksef-client/xsd/**`) remain under their original MIT license — see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Status

- ✅ All 11 KSeF API domains covered (101 OK / 0 FAIL / 15 SKIP across DEMO + TEST live demo runs; SKIPs are documented per-runner and reflect domain-content limitations such as the PEPPOL accepted-UBL fixture)
- ✅ 720+ unit + integration tests across 78 test classes (WireMock-mocked HTTP, full transport coverage)
- ✅ JaCoCo coverage gate green at `INSTRUCTION ≥ 0.70`, `METHOD ≥ 0.75` bundle floor + per-class `METHOD = 1.00` ratchet on every `domain.*.builder.*Builder` and `domain.*.*Client` (`./gradlew check` → `ksef-client/build/reports/jacoco/`)
- ✅ JSpecify null-safety annotations across all 29 exported `package-info` (ADR-017)
- ✅ `0.1.0-preview` published to Maven Central (EXPERIMENTAL — `@API` annotation on `KsefClient`)

- Release history: [`CHANGELOG.md`](CHANGELOG.md)
- Architectural decisions: [`ADR/`](ADR/)
