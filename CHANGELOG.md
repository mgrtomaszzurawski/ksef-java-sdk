# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

This is the first public release. The SDK has been developed iteratively against
the live KSeF demo environment; ADRs in `ADR/` document each architectural
decision in chronological order.

### Added (Codex follow-up)

- `PreparedInvoiceExport` — public handle returned from
  `client.invoices().prepareExport(...)` that retains the AES key + IV used
  during export init, polls the export status, downloads each
  `InvoicePackagePart`, verifies SHA-256 hashes, decrypts with the retained
  AES/IV, concatenates the parts back into a ZIP archive, unzips, and exposes
  `_metadata.json` + per-invoice XML bytes via `ExportedInvoicePackage`.
  Implements `AutoCloseable`; `close()` zeroises the retained AES key and IV
  (idempotent). `awaitReady()` and `downloadAndDecrypt(...)` reject post-close
  use.
- `KsefSessionTerminalFailureException` — thrown by
  `PreparedInvoiceExport.awaitReady()` (and the session pollers) when the
  server reports a terminal-but-non-200 status. Try-with-resources no longer
  collapses terminal failures into ambiguous "package missing" errors.
- `KsefVerificationLinks` — KSeF 2.0 KOD I (online invoice) and KOD II
  (offline-certificate) verification URL builders. KOD I:
  `qr-{env}.ksef.mf.gov.pl/invoice/{nip}/{DD-MM-YYYY}/{base64UrlSha256}`.
  KOD II: `qr-{env}.ksef.mf.gov.pl/certificate/{contextType}/{contextValue}/{nip}/{certSerial}/{hash}/{signature}`.
- `KsefVerificationLinks.canonicalCertificateSigningPayload(CertificateSigningInput)` —
  returns the UTF-8 bytes a KSeF offline certificate must sign for KOD II QR
  codes. The signed payload includes the QR host (without `https://` prefix)
  per `kody-qr.md`. New `CertificateSigningInput` record bundles the five
  no-signature parameters; SDK stays PKI-neutral (caller signs externally).
- `QrEnvironment` — `TEST` / `DEMO` / `PROD` enum exposing the official
  `qr-test.ksef.mf.gov.pl`, `qr-demo.ksef.mf.gov.pl`, `qr.ksef.mf.gov.pl` hosts.
- `QrContextType` enum (`NIP` / `INTERNAL_ID` / `NIP_VAT_UE` / `PEPPOL_ID`)
  replacing the previous raw `String contextType` parameter on
  `CertificateVerificationParams` and `CertificateSigningInput`. Each value
  carries its KSeF wire spelling (`Nip`, `InternalId`, `NipVatUe`, `PeppolId`)
  via `wireValue()`.
- `QrCodeService.addLabelToQrCode(byte[] qrPng, String label)` and
  `generateLabeledQrCode(payloadUrl, label)` — render the
  invoice-visualization labels (`LABEL_OFFLINE = "OFFLINE"`,
  `LABEL_CERTIFICATE = "CERTYFIKAT"`) below the QR per the official KSeF
  invoice-rendering spec.
- `KsefPkcs12Credentials.clearPassword()` zeroises the cloned password
  char-array; callers may invoke after authentication completes.
- `KsefSessionPollingTimeoutException` — thrown by `KsefSession.close()` and
  `KsefBatchSession.close()` when polling never reaches a terminal status
  within the polling budget. Try-with-resources no longer exits silently on
  indeterminate state.
- `KsefClientInternals` — deprecated public static seam exposing
  `runtime(KsefClient)` / `sessionContext(KsefClient)` for SDK-internal unit
  tests. Scheduled to move into a separate `ksef-client-testkit` artifact in
  0.2.x.
- `X-Error-Format: problem-details` header on every authenticated request, so
  KSeF returns structured RFC 7807 problem details on 4xx/5xx responses.
- `Retry-After` honored on HTTP 429 retries, clamped to
  `RetryPolicy.maxRetryAfterSeconds`. Now accepts RFC 7231 HTTP-date in all
  three permitted formats (RFC 1123, RFC 850, asctime); past dates collapse
  to immediate retry.

### Changed (Codex follow-up)

- `InvoiceClient.queryAllMetadata` no longer drops caller-supplied filters
  (`ksefNumber`, `invoiceNumber`, `sellerNip`, `invoicingMode`,
  `isSelfInvoicing`, `hasAttachment`, `amount`, `currencyCodes`,
  `buyerIdentifier`) between pages. Cursor advances by mutating
  `dateRange.from` only, preserving every other filter.
- Domain clients now obtain their access token via `HttpRuntime.requireToken()`,
  which authenticates proactively when no session exists. The first protected
  domain call no longer leaves with `Authorization: Bearer null`.
- `RetryHandler.runPost(...)` (and `runDelete` semantics) honors
  `RetryPolicy.retryPost(false)` for no-content POST/DELETE operations
  (session close, token revoke, certificate revoke, testdata mutators). Previously
  these used the GET-like retry path and ignored the policy.
- `QrCodeService` — rewritten as a generic URL → PNG renderer. Verification-URL
  construction moved to `KsefVerificationLinks`. Old `getVerificationUrl(...)`,
  `QrCodeService(boolean testEnvironment)` API removed; binary breaking change
  (no released consumers exist yet).
- `InvoiceMetadata`, `InvoicePackagePart`, `PublicKeyCertificate`,
  `KsefPkcs12Credentials` records now defensively clone `byte[]`/`char[]`
  fields in their compact constructors and accessor overrides, with
  `Arrays.equals`/`Arrays.hashCode` for structural equality.
- `ExportedInvoicePackage.equals/hashCode` made byte-array aware via custom
  helpers; two packages with identical XML bytes but different `byte[]`
  instances now compare equal.
- `PreparedInvoiceExport.downloadPart` rejects any non-GET HTTP method on
  `InvoicePackagePart` with a `KsefException` carrying the offending method
  name; `null` accepted as default-GET to match KSeF's typical responses.
- ZIP extraction in export enforces zip-bomb caps:
  `MAX_ZIP_ENTRIES = 100_000`, `MAX_ZIP_TOTAL_BYTES = 4 GiB`,
  `MAX_ZIP_ENTRY_BYTES = 256 MiB`. Exceeding any cap throws
  `KsefException` with the offending counter.
- Error messages embedding KSeF presigned package-part URLs now redact the
  query string before inclusion (avoids leaking signed query parameters in
  stack traces / logs).
- Targets KSeF API 2.4.0 (was 2.2.1). Upstream changes are additive: token-on-self
  permission operations, retention/410 Gone responses, optional Problem Details
  format, increased rate limits.

### Removed (Codex follow-up)

- `KsefClient.runtime()` removed from the public API (binary-breaking; no
  released consumers exist yet). Replaced by package-private
  `internalRuntime()`. Test code accesses the runtime through the new
  `KsefClientInternals.runtime(KsefClient)` static seam.
- Eleven internal client-impl ctors changed from `(KsefClient ksef)` to
  `(HttpRuntime runtime)` (`AuthClient`, `CertificateClientImpl`,
  `InvoiceClientImpl`, `LimitsClientImpl`, `RateLimitClientImpl`,
  `PeppolClientImpl`, `PermissionClientImpl`, `SecurityClient`,
  `SessionClient`, `TestDataClientImpl`, `TokenClientImpl`). These types
  live in `sdk.internal.client.*` (not exported via JPMS), so no public
  binary contract changes; documented for completeness.

### Deprecated (Codex follow-up)

- `KsefClient.activateSessionForTests(...)` annotated
  `@Deprecated(since = "0.1.0", forRemoval = true)`. The method stays
  reachable so the existing test fixtures keep compiling; planned to move
  into a dedicated `ksef-client-testkit` artifact in 0.2.x.
- `KsefClientInternals` annotated `@Deprecated(since = "0.1.0", forRemoval = true)`.
  Same migration target as `activateSessionForTests`.
- `InvoiceClient.exportInvoices(InvoiceExportBuilder)` annotated
  `@Deprecated(since = "0.1.0")` with javadoc redirecting to
  `prepareExport(query, fullContent)`. Legacy entry remains reachable for
  low-level use; IDE warnings surface the recommendation.

### Known limitations

- `Builder.build()` and `Record.from(*Raw)` bridge methods reference
  OpenAPI-generated `*Raw` types in their signatures. The `client.model`
  package is not exported via JPMS, so JPMS named-module consumers cannot
  invoke these methods directly. The documented public flows (passing a
  builder into a domain client method, receiving a record from a domain
  client method) are unaffected. See [ADR-018](ADR/ADR-018-raw-types-on-internal-bridge-methods.md);
  0.2.0 will eliminate `*Raw` from these signatures via SDK-owned request
  records and internal mapper extraction.

### Added

- OpenAPI-generated client for KSeF REST API v2 (60+ live operations across 11
  domains: authentication, sessions, invoicing, permissions, tokens,
  certificates, peppol, limits, rate-limits, test-data, security).
- JAXB-generated invoice models from official KSeF XSD schemas (FA(2), FA(3),
  PEF, RR, UPO).
- Single entry point: `KsefClient` with builder, lazy authentication, and
  per-domain accessors (`client.invoices()`, `client.permissions()`, ...) — see
  [ADR-016](ADR/ADR-016-ksef-client-single-entry-point.md).
- Layered package structure: `sdk/domain/<feature>/` for public API, immutable
  records via `from()` factories, generated `*Raw` types kept internal — see
  [ADR-005](ADR/ADR-005-sdk-overlay-on-generated-code.md) and
  [ADR-012](ADR/ADR-012-package-structure-domain-and-internal-split.md).
- Session abstractions `KsefSession` (online) and `KsefBatchSession` with
  `AutoCloseable` / try-with-resources support — see
  [ADR-008](ADR/ADR-008-api-redesign-session-abstraction.md).
- Two-level encryption flow: AES-256-CBC + PKCS#7 padding for invoice content,
  RSA-OAEP wrap of the session AES key with the KSeF SymmetricKeyEncryption
  cert — see [ADR-011](ADR/ADR-011-batch-encryption-and-polling-semantics.md).
- Authentication flows: KSeF token (RSA-encrypted), XAdES-BASELINE-B signature
  (PKCS#12 keystore or raw certificate + private key).
- Configurable retry: `RetryPolicy` with exponential backoff + full jitter, on
  5xx and 429 responses.
- Date-cursor pagination helper: `InvoiceClient.queryAllMetadata(...)` walks all
  pages using `permanentStorageHwmDate` as cursor.
- Typed exception hierarchy: `KsefAuthException`, `KsefServerException`,
  `KsefRateLimitException`, `KsefNotFoundException`,
  `KsefSessionExpiredException`, `KsefNetworkException`, `KsefCryptoException`,
  base `KsefException`.
- JPMS named module `io.github.mgrtomaszzurawski.ksef` with strict export
  boundaries — generated classes and `internal/` packages invisible to
  consumers.
- `HttpRuntime` narrow interface to break the transport→facade layering
  inversion — see [ADR-013](ADR/ADR-013-httpruntime-narrow-interface.md).
- `ApiPaths` centralisation — single source of truth for REST paths and version,
  bumping API major version is a one-line change — see
  [ADR-014](ADR/ADR-014-api-paths-centralization.md).
- ADR-015 "trust-the-spec" stance on `@Nonnull` fields: dead defensive
  null-checks on spec-required fields are removed; carve-outs documented in
  RCA where the server has been observed to violate its own spec.
- SLF4J logging: every domain client and internal REST client carries a
  `Logger`; `HttpSupport` logs every wire-level request/response at `DEBUG`
  with method, URI, status, elapsed time. Default consumer level: `WARN` (see
  README "Logging" section).
- 391 unit + integration tests across 33 test classes (WireMock-mocked HTTP).
- Demo / live-validation harness: `ksef-demo` module with per-domain runners and
  named modes (`AUTH_SAFE`, `READ_ONLY`, `FULL`, `CERTIFICATES`).
- Maven Central release profile (`mvn deploy -Prelease`) — GPG signing +
  Sonatype Central Portal upload.

### Architectural decisions

ADRs ([`ADR/`](ADR/)):

- **ADR-001** — generate from specs, do not wrap the official SDK.
- **ADR-002** — Java 17 baseline.
- **ADR-003** — OpenAPI-first for REST, XSD for invoice XML.
- **ADR-004** — domain-specific clients (vs single god-class).
- **ADR-005** — SDK overlay on generated code (immutable records as public API).
- **ADR-006** — separate SDK and sample-app modules.
- **ADR-007** — licence strategy (AGPL-3.0 pre-1.0 → Apache-2.0 at v1.0).
- **ADR-008** — API redesign: `KsefSession` / `KsefBatchSession` session
  abstractions.
- **ADR-009** — demo app purpose shift to live-validation harness.
- **ADR-010** — SDK functional completeness audit cycle.
- **ADR-011** — batch encryption (AES-256-CBC + PKCS#7) and polling semantics.
- **ADR-012** — package structure: `domain/<feature>/`,
  `internal/{client,runtime}/`.
- **ADR-013** — `HttpRuntime` narrow interface (transport→facade layering fix).
- **ADR-014** — `ApiPaths` centralisation.
- **ADR-015** — trust the spec on `@Nonnull` fields, carve-out via RCA.
- **ADR-016** — `KsefClient` is the only entry point.

### Known limitations

- `HttpClient` lifecycle: the JDK 17 baseline cannot call `HttpClient.close()`
  (added in JDK 21). The shared `HttpClient` is left to JVM cleanup. Will be
  addressed when the JDK baseline is bumped.
- Per-builder method coverage gate (PLAN A.9) is not at 1.00 yet —
  ~139 builder methods need explicit unit tests before the gate is enabled.
- JSpecify null-safety annotations on the public API are pending (PLAN A.8 /
  ADR-017).
- License switch to Apache-2.0 lands at the v1.0 tag (ADR-007), not in this
  pre-release.

[Unreleased]: https://github.com/mgrtomaszzurawski/ksef-java-sdk/commits/develop
