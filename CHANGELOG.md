# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-preview] — 2026-05-20

### About this release

**This is the first public preview of an unofficial, solo-developed Java
SDK for KSeF.** Published under groupId `io.github.mgrtomaszzurawski` to
make it clear this is a personal-namespace project, not affiliated with
Ministerstwo Finansów, CIRFMF, or any institution operating the KSeF
system.

**Status:** `EXPERIMENTAL` per the apiguardian `@API` annotation on
`KsefClient`. The API surface MAY break between 0.x releases as
real-world feedback accumulates. The eventual 1.0.0 cut will lock the
contract.

**Intended use:** read-only flows (queries, exports, incremental sync)
by the author for own consumption + adventurous early adopters who
accept breaking changes. **Not recommended for production write flows**
(invoice submission, batch upload, permission grants) until 1.0.0 lands
and the test coverage gaps below close.

**Support model:** best-effort GitHub issues. No SLA. No commercial
support. AGPL-3.0 §15–16 warranty disclaimer applies.

For production invoice flows, prefer the official SDK at
[CIRFMF/ksef-client-java](https://github.com/CIRFMF/ksef-client-java).

### Highlights since rewrite start

This is the cumulative state of the SDK as of 2026-05-20 after three
post-redesign feedback rounds (R1 → R2 → R3) on top of the 22-PR
architectural redesign that landed in late April.

- **Single facade entry point** — `KsefClient` exposes 9 typed accessors
  (`.invoices()`, `.permissions()`, `.tokens()`, `.certificates()`,
  `.peppol()`, `.limits()`, `.testData()`, `.authSessions()`,
  `.qrCode()`); plus a diagnostic `.config()` snapshot for multi-tenant
  orchestration (R1-16).
- **Workflow vs Endpoint Tier 1/Tier 2 split** (ADR-021): high-level
  workflow APIs (`session.send`, `batch().submit`, `invoices().sync()`)
  hide protocol details (challenge-response auth, AES-256-CBC session
  encryption, XAdES-BASELINE-B signing + SHA-256, retry-with-backoff,
  batch upload + polling); endpoint APIs (`client.invoices().queryInvoicesByMetadata`,
  etc.) for custom orchestration.
- **Sealed type hierarchies** — `KsefCredentials` (3 permits: token /
  certificate / PKCS#12), `PermissionGrantRequest` (7 permits, R2-11),
  `Session` (permits `OnlineSession` + `ClosedSession`, R3 item #10).
- **Custom invoice types registry** — `KsefInvoiceTypes` extension
  point for read-side typed wrappers beyond FA2/FA3/PEF/PEF_KOR
  (R2-6).
- **Stream paginators** — every read endpoint returning collections
  exposes a `streamX(...)` variant that lazy-walks via the SDK's
  internal cursor handler (R2-9, ADR-023).
- **Typed-exception hierarchy** — 13 exception classes grouped by
  consumer remediation action (input bad / refresh creds / wait rate
  limit / server problem / etc.), not by HTTP layer. `KsefException.of`
  dispatches HTTP status codes to typed subclasses with parsed error
  bodies (R3 item #8).
- **Package layout per ADR-034** — `domain/<feature>/` functional
  buckets with sub-bucketing where one bucket exceeds peers (invoicing
  sub-split into `session/`, `document/`, `offline/`, `archive/`,
  `builder/`, `model/`, `qrcode/`, `sync/`). Sentinel packages
  (`config/`, `core/`, `exception/`) sub-split when themes diverge
  (`config.credentials`, `config.policy`).
- **Internal/runtime split** (ADR-012, ADR-013) — `internal.client.*`
  endpoint wrappers, `internal.runtime.*` cross-cutting plumbing
  (transport, crypto, validation, async, batch, signing, jaxb,
  pagination); none of `internal.*` is exported via JPMS.
- **11 JBang example programs** demonstrating each `KsefClient`
  accessor (R2-13) plus 4 examples for legit settable knobs
  (FeaturePolicy, KsefInvoiceTypes registry, AuthorizationPolicy IP
  allow-list, multi-tenant orchestration via `client.config()`)
  (R3 item #9).
- **Live demo runner** (`./gradlew :ksef-demo:run -Pdemo.mode=FULL`)
  against KSeF DEMO and TEST environments, 75 + 22 = 97 wire flows
  verified with zero failures post-R3.

### Known limitations

The following gaps are tracked and will close before the 1.0.0 cut:

- **Negative-path test coverage** is partial. The XSD validation gate
  on `OnlineSession.sendInvoice` and the batch preflight on
  `submitFromFiles` are pinned with regression tests, and the typed
  HTTP-status → exception dispatch is pinned, but wider coverage is
  pending:
  - **G-2:** XSD violation variety (missing required fields, wrong
    data types, wrong enums) — only "wrong root" + "structurally
    invalid" cases covered today.
  - **G-3:** JAXB unmarshalling negative paths (malformed server
    response XML, missing required fields in server payloads) —
    relies on assumption that KSeF returns well-formed XML.
  - **G-5:** Test fixtures for invalid invoices are inline string
    literals; refactor to `src/test/resources/invalid-invoices/`
    pending.
- **Quality Gate `new_coverage`** drift on Sonar from the R3 refactor
  surface (file moves counted as "new code"). Legacy tests still cover
  the same logic at new paths; ratchet up to 80% post-stabilisation.
- **README quickstart snippet** uses pre-R3 import paths
  (`domain.invoicing.KsefSession`, `client.openSession(...)`) — the
  actual current API is `client.invoices().sessions().online(FormCode)`.
  Consumers should follow the `examples/` directory until README is
  modernised.

### Coordinates

```xml
<dependency>
    <groupId>io.github.mgrtomaszzurawski</groupId>
    <artifactId>ksef-client</artifactId>
    <version>0.1.0-preview</version>
</dependency>
```

Companion artifacts (transitive, no explicit declaration needed):

- `io.github.mgrtomaszzurawski:ksef-xml-models:0.1.0-preview`
- `io.github.mgrtomaszzurawski:ksef-rest-models:0.1.0-preview`

---

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
