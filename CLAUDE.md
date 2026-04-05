# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## What this is

OpenAPI-first Java SDK for the Polish National e-Invoicing System (KSeF) REST API v2. Multi-module Maven project:
- `ksef-client` — the SDK library (published to Maven Central)
- `ksef-sample` — integration examples and smoke tests (not published)

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
mvn exec:java -pl ksef-sample -Ddemo.mode=AUTH_SAFE

# IMPORTANT: after changing ksef-client code, run `mvn install` before `mvn exec:java` on ksef-sample
# mvn exec:java resolves from ~/.m2/repository, NOT from target/classes
```

## Architecture

### Maven multi-module structure

Root `pom.xml` is a `<packaging>pom</packaging>` aggregator. Modules:
- `ksef-client` — SDK jar. OpenAPI Generator plugin produces REST models into `target/generated-sources/openapi/`. JAXB XJC generates invoice XML models from XSD into `target/generated-sources/xjc/`. Configures GPG signing and Maven Central publishing.
- `ksef-sample` — consumer app. Depends on `ksef-client`. `<maven.deploy.skip>true</maven.deploy.skip>` — never published.

Both modules independently configure SpotBugs, PMD, and Checkstyle (shared config files at project root). No shared `<pluginManagement>` in root pom.

### Three-layer architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Consumer code                                                   │
│  Uses: KsefOperations, builders, sdk.model records               │
├─────────────────────────────────────────────────────────────────┤
│  Layer 3: High-level facade (sdk/)                               │
│  KsefOperations — multi-step workflows as single calls           │
│  Builders (sdk/model/builder/) — validated request construction  │
├─────────────────────────────────────────────────────────────────┤
│  Layer 2: Domain clients (sdk/)                                  │
│  10 clients: Auth, Session, Invoice, Permission, Token,          │
│  Certificate, Limits, RateLimit, Security, TestData              │
│  Returns: immutable records (sdk/model/)                         │
│  Accepts: Raw request types (until builders replace them)        │
│  + Cross-cutting: RetryHandler, SessionContext, HttpSupport,     │
│    CryptoService, SigningService, QrCodeService, exceptions      │
├─────────────────────────────────────────────────────────────────┤
│  Layer 1: Generated code (NOT exported via JPMS)                 │
│  client.model.*Raw — OpenAPI-generated mutable POJOs (273 types) │
│  xml.model.* — JAXB-generated invoice XML models                 │
└─────────────────────────────────────────────────────────────────┘
```

**Key design rule (ADR-005):** Consumers never import from `client.*` packages. All domain client public methods return immutable records from `sdk.model`. Generated `*Raw` types are internal implementation detail.

### Package structure

```
ksef-client/src/main/java/io/github/mgrtomaszzurawski/ksef/
├── sdk/                          # Hand-written SDK layer
│   ├── KsefClient.java           # Builder + entry point, exposes domain clients
│   ├── KsefOperations.java       # High-level facade (auth+send in one call)
│   ├── KsefEnvironment.java      # TEST, PREPROD, PROD, custom(url)
│   ├── SessionContext.java       # Thread-safe JWT holder (atomic, TTL)
│   ├── RetryHandler.java         # Retry with backoff + jitter
│   ├── RetryPolicy.java          # Configurable retry settings
│   ├── AuthClient.java           # 9 auth operations
│   ├── SessionClient.java        # 13 session operations
│   ├── InvoiceClient.java        # 4 invoice operations + queryAllMetadata
│   ├── PermissionClient.java     # 19 permission operations
│   ├── TokenClient.java          # 4 token operations
│   ├── CertificateClient.java    # 7 certificate operations
│   ├── LimitsClient.java         # 2 limit queries
│   ├── RateLimitClient.java      # 1 rate limit query
│   ├── SecurityClient.java       # 1 public key query
│   ├── TestDataClient.java       # 17 test data operations
│   ├── QrCodeService.java        # QR code generation (ZXing)
│   ├── crypto/
│   │   ├── CryptoService.java    # AES-256 encryption + RSA key wrapping
│   │   └── CertificateLoader.java # PKCS#12 / PEM loading
│   ├── signing/
│   │   └── SigningService.java   # XAdES-BASELINE-B (EU DSS)
│   ├── http/
│   │   └── HttpSupport.java      # HTTP client, auth headers, path validation
│   ├── exception/
│   │   ├── KsefException.java    # Base (statusCode, responseBody)
│   │   ├── KsefAuthException.java        # 401, 403
│   │   ├── KsefNotFoundException.java    # 404
│   │   ├── KsefRateLimitException.java   # 429 + Retry-After
│   │   ├── KsefServerException.java      # 5xx
│   │   ├── KsefNetworkException.java     # I/O, timeout
│   │   ├── KsefSessionExpiredException.java
│   │   └── KsefCryptoException.java
│   ├── model/                    # Immutable response records (81 types)
│   │   ├── StatusInfo.java       # Shared: status code + description
│   │   ├── TokenInfo.java        # Shared: token + validUntil
│   │   ├── AuthenticationChallenge.java
│   │   ├── SessionStatus.java
│   │   ├── InvoiceMetadata.java
│   │   ├── ...                   # 78 more records
│   │   └── builder/              # Request builders with validation
│   │       ├── InvoiceQueryBuilder.java
│   │       ├── OnlineSessionBuilder.java  # Returns SessionOpenResult (request + AES key/IV)
│   │       ├── SendInvoiceBuilder.java    # Accepts session AES key, not PublicKey
│   │       ├── InvoiceExportBuilder.java
│   │       ├── TokenGenerateBuilder.java
│   │       └── PersonPermissionGrantBuilder.java
│   └── paging/
│       └── PagedResponse.java    # Unused — KSeF uses date-cursor pagination
└── client/                       # Generated code (NOT exported)
    └── model/                    # 273 *Raw classes from OpenAPI
```

### Model generation

**REST API models (OpenAPI):**
The OpenAPI generator produces low-level HTTP client code at build time. Generated classes live in `client.api` and `client.model` packages with `Raw` suffix. Never edited by hand. Consumers never see these — domain clients map them to `sdk.model` records via `from()` factory methods.

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

Simplified: `KsefOperations.authenticateWithToken()` does steps 1-5 in one call.

### JPMS

The SDK is a named Java module (`io.github.mgrtomaszzurawski.ksef`). Exported packages:
- `sdk` — domain clients, KsefClient, KsefOperations, KsefEnvironment
- `sdk.model` — 81 immutable response records
- `sdk.model.builder` — 6 request builders
- `sdk.exception` — typed exception hierarchy
- `sdk.crypto` — CryptoService, CertificateLoader
- `sdk.signing` — SigningService
- `sdk.http` — HttpSupport
- `sdk.paging` — PagedResponse (unused)

NOT exported: `client.model` (opened to Jackson only for deserialization).

## Code patterns and conventions

### Domain client pattern

Every domain client:
1. Takes `KsefClient` in constructor, extracts `HttpSupport` and `SessionContext`
2. Defines `private static final String` constants for API paths and operation names
3. Public methods call `http.getAuthenticated/postJsonAuthenticated(...)` with Raw types
4. Maps Raw response to immutable record via `RecordType.from(raw)` before returning
5. Methods returning void (delete, close) do not need mapping

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
- Builders return Raw request types (consumed by domain clients)

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
- **180 tests** total across 21 test classes
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

## Additional context

**Spec source:** CIRFMF/ksef-docs GitHub repository (public). Contains OpenAPI spec, XSD schemas, auth documentation, test certificates, and test scenarios.

**Official SDK comparison:** The official CIRFMF/ksef-client-java SDK has 276 hand-written model classes (NOT generated from their own OpenAPI spec), a single god-class client, no retry, no pagination abstraction. This project generates from specs instead of wrapping the official SDK.

**Test environment:** `https://api-demo.ksef.mf.gov.pl` — KSeF demo environment. Credentials in gitignored `ksef-credentials.properties`.

**NoviCloud reference:** `/workspace/novicloud-client-java/` — reference SDK with similar patterns (adapted, not copied).

**Implementation plan:** `context/PLAN-2026-04-03-2045-implementation-plan.md` — phases 0-9 with current status.

**Session reports:** `context/REPORT-*.md` — detailed reports from each development session with decisions, missteps, and findings.

**Validation probe results:** `context/validation/` — per-endpoint server validation behavior documentation.

**RCA documents:** `context/RCA/` — root cause analysis for every bug found during development/testing.
