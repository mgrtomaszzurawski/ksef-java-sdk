# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

This is the first public release. The SDK has been developed iteratively against
the live KSeF demo environment; ADRs in `ADR/` document each architectural
decision in chronological order.

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
- 264 unit + integration tests across 29 test classes (WireMock-mocked HTTP).
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
