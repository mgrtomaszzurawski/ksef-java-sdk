# ADR-018: Generated `*Raw` types on internal bridge methods (deferred to 0.2.0)

Date: 2026-05-02
Status: Accepted (with deferred follow-up)

## Context

ADR-005 declares that consumers must never see generated `client.model.*Raw`
types — the SDK overlays them with immutable records under
`sdk.domain.<feature>.model`. The 0.1.0 release of `ksef-client` ships with
this contract intact for the common consumer surface (domain client
interfaces, builder fluent APIs, returned record types). However, two
specific bridge points still reference `*Raw` in their signatures:

1. **Builder `build()` methods** (~25 builders) return the OpenAPI-generated
   request type (e.g. `GenerateTokenRequestRaw`). The SDK invokes
   `builder.build()` internally; consumers don't normally need to call it.
2. **Record `from(*Raw)` static factories** (~70 records) accept the
   OpenAPI-generated response type. The SDK invokes them when mapping
   responses; consumers don't normally need to call them.

JPMS classpath consumers (the common case for Maven Central artifacts) are
unaffected — they read the unnamed module and can resolve any reference. JPMS
named-module consumers, however, cannot reference these bridge methods
because the `client.model` package is not exported. Calls compile fine when
the consumer relies on the documented public flows (passing a builder into a
domain client method, receiving a record from a domain client method) but
fail if the consumer attempts to call `build()` or `from()` directly.

## Decision

For 0.1.0, ship with the bridge methods annotated `@apiNote internal — SDK
plumbing only` and document the limitation in CHANGELOG. Both methods stay
public because SDK domain client implementations live in non-exported
`internal.client.<feature>` packages and need cross-package access; making
the bridge methods package-private would break the SDK itself.

For 0.2.0, eliminate `*Raw` from these signatures by:

- Introducing SDK-owned request records under
  `sdk.domain.<feature>.builder.*Request` — `build()` returns the SDK record,
  the internal client impl maps `Request → Raw` at the HTTP boundary.
- Moving `Record.from(*Raw)` bodies to internal mapper classes under
  `sdk.internal.client.<feature>.mapping.*Mapper` — public records lose the
  `from()` factory; mapping is invisible to consumers.

Estimated scope for 0.2.0: ~30 new request records, ~70 mapper extractions,
~25 builder refactors, ~2400 LOC of mechanical change. Tests already exercise
the public API surface, so the refactor is compile-time-only on the consumer
side.

## Consequences

**0.1.0 ships with**:
- Builders that work via the documented flow (`client.tokens().generate(builder)`).
- Records returned by domain client methods (no consumer call to `from()`).
- A documented limitation: direct invocation of `Builder.build()` /
  `Record.from(*Raw)` from a JPMS named-module consumer is unsupported.

**0.2.0 will introduce a breaking change** to anyone who actually invokes
these bridge methods. The Javadoc note + CHANGELOG entry warn early adopters
that the bridge methods are not part of the supported API.
