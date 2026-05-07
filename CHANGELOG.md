# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — planned

First public release. Targets KSeF API 2.4.0 (per
`ksef-docs/api-changelog.md`). The artefact has not been staged on
Maven Central yet; the dependency snippets in the README show the
coordinates that will resolve once Central publication completes.

### Added

#### Authentication
- `KsefClient.authenticate()` lazy challenge-response flow with auto-retry
  on HTTP 401. Token, certificate, and PKCS#12 credentials supported.
- VAT-UE / EU-entity authentication via `KsefCertificateCredentials` with
  `NipVatUe`-context `KsefIdentifier`.
- `AuthorizationPolicy` (IP allow-list) emitted in both XAdES
  `<AuthorizationPolicy>` element and the token-flow JSON request.
- `streamAuthSessions()` paginator over `GET /auth/sessions`, walking
  `x-continuation-token` lazily.
- `KsefClient.lastChallengeClientIp()` for autopinning IP allow-lists.
- `refreshAuthToken()` manual refresh and `terminateAuthSession(ref)`.

#### Sessions and invoicing
- `KsefSession` (online) and `KsefBatchSession` (batch) try-with-resources
  abstractions; `close()` polls until terminal and surfaces failures as
  typed `KsefSessionTerminalFailureException` / `KsefSessionPollingTimeoutException`.
- `SendInvoiceCommand` sealed interface — `Normal(byte[])` and
  `TechnicalCorrection(byte[], byte[] hashOfCorrected)` (per
  `ksef-docs/offline/korekta-techniczna.md`).
- `BatchSessionOptions` with `online()`, `offline()`, and assembly modes
  (`onDisk(Path)`, `inMemory(maxBytes)`).
- `PreparedBatchPackage` validates aggregate caps client-side
  (`KsefLimits.MAX_BATCH_PARTS = 50`, `MAX_BATCH_TOTAL_BYTES = 5 GB`).
- `PreparedInvoiceExport` streaming export with file-backed extraction,
  ZIP-bomb caps, SHA-256 verification, AES key zeroisation on close.
- `FormCode.assertAllowedOn(env)` — client-side preflight blocking
  `FA(2)` on DEMO/PROD before reaching the wire.
- Cooldown 415 detection via `KsefSessionCooldownException` (server quirk
  documented in `KNOWN-SERVER-BEHAVIORS.md`).

#### Invoice retrieval
- `InvoiceClient.queryInvoicesByMetadata(filter)` — single-page snapshot
  with `InvoiceQueryFilters` (16 fields including amount, buyerIdentifier,
  formType, invoiceTypes).
- `InvoiceClient.streamInvoicesByMetadata(filter)` — lazy paginator using
  `permanentStorageHwmDate` cursor; bound memory with `.limit(N)` or
  `.takeWhile(...)`.
- `InvoiceClient.getByKsefNumber(KsefNumber)` typed input with CRC-8
  client-side validation (REQ-SESS-18/19/20).
- `InvoiceSyncClient` — HWM-based incremental sync with `CheckpointStore`
  + `InvoiceSink`; mandates `PERMANENT_STORAGE` axis per spec.

#### Permissions
- 10 grant builders (`PersonPermissionGrantBuilder`,
  `EntityPermissionGrantBuilder`, `EuEntityAdminPermissionGrantBuilder`,
  `EuEntityPermissionGrantBuilder`, `IndirectPermissionGrantBuilder`,
  `SubunitPermissionGrantBuilder`, `EntityAuthorizationPermissionGrantBuilder`,
  ...).
- 8 stream paginators (`streamPersons`, `streamEntities`, `streamSubunits`,
  `streamPersonal`, `streamEuEntities`, `streamAuthorizations`, ...).
- 4 query builders for the previously no-arg endpoints
  (`SubunitPermissionsQueryBuilder`, `EntityPermissionsQueryBuilder`,
  `EntityRolesQueryBuilder`, `SubordinateEntityRolesQueryBuilder`).
- `*AndAwait` async helpers for grant and revoke flows.

#### Tokens
- `TokenGenerateBuilder` + `generateAndAwait(...)` async helper.
- `TokenQueryBuilder` with five filter parameters from spec
  (`status[]`, `description`, `authorIdentifier`,
  `authorIdentifierType`, `pageSize`); `streamTokens(filter)` honours
  the filter on every page.

#### Certificates
- `CertificateEnrollBuilder` with `*AndAwait` helper.
- `CertificateQueryBuilder` with `pageOffset` / `pageSize` setters.
- Certificate quota visible via `client.certificates().getLimits()`
  (12 enrolments / 6 active per taxpayer).

#### QR codes (KOD I + KOD II)
- `KsefVerificationLinks.canonicalCertificateSigningPayload(input)`
  returns the bytes the offline KSeF certificate must sign — caller
  signs with their own crypto stack (PKI-neutral, ADR-019).
- `QrSigningService` for owns-key flow (RSA-PSS / ECDSA P-256
  auto-detected).
- `QrCodeService` PNG generation with KSeF-number label.

#### Public crypto facade
- `sdk.crypto.KsefCryptoService` — byte and stream encrypt/decrypt,
  RSA-OAEP-SHA256, AES-256-CBC + PKCS#7, CSR generation (RSA + EC P-256),
  file metadata.
- `EncryptionMaterial`, `KsefEncryptionInfo`, `FileMetadata`,
  `CsrRequest`, `CsrResult` records.

#### Configuration and infrastructure
- `KsefClient.builder()` no-arg + required `.environment(...)` and
  `.credentials(...)` setters validated at `build()` (matches AWS / Spring
  / Azure SDK idiom).
- `KsefEnvironment.{TEST, DEMO, PROD, custom(url)}` (constants match
  `ksef-docs/srodowiska.md`).
- `RetryPolicy` with exponential backoff + `ThreadLocalRandom` jitter,
  `retryOn5xx`, `retryOn429`, `retryPost=false` default,
  `maxRetryAfterSeconds=60`.
- `FeaturePolicy` with `UpoVersion`, `problemDetails`, `xadesStrict`,
  `enforceXadesCompliance` (api-changelog v2.1.1).
- Stream paginators over offset-based and cursor-based pagination
  (ADR-023). Bounded memory; AWS-SDK-style.

#### Quality + safety
- JSpecify null-safety annotations across the public API
  (`@NullMarked` per package, `@Nullable` per field/return as needed).
- AES key zeroisation on `KsefSession.close()` and
  `PreparedInvoiceExport.close()` (CWE-316).
- Path-segment hardening (`requireSafePathSegment`) on every endpoint
  carrying reference numbers.
- Transport-level URI redaction masks 10-digit decimal segments before
  they hit DEBUG logs and `KsefException` messages (ADR-027).
- XXE hardening on `KsefXmlValidator` (ADR-029).
- License files (`LICENSE.txt`, `THIRD-PARTY-NOTICES.md`) shipped in
  the published JAR under `META-INF/`.

### Changed

- N/A — first public release.

### Removed

- N/A — first public release.

### Security

- AGPL-3.0-only retained (per revised ADR-007 — original Apache-2.0 plan
  deprecated).
- AES-256-CBC + PKCS#7 (NOT GCM) for invoice payloads, RSA-OAEP-SHA256
  for AES key wrap, XAdES-BASELINE-B for auth, ECDSA P-256 / RSA-PSS for
  KOD II QR signing.
- Bearer JWT tokens redacted in DEBUG logs; bodies never logged.
- Defensive byte-array cloning in cryptographic records (CSR result,
  encryption material, signed verification params).
- HTTPS-only assertion on presigned upload/download URLs.
- 410 Gone surfaced as `KsefRetentionExpiredException` (subtype of
  `KsefNotFoundException`) for retention-expired endpoints
  (auth 7d, exports 7d, certificates 30d, permissions 30d).

### Known limitations

- `HttpClient` lifecycle: the JDK 17 baseline cannot call
  `HttpClient.close()` (added in JDK 21). The shared `HttpClient` is
  left to JVM cleanup. Will be addressed when the JDK baseline is bumped.

### Architectural decisions

ADRs ([`ADR/`](ADR/)):

- **ADR-001** — generate from specs, do not wrap the official SDK.
- **ADR-002** — Java 17 baseline.
- **ADR-003** — OpenAPI-first for REST, XSD for invoice XML.
- **ADR-004** — domain-specific clients (vs single god-class).
- **ADR-005** — SDK overlay on generated code (immutable records as public API).
- **ADR-006** — separate SDK and sample-app modules.
- **ADR-007** — licence strategy (AGPL-3.0-only retained at 1.0.0).
- **ADR-008** — API redesign: `KsefSession` / `KsefBatchSession` abstractions.
- **ADR-009** — demo app purpose shift to live-validation harness.
- **ADR-010** — SDK functional completeness audit cycle.
- **ADR-011** — batch encryption (AES-256-CBC + PKCS#7) and polling semantics.
- **ADR-012** — package structure: `domain/<feature>/`, `internal/{client,runtime}/`.
- **ADR-013** — `HttpRuntime` narrow interface (transport→facade layering fix).
- **ADR-014** — `ApiPaths` centralisation.
- **ADR-015** — trust the spec on `@Nonnull` fields, carve-out via RCA.
- **ADR-016** — `KsefClient` is the only entry point.
- **ADR-017** — JSpecify null-safety annotations on the public API.
- **ADR-018** — `*Raw` types on internal bridge methods.
- **ADR-019** — KOD II signing scheme: PKI-neutral plus owns-key convenience.
- **ADR-020** — testkit philosophy.
- **ADR-021** — public API tiers.
- **ADR-022** — REQ-ID citation discipline for spec-touching changes.
- **ADR-023** — `Stream<T>` paginators replace materialized `queryAll*` / `listAll`.
- **ADR-024** — cross-package construction via reflective bridge.
- **ADR-025** — batch assembly mode and temp-file hygiene.
- **ADR-026** — AES key zeroisation on close.
- **ADR-027** — transport-level URI redaction.
- **ADR-028** — JPMS public-API defence gates.
- **ADR-029** — XXE hardening on XML validator.

### Server-side behaviours worth knowing

KSeF has documented divergences from spec that consumers will hit at
runtime: session cooldown after termination, asynchronous
authentication, certificate quota, retention as HTTP 410. The SDK
either handles them transparently or surfaces them as typed
exceptions. Curated list in
[`KNOWN-SERVER-BEHAVIORS.md`](KNOWN-SERVER-BEHAVIORS.md).

[1.0.0]: https://github.com/mgrtomaszzurawski/ksef-java-sdk/commits/develop
