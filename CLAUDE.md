# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## What this is

OpenAPI-first Java SDK for the Polish National e-Invoicing System (KSeF) REST API v2. Multi-module Maven project:
- `ksef-client` — the SDK library (published to Maven Central)
- `ksef-demo` — integration examples and smoke tests (not published)

KSeF (Krajowy System e-Faktur) is a mandatory e-invoicing platform operated by the Polish Ministry of Finance. The API handles invoice submission, retrieval, session management, authentication via qualified signatures, and permissions management.

## Rules

**All documentation, ADRs, code comments, and commit messages must be in English.**
Exception: Polish KSeF-specific terms (`faktura`, `NIP`, `PESEL`, `UPO`) are untranslatable domain identifiers.

## Build and test commands

```bash
# Full build + all tests (no live KSeF access needed)
mvn clean verify --no-transfer-progress

# Static analysis (must pass with 0 violations)
mvn spotbugs:check pmd:check checkstyle:check -pl ksef-client --no-transfer-progress

# Run a single test class
mvn test -pl ksef-client -Dtest=SessionClientTest --no-transfer-progress

# Run a single test method
mvn test -pl ksef-client -Dtest=SessionClientTest#openOnline_whenChallengeSucceeds_returnsSession --no-transfer-progress

# SDK module only
mvn clean verify -pl ksef-client --no-transfer-progress

# Run sample demo app (requires ksef-credentials.properties in project root)
mvn install -pl ksef-client -DskipTests -Dmaven.javadoc.skip=true --no-transfer-progress
mvn exec:java -pl ksef-demo -Ddemo.mode=AUTH_SAFE

# IMPORTANT: after changing ksef-client code, run `mvn install` before `mvn exec:java` on ksef-demo
# mvn exec:java resolves from ~/.m2/repository, NOT from target/classes
```

## Architecture

### Maven multi-module structure

Root `pom.xml` is a `<packaging>pom</packaging>` aggregator. Modules:
- `ksef-client` — SDK jar. OpenAPI Generator plugin produces REST models into `target/generated-sources/openapi/`. JAXB XJC generates invoice XML models from XSD into `target/generated-sources/xjc/`. Configures GPG signing and Maven Central publishing.
- `ksef-demo` — consumer app. Depends on `ksef-client`. `<maven.deploy.skip>true</maven.deploy.skip>` — never published.

Both modules independently configure SpotBugs, PMD, and Checkstyle (shared config files at project root). No shared `<pluginManagement>` in root pom.

### Layered architecture (ADR-012)

```
┌─────────────────────────────────────────────────────────────────┐
│  Consumer code                                                   │
│  Uses: KsefClient, builders, sdk.domain.*.model records          │
├─────────────────────────────────────────────────────────────────┤
│  Layer 3: Public API surface                                     │
│  sdk/KsefClient.java — single entry point + domain accessors     │
│  sdk/domain/<feature>/ — 8 functionality buckets, each with      │
│    headline types + builder/ + model/                            │
│  sdk/{config,common,exception}/ — sentinel utility packages      │
├─────────────────────────────────────────────────────────────────┤
│  Layer 2: Internal mechanisms (NOT exported via JPMS)            │
│  sdk/internal/client/<area>/  — endpoint wrappers used by        │
│    KsefClient under the hood (auth, session, security)           │
│  sdk/internal/runtime/<purpose>/ — cross-cutting plumbing        │
│    (transport, crypto, signing, batch helper)                    │
│  HttpRuntime narrow interface (ADR-013) breaks transport→facade  │
│    layering inversion; ApiPaths centralises REST paths (ADR-014) │
├─────────────────────────────────────────────────────────────────┤
│  Layer 1: Generated code (NOT exported via JPMS)                 │
│  client.model.*Raw — OpenAPI-generated mutable POJOs             │
│  xml.model.*       — JAXB-generated invoice XML models           │
└─────────────────────────────────────────────────────────────────┘
```

**Key design rule (ADR-005):** Consumers never import from `client.*` packages. All domain client public methods return immutable records from `sdk.domain.*.model`. Generated `*Raw` types are internal implementation detail.

### Package structure (ADR-012)

```
ksef-client/src/main/java/io/github/mgrtomaszzurawski/ksef/
├── sdk/
│   ├── KsefClient.java                  # entry point at sdk/ root
│   ├── KsefClientInternals.java         # @Deprecated test seam (moves to ksef-client-testkit in 0.2.x)
│   ├── package-info.java
│   ├── config/                          # KsefEnvironment, KsefIdentifier, RetryPolicy
│   ├── common/                          # StatusInfo, TokenInfo, PublicKeyCertificate(Usage)
│   ├── exception/                       # 10 typed exceptions (KsefException + subclasses)
│   │
│   ├── domain/                          # PUBLIC functionality buckets (8)
│   │   ├── authentication/  # KsefCredentials + 3 impls (Token, Pkcs12, Certificate)
│   │   │   └── model/       # 10 auth-flow records
│   │   ├── invoicing/       # FormCode, KsefSession, KsefBatchSession, InvoiceClient,
│   │   │   │                  PreparedInvoiceExport, ExportedInvoicePackage
│   │   │   ├── builder/     # 5 builders (online/batch session, send invoice, query, export)
│   │   │   ├── model/       # 27 records
│   │   │   ├── batch/       # BatchFileSpec, PreparedBatchPackage
│   │   │   └── qrcode/      # QrCodeService, KsefVerificationLinks, QrEnvironment, QrContextType
│   │   ├── permissions/     # PermissionClient
│   │   │   ├── builder/     # 12 builders
│   │   │   └── model/       # 22 records
│   │   ├── tokens/          # TokenClient
│   │   │   ├── builder/     # 2 builders
│   │   │   └── model/       # 8 records
│   │   ├── certificates/    # CertificateClient
│   │   │   ├── builder/     # 3 builders
│   │   │   └── model/       # 13 records
│   │   ├── peppol/          # PeppolClient + model/ (3 records)
│   │   ├── limits/          # LimitsClient, RateLimitClient + model/ (5 records)
│   │   └── testdata/        # TestDataClient
│   │       ├── builder/     # 8 builders
│   │       └── model/       # 5 records
│   │
│   └── internal/                        # NOT exported via JPMS
│       ├── client/                      # endpoint wrappers used only by KsefClient
│       │   ├── auth/        # AuthClient, SessionContext
│       │   ├── session/     # SessionClient
│       │   └── security/    # SecurityClient
│       └── runtime/                     # cross-cutting infrastructure
│           ├── transport/   # HttpSupport, HttpRuntime, RetryHandler, ApiPaths
│           ├── crypto/      # CryptoService, CertificateLoader
│           ├── signing/     # SigningService
│           └── batch/       # BatchPackageBuilder
│
└── client/                              # Generated code (NOT exported)
    └── model/                           # OpenAPI *Raw classes
```

**Sub-split convention (ADR-012):** Inside any `domain/<feature>/` bucket,
*headline* types (clients, credentials, session abstractions) live at the
bucket root; *builders* live under `<bucket>/builder/`; *records* live
under `<bucket>/model/`. Sentinel packages (`config/`, `common/`,
`exception/`) stay flat — each holds one *kind* of type.

### Model generation

**REST API models (OpenAPI):**
The OpenAPI generator produces low-level HTTP client code at build time. Generated classes live in `client.api` and `client.model` packages with `Raw` suffix. Never edited by hand. Consumers never see these — domain clients map them to `sdk.domain.<feature>.model` records via `from()` factory methods.

**Invoice XML models (XSD → JAXB):**
JAXB XJC generates Java classes from official KSeF XSD schemas (FA(2), FA(3), PEF, RR, UPO). These model the XML invoice structure, separate from the REST API models.

### Encryption flow

KSeF uses a two-level encryption scheme:

1. **Session level** — `OnlineSessionBuilder.build()` generates AES key + IV, encrypts AES key with KSeF RSA public key (SymmetricKeyEncryption cert), sends encrypted key with session open request. Returns `SessionOpenResult` containing the request AND the AES key/IV.
2. **Invoice level** — `SendInvoiceBuilder.create(xml, aesKey, iv)` encrypts invoice content with the SAME AES key established at session open. The server already has the key from step 1.

**Critical:** Each invoice within a session MUST be encrypted with the session's AES key, NOT a new key. `SendInvoiceBuilder` accepts `aesKey` and `initVector` parameters, not a `PublicKey`.

### Authentication flow

KSeF uses a challenge-response flow, NOT simple API keys:
1. `POST /auth/challenge` — get encryption challenge
2. Sign challenge with XAdES qualified signature OR encrypt token with KSeF public key
3. `POST /auth/xades-signature` or `POST /auth/ksef-token` — exchange for operation token
4. Poll `GET /auth/{ref}` until status 200
5. `POST /auth/token/redeem` — exchange operation token for access + refresh tokens
6. Use Bearer token for all subsequent requests within session
7. Session has timeout — must handle expiry and refresh

Simplified: pass a `KsefCredentials` to the `KsefClient` builder; the
client lazily authenticates on the first call and re-authenticates on
HTTP 401 (driven by `HttpRuntime.reauthenticate()`).

### JPMS

The SDK is a named Java module (`io.github.mgrtomaszzurawski.ksef`).
Every package outside `sdk.internal.*` is exported; the `internal` tree
is invisible to consumers (ADR-005, ADR-012):

- `sdk` — `KsefClient` (entry point)
- `sdk.config` — `KsefEnvironment`, `KsefIdentifier`, `RetryPolicy`
- `sdk.common` — `StatusInfo`, `TokenInfo`, public-key types
- `sdk.exception` — typed exception hierarchy (10 types)
- `sdk.domain.<feature>` + `.builder` + `.model` — one set per
  functionality bucket (authentication, invoicing, permissions,
  tokens, certificates, peppol, limits, testdata)

NOT exported:
- `sdk.internal.client.*` — endpoint wrappers (`AuthClient`,
  `SessionClient`, `SecurityClient`, `SessionContext`)
- `sdk.internal.runtime.*` — cross-cutting plumbing (`HttpSupport`,
  `HttpRuntime`, `RetryHandler`, `ApiPaths`, `CryptoService`,
  `CertificateLoader`, `SigningService`, `BatchPackageBuilder`)
- `client.model` — opened to Jackson only for deserialization

## Code patterns and conventions

### Domain client pattern

Every domain client:
1. Takes `HttpRuntime` in constructor (the narrow transport interface from
   ADR-013), wraps it in a `new HttpSupport(runtime)`. `KsefClient` no longer
   leaks itself into impl ctors — see commit `f958189`.
2. Defines `private static final String` path constants built from
   `ApiPaths.<DOMAIN>` (ADR-014); never hardcode `/api/v2` literals
3. For dynamic segments use `ApiPaths.subPath(base, segments...)` —
   handles separator placement consistently across all clients
4. Public methods call `http.getAuthenticated/postJsonAuthenticated(...)` with Raw types
5. Maps Raw response to immutable record via `RecordType.from(raw)` before returning
6. Methods returning void (delete, close) do not need mapping

### Immutable record pattern

```java
public record SessionStatus(StatusInfo status, OffsetDateTime dateCreated, ...) {
    public static SessionStatus from(SessionStatusResponseRaw raw) {
        return new SessionStatus(
            StatusInfo.from(raw.getStatus()),
            raw.getDateCreated(), ...);
    }
}
```

- `from()` factory is the only way to create instances
- Null-safe: `from()` handles nullable raw fields
- Collections are always non-null (use `List.of()` for empty)
- Nested records also have `from()` factories
- Enums map via `switch` expression

### Builder pattern

Builders enforce required fields and validate constraints discovered by the server validation probe (Phase 7.0):
- Factory methods encode choices: `InvoiceQueryBuilder.seller()`, `OnlineSessionBuilder.fa2(key)`
- Required fields fail-fast with `Objects.requireNonNull` or `IllegalStateException`
- Server-side constraints replicated: description 5-256 chars, dateRange max 3 months, etc.
- Builders return SDK domain records (e.g. `OnlineSessionOpenRequest`, `TokenGenerateRequest`); domain clients map records to `*Raw` internally via package-private mappers

### KSeF server error mechanism

Server (.NET backend) returns structured validation errors:
- **exceptionCode 21405** — per-field validation, dot-notation paths (`filters.dateRange.from`)
- **exceptionCode 21001** — JSON parsing (wrong types, invalid enums)
- Server validates all fields in one pass, returns all errors at once
- Unknown fields silently ignored
- Full documentation in `context/validation/`

## Test patterns

- **WireMock** for all domain client HTTP tests (mock responses, verify requests)
- **Naming convention:** `methodUnderTest_whenScenario_expectedResult`
- **Structure:** given/when/then with `// given`, `// when`, `// then` markers
- **391 tests** total across 33 test classes (surefire count, includes parameterized expansions)
- No live KSeF calls in tests — all mocked
- `TestCertificates.java` provides test X.509 certs/keys for crypto tests

## Key conventions

### KSeF-specific terminology

| Term | Meaning |
|------|---------|
| NIP | Polish tax ID (10 digits, checksum-validated by server) |
| PESEL | Polish personal ID (11 digits, checksum-validated by server) |
| UPO | Urzędowe Poświadczenie Odbioru — official receipt confirming invoice delivery |
| FA(2), FA(3) | Invoice schema versions |
| KSeF number | Unique invoice ID assigned by KSeF system (format: YYYYMMDD-XX-...) |
| Reference number | Session/operation tracking ID (format: YYYYMMDD-XX-...) |
| Online session | Interactive invoice submission (one at a time) |
| Batch session | Bulk invoice submission (ZIP package) |
| Raw types | Generated `*Raw` classes — internal, never in public API |

### Security model

- Bearer JWT tokens (obtained via challenge-response or KSeF token)
- Invoices encrypted with AES-256, key wrapped with RSA using KSeF public key
- Session AES key reused for all invoices within session
- XAdES-BASELINE-B signatures for authentication
- Test certificates available in CIRFMF/ksef-docs repo
- KSeF demo environment: `https://api-demo.ksef.mf.gov.pl`

### Known server behaviors (discovered in testing)

- Session cooldown: ~30-60s after termination before new session can open for same NIP
- Status 415 = session busy, cannot send invoices
- `certificates/enrollments` crashes on invalid certificateType (server bug)
- `subjectDetails` required for permission grants despite spec saying optional
- `pageSize` not validated on permission query endpoints
- Full findings in `context/validation/` and `context/RCA/`

## ADRs

Architectural decisions in `ADR/`. Consult before making changes.

- **ADR-005** — SDK overlay on generated code: immutable records as public API, `*Raw` types internal
- **ADR-008** — API redesign: `KsefSession` / `KsefBatchSession` session abstractions
- **ADR-011** — Batch encryption (AES-256-CBC + PKCS#7) and polling semantics
- **ADR-012** — Package structure: `domain/<feature>/` for functionality, `internal/{client,runtime}/` for plumbing
- **ADR-013** — `HttpRuntime` narrow interface — breaks the transport→facade layering inversion
- **ADR-014** — `ApiPaths` centralisation — single source of truth for REST paths and version

## Additional context

**Spec source:** CIRFMF/ksef-docs GitHub repository (public). Contains OpenAPI spec, XSD schemas, auth documentation, test certificates, and test scenarios.

**Official SDK comparison:** The official CIRFMF/ksef-client-java SDK has 276 hand-written model classes (NOT generated from their own OpenAPI spec), a single god-class client, no retry, no pagination abstraction. This project generates from specs instead of wrapping the official SDK.

**Test environment:** `https://api-demo.ksef.mf.gov.pl` — KSeF demo environment. Credentials in gitignored `ksef-credentials.properties`.

**NoviCloud reference:** `/workspace/novicloud-client-java/` — reference SDK with similar patterns (adapted, not copied).

**Implementation plan:** `context/PLAN-2026-04-03-2045-implementation-plan.md` — phases 0-9 with current status.

**Session reports:** `context/REPORT-*.md` — detailed reports from each development session with decisions, missteps, and findings.

**Validation probe results:** `context/validation/` — per-endpoint server validation behavior documentation.

**RCA documents:** `context/RCA/` — root cause analysis for every bug found during development/testing.
