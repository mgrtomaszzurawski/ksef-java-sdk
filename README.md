# KSeF Java SDK

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)]()

OpenAPI-first Java SDK for the Polish National e-Invoicing System ([KSeF](https://www.podatki.gov.pl/ksef/)) REST API v2.

Generated from the official [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs) OpenAPI specification, with a hand-written ergonomic overlay that hides the protocol details (challenge-response auth, AES-256-CBC session encryption, XAdES-BASELINE-B signing, retry-with-backoff, batch upload + polling).

> **Status:** SDK has reached `1.0.0` source-tree readiness. Maven Central
> publication is gated on a clean release-profile dry run; until the artifact
> is staged on Central, install locally with `mvn install` to use it.

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

byte[] invoiceXml = ...; // your FA(2) or FA(3) invoice

try (KsefClient client = KsefClient.builder(KsefEnvironment.TEST)
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

## Environment configuration

| Environment | URL |
|-------------|-----|
| `KsefEnvironment.TEST` | `https://api-test.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.DEMO` | `https://api-demo.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.PREPROD` | `https://api-preprod.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.PROD` | `https://api.ksef.mf.gov.pl/v2` |
| `KsefEnvironment.custom(url)` | Self-hosted / staging |

Builder options:

```java
KsefClient.builder(KsefEnvironment.TEST)
    .credentials(...)
    .connectTimeout(Duration.ofSeconds(10))   // TCP connect
    .readTimeout(Duration.ofSeconds(30))       // per-request response wait
    .retryPolicy(RetryPolicy.builder()
            .maxAttempts(3)
            .baseDelay(Duration.ofSeconds(1))
            .retryOn5xx(true)
            .retryOn429(true)
            .build())
    .build();
```

## Domain operations

`KsefClient` is the single entry point. Domain operations are reached via accessors:

- `client.invoices()` — query metadata, export packages, retrieve by KSeF number
- `client.permissions()` — grant/revoke/query for persons, entities, EU entities, subunits, proxies (19 ops)
- `client.tokens()` — KSeF API token lifecycle
- `client.certificates()` — enrollment, revocation, query
- `client.peppol()` — Peppol provider directory query
- `client.limits()` — context + subject limits
- `client.rateLimits()` — current rate limits
- `client.testData()` — test-environment helpers (subjects, persons, permissions)

Plus session-level helpers on `KsefClient` itself:

- `client.authenticate()` / `client.reauthenticate()` / `client.terminateAuth()`
- `client.openSession(FormCode)` / `client.openBatchSession(...)`

## How this compares to the official SDK

| Aspect | Official `CIRFMF/ksef-client-java` | This project |
|--------|------------------------------------|-------------|
| REST models | 276 hand-written POJOs | Generated from OpenAPI spec (`*Raw` types kept internal) |
| XML invoice models | JAXB from XSD | JAXB from XSD (same approach) |
| HTTP client | Single god-class `DefaultKsefClient` | Per-domain clients reached via `KsefClient.<feature>()` |
| Retry | None | Configurable `RetryPolicy` with backoff + jitter |
| Pagination | None | Lazy `Stream<T>` paginators (`streamMetadata`, `streamPersons`, ...) — AWS-SDK-style |
| Exceptions | Basic `ApiException` | Typed hierarchy (`KsefAuthException`, `KsefServerException`, `KsefRateLimitException`, …) |
| Build tool | Gradle | Maven |
| Java version | 11 source, 21 toolchain | 17+ |
| Distribution | GitHub Packages only | Maven Central (release-profile staging dry-run pending) |
| JPMS | None | Named module with strict export boundaries |

## Sample app (`ksef-demo`)

Live-validation harness that exercises the SDK against the KSeF demo
environment. Each runner reports per-operation results.

```bash
# Configure ksef-credentials.properties at repo root:
#   ksef.environment=https://api-test.ksef.mf.gov.pl
#   ksef.token=YOUR_TOKEN
#   ksef.nip=1234567890

# Build the SDK locally first:
mvn install -pl ksef-client -DskipTests

# Run a demo mode:
mvn exec:java -pl ksef-demo -Ddemo.mode=AUTH_SAFE
```

Available modes (defined in `DemoMode`):
- `READ_ONLY` — auth + listing operations (no writes)
- `AUTH_SAFE` — auth + safe-write operations (no permission changes)
- `FULL` — full lifecycle including session open/send/close
- `CLEANUP` — terminate any leftover sessions/permissions from prior runs

Certificate enrollment runs are gated separately via `CertificateRunner` to
avoid burning the monthly KSeF certificate quota on every demo execution.

## Build and test

```bash
# Build SDK + run all tests (no live KSeF access needed)
mvn clean verify -Dmaven.javadoc.skip=true

# Static analysis (must pass with 0 violations)
mvn spotbugs:check pmd:check checkstyle:check -pl ksef-client

# Coverage report
mvn verify -pl ksef-client  # produces target/site/jacoco/index.html
```

## Logging

The SDK uses SLF4J. No logging backend is bundled — your application picks the implementation (Logback, Log4j2, `slf4j-simple`, etc.).

**Default level should be `WARN`** for the SDK logger (`io.github.mgrtomaszzurawski.ksef`). Per AWS/Azure SDK guidelines, leaving the SDK at `WARN` keeps production logs quiet; consumers turn on `DEBUG` only when diagnosing a specific problem.

```xml
<!-- logback.xml — show only terminal failures by default -->
<logger name="io.github.mgrtomaszzurawski.ksef" level="WARN"/>

<!-- Diagnostic mode: log every HTTP request/response + per-call entry -->
<logger name="io.github.mgrtomaszzurawski.ksef" level="DEBUG"/>
```

What you'll see at `DEBUG`: HTTP method + URI, response status + elapsed ms, per-domain operation entry. Bodies are never logged (they carry NIPs, PESELs, JWT tokens, AES keys, full invoice XML — `RODO` violation if leaked).

## Architecture

See:
- [`context/ARCHITECTURE.md`](context/ARCHITECTURE.md) — current package layout, encryption flow, JPMS boundaries
- [`ADR/`](ADR/) — 16 architectural decision records covering generation strategy, package structure, single-entry-point design, encryption semantics, etc.

## License

Project source code is [AGPL-3.0-only](LICENSE.txt) — strong-copyleft license. Suitable for SaaS / internal-tool use; commercial closed-source integration requires a separate agreement. Solo-maintained SDK, tested against the KSeF demo environment; provided without warranty (see LICENSE).

Bundled official KSeF OpenAPI/XSD files (`ksef-client/openapi/open-api.json`, `ksef-client/xsd/**`) remain under their original MIT license — see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Status

- ✅ All 11 KSeF API domains covered (62+ live ops verified against demo env)
- ✅ 500+ unit + integration tests (WireMock-mocked HTTP, full transport coverage)
- ✅ JaCoCo coverage report generated (`mvn verify` → `target/site/jacoco/`)
- ✅ Per-builder method coverage gate green
- 🚧 JSpecify null-safety annotations (ADR-017)
- 🚧 Maven Central first publish (release-profile dry run pending)

Project state and roadmap: [`context/PLAN-2026-04-03-2045-implementation-plan.md`](context/PLAN-2026-04-03-2045-implementation-plan.md).
