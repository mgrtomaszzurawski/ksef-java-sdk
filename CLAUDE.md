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
```

## Architecture

### Maven multi-module structure

Root `pom.xml` is a `<packaging>pom</packaging>` aggregator. Modules:
- `ksef-client` — SDK jar. OpenAPI Generator plugin produces REST models into `target/generated-sources/openapi/`. JAXB XJC generates invoice XML models from XSD into `target/generated-sources/xjc/`. Configures GPG signing and Maven Central publishing.
- `ksef-sample` — consumer app. Depends on `ksef-client`. `<maven.deploy.skip>true</maven.deploy.skip>` — never published.

Both modules independently configure SpotBugs, PMD, and Checkstyle (shared config files at project root). No shared `<pluginManagement>` in root pom.

### Two-layer model generation

**REST API models (OpenAPI):**
The OpenAPI generator produces low-level HTTP client code at build time. Generated classes live in `client.api` and `client.model` packages with `Raw` suffix. Never edited by hand.

**Invoice XML models (XSD → JAXB):**
JAXB XJC generates Java classes from official KSeF XSD schemas (FA(2), FA(3), PEF, RR, UPO). These model the XML invoice structure, separate from the REST API models.

### SDK overlay

The hand-written SDK layer in `sdk/` wraps generated code and adds: retry, session management, exception mapping, typed builders, crypto, and signing.

**Endpoint domains** (from KSeF OpenAPI spec, 73 operations across 10 domains):

| Domain | Operations | Purpose |
|--------|-----------|---------|
| auth | 9 | Challenge-response auth, XAdES signature, token management |
| sessions | 13 | Online/batch session lifecycle, invoice send/receive, UPO |
| invoices | 4 | Query metadata, export, retrieve by KSeF number |
| permissions | 19 | Grant/revoke/query permissions (persons, entities, EU entities, subunits) |
| tokens | 4 | Generate, list, get, revoke API tokens |
| certificates | 7 | Certificate enrollment, revocation, querying |
| limits | 2 | Context and subject rate limits |
| rate-limits | 1 | API rate limit info |
| security | 1 | Public key certificates for encryption |
| testdata | 17 | Test environment data management (subjects, persons, permissions) |

Cross-cutting concerns:
- `RetryHandler` + `RetryPolicy` — configurable retry for 429/5xx with exponential backoff + jitter
- Exception hierarchy — typed exceptions mapped from HTTP status codes and KSeF error codes
- `CryptoService` — AES-GCM encryption + RSA-OAEP key exchange for invoice encryption
- `SigningService` — XAdES-BASELINE-B via EU DSS library for authentication
- `QrCodeService` — verification link QR codes via ZXing

### Authentication flow

KSeF uses a challenge-response flow, NOT simple API keys:
1. `POST /auth/challenge` — get encryption challenge
2. Sign challenge with XAdES qualified signature
3. `POST /auth/xades-signature` — exchange signed challenge for session token
4. Use Bearer token for all subsequent requests within session
5. Session has timeout — must handle expiry and refresh

Alternative: `POST /auth/ksef-token` for pre-generated KSeF token auth.

### JPMS

The SDK is a named Java module. `module-info.java` controls exports — generated code packages are NOT exported.

### OpenAPI spec

Source of truth: `ksef-client/openapi/open-api.json` (from CIRFMF/ksef-docs). Original spec is monolithic JSON — may be split into modular structure if needed.

### XSD schemas

Source of truth: `ksef-client/xsd/` (from CIRFMF/ksef-docs). Includes:
- `FA/` — invoice schemas FA(2), FA(3) + base types
- `PEF/` — Peppol e-invoice schemas (UBL-based)
- `RR/` — farmer invoice schemas
- `upo/` — UPO (official receipt) schema

## Code patterns and conventions

*To be established as implementation progresses. Will follow similar structure to documented patterns — one section per concern (client structure, builders, retry, exceptions, testing).*

## Test patterns

*To be established. Will use WireMock for HTTP mocking, Mockito for unit tests. KSeF test environment (api-test.ksef.mf.gov.pl) available for integration validation.*

## Key conventions

### KSeF-specific terminology

| Term | Meaning |
|------|---------|
| NIP | Polish tax ID (10 digits) |
| PESEL | Polish personal ID (11 digits) |
| UPO | Urzędowe Poświadczenie Odbioru — official receipt confirming invoice delivery |
| FA(2), FA(3) | Invoice schema versions |
| KSeF number | Unique invoice ID assigned by KSeF system |
| Reference number | Session/operation tracking ID |
| Online session | Interactive invoice submission (one at a time) |
| Batch session | Bulk invoice submission (ZIP package) |

### Security model

- Bearer JWT tokens (obtained via challenge-response or KSeF token)
- Invoices encrypted with AES-GCM, key wrapped with RSA-OAEP using KSeF public key
- XAdES-BASELINE-B signatures for authentication
- Test certificates available in CIRFMF/ksef-docs repo

## ADRs

Architectural decisions in `ADR/`. Consult before making changes — decisions document API constraints and design trade-offs.

## Additional context

**Spec source:** CIRFMF/ksef-docs GitHub repository (public). Contains OpenAPI spec, XSD schemas, auth documentation, test certificates, and test scenarios.

**Official SDK comparison:** The official CIRFMF/ksef-client-java SDK has 276 hand-written model classes (NOT generated from their own OpenAPI spec), a single god-class client, no retry, no pagination abstraction. This project generates from specs instead of wrapping the official SDK.

**Test environment:** `https://api-test.ksef.mf.gov.pl/v2` — KSeF TE (Test Environment). Requires test certificates for authentication.
