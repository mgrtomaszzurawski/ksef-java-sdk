# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-05-03

First public Maven Central release.

### Source-incompatible record changes (1.0.0 stabilisation)

- **`InvoiceQueryFilters`** — record component count grew from 10 to 16
  (added: `restrictToPermanentStorageHwm`, `amount`, `buyerIdentifier`,
  `currencyCodes`, `formType`, `invoiceTypes`). Source-incompatible only
  for callers that constructed the record positionally; the canonical
  path is `InvoiceQueryBuilder.build()`, which is updated.
- **`IncrementalSyncPlan`** — `dateType` record component REMOVED
  (HWM sync only works with `PERMANENT_STORAGE` per spec; an
  arbitrary-axis setter was unsafe and is gone). The `dateType()`
  accessor method survives as a constant-returning shim so existing
  read-side callers still compile; positional constructor invocations
  of the record will break.

### License decision

- **AGPL-3.0-only retained** at 1.0.0 (per revised ADR-007). The original
  ADR-007 had scheduled a switch to Apache 2.0 at this boundary; that
  decision was reconsidered and deprecated. Free-rider protection is the
  primary driver. A README disclaimer covers solo-maintenance and warranty
  scope independently of the license choice.

### Known server-side issues

These are KSeF server behaviours empirically observed during pre-1.0
validation that consumers will encounter at runtime. They are not SDK
bugs and not always reflected in the upstream `ksef-docs` repository.
The SDK either handles them transparently (where noted) or surfaces
them as typed errors so the consumer can react.

- **Online session cooldown after termination (~30-60 s).** Opening a
  new online session for the same NIP within ~30 seconds of a previous
  session's termination yields a session that opens with a reference
  number but enters status 415 immediately and rejects invoice
  submissions. SDK surfaces this via `KsefSessionCooldownException`
  on `openSession(...)` for fast-fail clarity (auto-retry with backoff
  is on the consumer's policy).
- **Session status 415 is the normal "open" state during processing.**
  After `close()`, the server returns 415 while batch validation is
  still in flight; this is not a failure. SDK polls until terminal
  inside `KsefSession.close()` / `KsefBatchSession.close()`.
- **Authentication is asynchronous (status 450 transient).** After
  posting `/auth/xades-signature` or `/auth/ksef-token`, the
  `redeemTokens` call returns HTTP 450 until the server finishes
  challenge processing. SDK polls `GET /auth/{ref}` internally before
  redeem; consumers see only the final outcome.
- **XAdES terminate requires prior token redeem.** Calling
  `terminateAuth()` immediately after async XAdES authentication
  (without `redeemTokens` between) returns HTTP 500. SDK guards this
  with state tracking in `SessionContext`.
- **Certificate serial number is not immediately available.**
  `enroll()` accepts the CSR and returns a reference number, but the
  certificate's serial number is populated asynchronously. Consumers
  must poll `getEnrollmentStatus(ref)` until terminal. (See `*AndAwait`
  helpers added in 1.0.)
- **Certificate operations require certificate-based authentication.**
  Token-based auth cannot reach the certificate enrollment endpoints.
  Consumers must authenticate with `KsefCertificateCredentials` /
  `KsefPkcs12Credentials` to use cert-domain APIs.
- **`certificates/enrollments` returns HTTP 500 on invalid
  `certificateType`.** Server crashes instead of returning a typed
  validation error. SDK validates the enum upstream so this only fires
  for callers bypassing the typed builder.
- **Permissions: `subjectDetails` required despite spec saying
  optional.** The `permissions/persons/grants` endpoint rejects
  requests without `subjectDetails` even though the OpenAPI schema
  marks it optional. SDK populates it from the subject identifier;
  external callers using the raw types must include it.

### Step 1 — Process foundation (this release)

- Four new ADRs governing 1.0.0 design:
  - **ADR-019** — KOD II signing scheme: RSA-PSS + ECDSA P-256 auto-detected
    from key type. Both `QrSigningService` (PrivateKey-aware) and the
    pre-existing canonical-payload flow (HSM/external signers) ship.
  - **ADR-020** — `ksef-client-testkit` philosophy: deprecated test seams
    relocate as test rewrites, not jar moves.
  - **ADR-021** — Public API tiers: Tier 1 workflow, Tier 2 endpoint, no
    Tier 3.
  - **ADR-022** — REQ-ID citation discipline: every spec-touching change
    cites a REQ-ID from `context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md`.
- `.github/PULL_REQUEST_TEMPLATE.md` enforces REQ-ID citation per ADR-022.
- New `ksef-examples` Maven module compiles all loose JBang-style scripts in
  `examples/` as part of `mvn verify`. Closes Codex round-7 finding A8
  (broken `QrCodeGeneration.java` no longer possible to drift undetected).

### Step 2 — Foundational types

- **`sdk.common.KsefNumber`** value object (REQ-SESS-18/19/20). Validates
  35-character length, separator placement, NIP digits, strict
  `uuuuMMdd` date, uppercase hex segments, and CRC-8 checksum (poly
  `0x07`, init `0x00`). Wired into `InvoiceClient.getByKsefNumber` and
  `SessionClient.getUpoByKsefNumber` with `String` overloads.
- **`sdk.config.CertificateSubjectIdentifier`** sealed interface
  (REQ-AUTH-033). Strategies: `Subject` (default, preserves pre-1.0
  behavior) and `Fingerprint(String)`. `KsefCertificateCredentials` and
  `KsefPkcs12Credentials` gained the field; `AuthClient` emits the
  matching `<SubjectIdentifierType>` XML element.
- **`sdk.domain.invoicing.SendInvoiceCommand`** sealed interface
  (REQ-OFFLINE-003). `Normal(byte[])` and
  `TechnicalCorrection(byte[], byte[] hashOfCorrected)` records.
  `KsefSession.send(SendInvoiceCommand)` and
  `sendTechnicalCorrection(...)` convenience method. Closes
  korekta-techniczna feature gap.
- **`sdk.config.UpoVersion`** enum + **`sdk.config.FeaturePolicy`**
  record. `KsefClient.builder().features(FeaturePolicy)` accepts the
  policy; defaults preserve pre-1.0 behavior.

### Step 3 — Public crypto facade + KOD II signing service

- **`sdk.crypto.KsefCryptoService`** public facade
  (REQ-CRYPTO-001..004). Byte and stream encrypt/decrypt
  (AES-256-CBC + PKCS#7), RSA-OAEP key wrap, ECDH path, RSA + EC key
  pair generation, file metadata (size + SHA-256). Stateless,
  thread-safe.
- **`sdk.crypto.EncryptionMaterial`** record (`AutoCloseable` —
  `close()` zeroises both key and IV). `KsefEncryptionInfo`,
  `FileMetadata`, `CsrRequest`, `CsrResult`.
- **CSR generation API** —
  `KsefCryptoService.generateCsr(CsrRequest) → CsrResult` for RSA and
  ECDSA. Auto-selects `SHA256withRSA` vs `SHA256withECDSA`. Spec
  citation: `ksef-docs/certyfikaty-KSeF.md` certificate enrollment.
- **`sdk.domain.invoicing.qrcode.QrSigningService`** (ADR-019). Auto-detects
  RSA-PSS (SHA-256/MGF1-SHA-256/saltLen=32) vs ECDSA P-256 from
  `PrivateKey` type. ECDSA DER → IEEE P1363 conversion built in.
- **`sdk.config.SigningOptions`** record with `XadesProfile` and
  `DigestAlgorithm` enums. Initial enums hold ONLY currently
  implemented values (`BASELINE_B`, `SHA256`); per ADR-021 public
  API must mean working support.

### Step 4 — Workflow fixes

- **`InvoicePackage.continuationCursor()`** (REQ-HWM-002/003). Returns
  `lastPermanentStorageDate` when truncated, `permanentStorageHwmDate`
  otherwise. Matches the spec at
  `ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md:258-264`.
- **`KsefSession.shouldUseOfflineMode(LocalDate, LocalDate)`**
  (REQ-OFFLINE-002). Calendar-day comparison helper.
- **QR label clipping fix** (Codex round-7 F2). Canvas now sized to
  `max(qrWidth, labelWidth + 2*padding)` so 35-character KSeF numbers
  (per `numer-ksef.md:3`) no longer clip.

### Step 5 — Streaming export

- **`PreparedInvoiceExport.downloadAndDecryptTo(InvoiceExportStatus, Path)`**.
  File-backed extraction for export packages too large for heap.
  ZIP-bomb caps and SHA-256 verification mirror the in-memory variant.
  ZIP path-traversal defense rejects entries that escape the output
  directory.
- **`sdk.domain.invoicing.model.ExportedInvoiceDirectory`** record
  with on-disk paths.

### Step 6 — Incremental invoice sync orchestrator

- **`sdk.domain.invoicing.sync.InvoiceSyncClient`** (REQ-HWM-001/002/003,
  REQ-EXPORT-WINDOWING-002). Implements the spec's HWM-based
  pagination algorithm: per-subject-type independent iteration,
  dedupe by KSeF number (validated via `KsefNumber.parse`),
  permanentStorageHwmDate cursor advancement, commit-after-accept
  checkpoint semantics. Acquired via `KsefClient.invoiceSync()`.
- **`CheckpointStore`** persistence-boundary interface. Static
  factory `CheckpointStore.inMemory()` for tests/short-lived processes.
- **`InvoiceSink`** functional interface, **`SyncCheckpoint`**,
  **`IncrementalSyncPlan`** (with builder), **`SyncResult`** records.

### Step 7 — Test seam policy

- `KsefClient.activateSessionForTests(...)` and `KsefClientInternals`
  were removed during 1.0 stabilisation (commit `0d1c264`). Tests now
  drive auth via the public `KsefAuthFlowFixture` WireMock harness;
  no public test seam remains in the 1.0.0 surface.

### Step 8 — Release boundary

- **`PublicApiSurfaceTest`** (Step 8 quality gate) asserts no
  `*Raw` types and no `sdk.internal.*` types leak through public
  method signatures, constructors, or fields. The previous
  `KsefClientInternals` allow-list entry is gone — that class was
  removed during 1.0 stabilisation, and the third Codex review
  fresh-pass tightened the gate so no public constructor is allowed
  to reference internal-package types either (construction now goes
  through the non-exported
  `sdk.internal.client.session.SessionHandleConstructor`).
- Version bumped from `0.1.0` to `1.0.0` across root, ksef-client,
  ksef-demo, ksef-examples poms.
- License remains AGPL-3.0-only (per revised ADR-007 — the original
  plan to switch to Apache-2.0 at v1.0 was deprecated).
- Maven Central pom metadata (SCM, developers, description, license)
  already in place from earlier releases.

### Spec audit closure

- Audit ❌ items closed: REQ-OFFLINE-003 (technical correction),
  REQ-SESS-18/19/20 (KSeF number CRC-8), REQ-AUTH-033 (cert
  fingerprint), REQ-HWM-002 (truncation cursor), REQ-EXPORT-WINDOWING-002
  (subject-type iteration via InvoiceSyncClient),
  REQ-OFFLINE-002 (auto-offline detection helper), QR-A8 (broken
  example), Codex round-7 F2 (QR label clipping).
- Audit ⚠️ items: most internal cryptographic primitives now
  exercised by `KsefCryptoServiceTest` (round-trip, stream, CSR,
  zeroisation). Remaining ⚠️ items remain documented as test debt
  in the audit; cryptographic primitives are exercised through the
  public facade tests.

### Final-stretch 1.0.0 fixes (2026-05-04 to 2026-05-05)

Workflow-correctness blockers (Codex F1-F8 across two review rounds):

- **`SendInvoiceCommand.Offline` + `KsefSession.sendOffline(byte[])`** —
  public offline-mode invoice send (offline24 / offline-niedostępność /
  awaryjny). Sets `offlineMode=true` on the wire without requiring
  technical-correction semantics.
- **`BatchSessionOptions(boolean offlineMode)`** — required parameter
  on every batch-open public method (`openBatchSession` ×2,
  `openBatchSessionFromFiles`). Static factories
  `BatchSessionOptions.online()` / `.offline()`.
- **`InvoiceClientImpl.queryAllMetadata` truncation algorithm** —
  rewritten per spec (`przyrostowe-pobieranie-faktur.md`):
  `pageOffset++` for `hasMore && !isTruncated`; reset `pageOffset` and
  advance `dateRange.from` to last record's date for `isTruncated`.
  Throws `KsefException` instead of silent partial data when the
  truncated page lacks a usable cursor.
- **`PermissionClient.queryAll*`, `TokenClient.listAll`,
  `CertificateClient.queryAll`** — abstract on the interface;
  compile-enforced full pagination contract.
- **`KsefCryptoService.parsePrivateKey(byte[])` /
  `parseCertificate(byte[])`** — public PEM/DER parsers (PKCS#8 +
  X.509). Path overloads. Legacy PKCS#1 / SEC1 / encrypted PKCS#8
  rejected with diagnostic pointing at `openssl pkcs8`.
- **`AuthorizationPolicy(ip4Addresses, ip4Ranges, ip4Masks)`** — public
  IP allow-list policy with full OpenAPI `AllowedIps` shape (max 10
  per kind, regex-validated). Surfaced via
  `KsefCredentials.authorizationPolicy()` default + 3-arg
  `KsefTokenCredentials` canonical constructor. `AuthClient`
  translates to `AllowedIpsRaw`.
- **`KsefSession.validUntil()` / `KsefBatchSession.validUntil()` +
  `timeToExpiry(Clock)`** — open-response `validUntil` retained on
  the handle; `Optional<Duration>` helper for clock-relative remaining
  budget.
- **`KsefSessionCooldownException`** — typed exception for the
  ~30-60 s post-termination cooldown observed during pre-1.0
  validation. Public `TYPICAL_COOLDOWN`, `COOLDOWN_STATUS_CODE`,
  `isCooldownStatus(int)` predicate, `suggestedRetryAfter()`.
- **`SendInvoiceBuilder` payload-size guard** — fail-fast
  `IllegalArgumentException` when invoiceXml exceeds the spec
  3 MiB max-with-attachments limit; prevents needless encrypt-and-post
  round-trips.
- **`InvoiceClient.exportInvoices(InvoiceExportBuilder)`** — removed
  `@Deprecated`, repositioned as Tier-3 advanced API per ADR-021.
  Most consumers use `prepareExport(InvoiceQueryBuilder, boolean)`.

Release-artefact polish:

- **README.md** — `FormCode.FA2` → `FormCode.FA3` in both quickstart
  samples; new "Form-code per environment" callout pointing at
  `srodowiska.md` (DEMO/PROD: FA(3) only; TEST also accepts FA(2)).
- **CHANGELOG.md:350** — `CERTIFICATES` → `CLEANUP` matching the
  actual `DemoMode` enum.
- **`InvoiceSink` Javadoc** — explicit cross-run idempotency contract:
  `accept(...)` may receive the same `KsefNumber` again across
  process restarts, overlapping HWM windows, or caller-driven retries;
  implementations must persist by `KsefNumber` idempotently.
- **`examples/IncrementalSync.java`** — new compile-checked example
  demonstrating `InvoiceSyncClient.sync(...)` workflow.
- **Maven Central artefact gate** — `maven-javadoc-plugin`
  `excludePackageNames` covers `sdk.internal.*`, `client`, `client.*`,
  `xml.*`. New `JavadocPackageGateTest` pins the documented surface
  to JPMS exports.
- **`@since 1.0.0`** on every public top-level type
  (269 files mass-updated).
- **SPDX-License-Identifier** on every `.java` (28 ksef-demo files
  back-filled with the AGPL header tag).
- **OWASP `dependency-check-maven`** plugin under new
  `security-scan` Maven profile (manual run pre-release).

Wire-shape regression coverage added:

- `ManualValidationWireShapeTest` (6 tests): `restrictToPermanentStorageHwmDate`,
  new query filters, sessions URL shape, permissions pagination,
  truncation cursor.
- `JavadocPackageGateTest` (2 tests): no internal/generated package
  leakage; documented set == JPMS exports.
- `KsefCryptoServiceTest` (4 new tests): PEM/DER parsers + legacy
  rejection.
- `AuthorizationPolicyTest` (7 tests): factories, validation matrix.
- `SendInvoiceBuilderSizeGuardTest` (2 tests): at-limit, over-limit.
- New offline-mode WireMock assertions in `KsefSessionTest` and
  `KsefClientOpenBatchSessionTest`.

## [Unreleased]

(future entries go here)

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
  named modes (`AUTH_SAFE`, `READ_ONLY`, `FULL`, `CLEANUP`).
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
- **ADR-007** — licence strategy (AGPL-3.0-only retained at 1.0.0 per
  revised decision; the original plan to switch to Apache-2.0 was
  deprecated).
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
[Unreleased]: https://github.com/mgrtomaszzurawski/ksef-java-sdk/commits/develop
