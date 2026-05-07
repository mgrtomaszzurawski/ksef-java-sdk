# ADR-018: Generated `*Raw` types removed from public bridge methods

Date: 2026-05-02 (originally deferred plan)
Amended: 2026-05-05 (closure verified for 1.0.0)
Status: Accepted — resolved historical
Last verified: 2026-05-07

## Context

ADR-005 declares that consumers must never see generated `client.model.*Raw`
types — the SDK overlays them with immutable records under
`sdk.domain.<feature>.model`. An earlier draft of the SDK left two bridge
points referencing `*Raw` in their public signatures:

1. **Builder `build()` methods** (~25 builders) returned the OpenAPI-generated
   request type (e.g. `GenerateTokenRequestRaw`).
2. **Record `from(*Raw)` static factories** (~70 records) accepted the
   OpenAPI-generated response type.

JPMS classpath consumers (the common case for Maven Central artifacts) were
unaffected — they read the unnamed module and could resolve any reference.
JPMS named-module consumers, however, could not reference these bridge
methods because the `client.model` package is not exported. Calls compiled
fine when the consumer relied on the documented public flows (passing a
builder into a domain client method, receiving a record from a domain client
method) but failed if the consumer attempted to call `build()` or `from()`
directly.

## Decision (resolved for 1.0.0)

Eliminate `*Raw` from these public signatures before the 1.0.0 tag, by:

- Introducing SDK-owned request records under
  `sdk.domain.<feature>.builder.*Request` — `build()` returns the SDK record,
  the internal client impl maps `Request → Raw` at the HTTP boundary.
- Moving `Record.from(*Raw)` bodies to internal mapper classes (or
  package-private mappers in the same domain package) — public records do
  not expose `from()` factories; mapping is invisible to consumers.

Total scope of the closure: ~30 new request records, ~70 mapper extractions,
~25 builder refactors, ~2400 LOC of mechanical change. Tests already
exercised the public API surface, so the refactor was compile-time-only on
the consumer side.

## Consequences (1.0.0)

- Builders work via the documented flow (`client.tokens().generate(builder)`).
- Records returned by domain client methods carry no public `from()` factory.
- JPMS named-module consumers see a clean public surface with no
  unreferenceable types.
- Early adopters (none on Maven Central yet) needed no migration.

## Note on Javadoc `@apiNote`

A handful of public builder Javadocs may still carry the historical
`@apiNote internal — SDK plumbing only` from before the refactor. Those notes
are now harmless: the methods return SDK-owned records, are part of the
documented public surface, and may be invoked directly by consumers.
