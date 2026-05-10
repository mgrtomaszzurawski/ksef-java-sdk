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
- `OnlineSession` (online) try-with-resources abstraction; `close()` polls
  until terminal and surfaces failures as typed
  `KsefSessionTerminalFailureException` / `KsefSessionPollingTimeoutException`.
- `Invoices.submitBatch(FormCode, List<Invoice>, BatchOptions)` — synchronous
  batch facade replacing the prior 5-step open / upload / close / poll / fetch
  state machine. SDK encrypts every invoice with a session AES key, splits
  into parts, opens the batch session, uploads parts in parallel, closes,
  polls until terminal, and downloads UPOs for accepted invoices. Returns a
  populated `BatchResult` with `cleared`/`failed` breakdown. `submitBatchFromFiles(...)`
  is the file-streaming variant for large batches. (PR11)
- **Threading warning:** `submitBatch` blocks the calling thread for minutes
  to hours, depending on batch size and upload bandwidth. KSeF batch can be
  up to 5 GB. Do not call from UI threads, HTTP request handlers, or
  reactive framework dispatch threads. Wrap with a dedicated executor for
  async use. (PR11)
- **No progress listener** on `BatchOptions` — per ADR-008/D1, callback-style
  progress events invert control of the consumer's thread context. Callers
  needing UI progress wrap `submitBatch(...)` in
  `CompletableFuture.supplyAsync(...)`. (PR11)
- `SendInvoiceCommand` sealed interface — `Normal(byte[])` and
  `TechnicalCorrection(byte[], byte[] hashOfCorrected)` (per
  `ksef-docs/offline/korekta-techniczna.md`).
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
- `InvoiceClient.getByKsefNumber(KsefNumber)` returns a typed
  `InvoiceDocument` (`Fa2InvoiceDocument` / `Fa3InvoiceDocument` /
  `PefInvoiceDocument` / `PefKorInvoiceDocument`) with JAXB tree
  exposed via the schema-specific escape-hatch accessor. Unknown
  schemas fall through to `InvoiceDocument.fromXml(...)` minimal
  wrapper for forward compatibility. (PR14)
- `FormCodeDetector` infers the `FormCode` from the XML root element
  + namespace; covers FA(2)/FA(3)/PEF(3)/PEF_KOR(3) and reports unknown
  for forward-compat. (PR14)
- `InvoiceSyncClient` — HWM-based incremental sync with `CheckpointStore`
  + `InvoiceSink`; mandates `PERMANENT_STORAGE` axis per spec.
- `InvoiceClient.syncAsStream(plan, store)` — Stream-based incremental
  sync. Each consumed element returns a `DecryptedInvoice` (KsefNumber +
  metadata + decrypted XML bytes + optional file path). Caller composes
  with standard Stream operators (`filter`, `limit`, `takeWhile`) and
  consumes via try-with-resources for deterministic cleanup. (PR17)

#### Typed Invoice impls (PR12b)
- `Fa2Invoice`, `Fa3Invoice`, `PefInvoice`, `PefKorInvoice` — typed
  wrappers over the JAXB raw tree with fluent data-authoring builders
  for the common business cases (header, seller/buyer, line items,
  currency, totals, correction reference). Builders validate XSD-equivalent
  required-field rules at `.build()` time and throw informative
  `IllegalStateException` on missing fields.
- `Invoice.fromXml(FormCode, byte[])` minimal escape-hatch factory
  remains the path for schemas the SDK does not yet model.

#### Offline + technical correction (PR13)
- `OfflineInvoice` immutable type wrapping an authored `Invoice` with
  pre-rendered KOD I + KOD II QR PNGs.
- `OfflineInvoiceBuilder` fluent builder; `OfflineMode` enum captures
  the legal basis (`OFFLINE_24`, `KSEF_UNAVAILABILITY`, `KSEF_EMERGENCY`).
- `OnlineSession.sendOfflineInvoice(OfflineInvoice)` and
  `sendTechnicalCorrection(Invoice, byte[] hashOfOriginal)`.
- `KsefUnavailableException` thrown on HTTP 503 and on transport-level
  unreachability (ConnectException, UnknownHostException) — caller
  branches into offline-mode flow.

#### UPO retrieval (PR15)
- `ClearedInvoice` record completes the `Invoice → SubmittedInvoice →
  ClearedInvoice` lifecycle. Embeds the full SubmittedInvoice + UpoEntry
  (raw XAdES bytes + parsed summary).
- `UpoSummary.parse(byte[])` static factory — hardened SAX +
  JAXB unmarshal of the Potwierdzenie tree with bit-exact `rawXml`
  preserved for archive use.
- `ClosedSession.cleared(SubmittedInvoice)` and
  `ClosedSession.cleared(String referenceNumber)` overloads;
  `ClosedSession.allCleared()` returns the typed list.
- Legacy `OnlineSession.upo(...)` / `upoByKsefNumber(...)` /
  `bulkUpos()` accessors removed — UPO retrieval is post-archive only
  per the type-state model.

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
- `KsefClient.qrCode()` accessor returning the shared `QrCodeService`
  for KOD I / KOD II rendering. (PR16)
- `KsefIdentifier` per-type format validation: `internalId` matches
  `<10-digit NIP>-<5 digits>`, `nipVatUe` matches country-prefix +
  alphanumeric, `peppolId` matches `<ICD>:<value>`. NIP factory now
  validates the weighted-sum checksum (weights 6,5,7,2,3,4,5,6,7
  modulo 11). (PR19)
- `KsefTimeoutException` abstract parent of `KsefAsyncTimeoutException`
  and `KsefSessionPollingTimeoutException` — single catch covers both. (PR19)
- `KsefException.safeResponseBody()` scrubs digit runs ≥ 9 (NIP, PESEL,
  JWT) preserving last 4 chars; suitable for log/audit output. Raw
  `responseBody()` still public for SDK-internal error parsing. (PR19)
- `KsefEnvironment.custom(String)` validates via `URI.parseServerAuthority()`
  — rejects malformed URLs and missing-host configurations at config
  time instead of letting them surface as obscure HTTP failures later. (PR19)
- HTTP 410 Gone mapped to `KsefRetentionExpiredException` for KSeF
  retention-expired responses (api-changelog v2.4.0).
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

- **Public batch session surface** (PR11): `KsefBatchSession`,
  `PreparedBatchPackage`, `BatchFileSpec`, `BatchSessionOptions`,
  `BatchAssemblyMode`, and the three `KsefClient.openBatchSession*`
  overloads were removed from the public API. The 5-step state machine
  they exposed (open → uploadParts → close → pollUntilComplete → bulkUpos)
  was consolidated into the synchronous `Invoices.submitBatch(...)` facade.
  These types now live in `sdk.internal.runtime.batch` /
  `sdk.internal.client.session` and are not exported via JPMS.

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
