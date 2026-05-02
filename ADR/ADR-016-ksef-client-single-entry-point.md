# ADR-016: `KsefClient` is the only entry point — interface + private-impl for domain clients

**Date:** 2026-05-02
**Status:** Accepted
**Last verified:** 2026-05-02
**Trigger:** USER-REVIEW-REMARKS findings #3 and #5. Three independent
leaks let consumers bypass the `KsefClient` facade:

1. `client.auth()`, `client.sessions()`, `client.security()` accessors
   exposed `internal/client/<area>/` clients (#3 — fork (a)).
2. All 11 `*Client` classes had `public XxxClient(KsefClient ksef)`
   constructors — `new PermissionClient(ksef)` worked from anywhere
   (#5 — Pattern B leaky implementation).
3. Six `KsefClient` methods (`httpClient()`, `objectMapper()`, ...)
   are language-public because `KsefClient implements HttpRuntime`
   per ADR-013, leaking the transport contract to consumers (#5
   sub-finding 3 — deferred to a follow-up PR).

This ADR addresses (1) and (2). Sub-finding (3) is tracked as Phase 9
follow-up.

## Context

The SDK was designed (per ADR-008, ADR-012) so consumers reach
functionality only through `KsefClient`. Reality drifted: tests and
demo runners worked against `client.auth().listSessions()` style code,
domain `*Client` constructors were public, and the facade lost its
single-entry-point property.

Pattern B (namespaced accessors, AWS / Stripe / Azure SDK style) is
the right shape — `client.permissions().grant(...)` is industry
standard. The fix is *not* to abandon Pattern B for Pattern A (god
class with all 60+ ops directly on `KsefClient`); the fix is to lock
down Pattern B properly so the only path from consumer to a `*Client`
is through the facade accessor.

## Decision

### Part 1 — Domain `*Client` interface + private impl

Each of the 8 domain clients splits into:

- `sdk/domain/<feature>/<XxxClient>.java` — public **interface**,
  exported via JPMS. Contains only method signatures; the consumer
  programs against this contract.
- `sdk/internal/client/<feature>/<XxxClient>Impl.java` — concrete
  implementation, package **not exported via JPMS**. Class is `public`
  (Java requires it for cross-package instantiation by `KsefClient`),
  constructor is `public` (same reason). The encapsulation is
  enforced by JPMS package non-export, not by Java visibility.

Affected clients:
`PermissionClient`, `TokenClient`, `CertificateClient`,
`LimitsClient`, `RateLimitClient`, `PeppolClient`, `InvoiceClient`,
`TestDataClient`.

`KsefClient` accessors (`permissions()`, `tokens()`, etc.) return the
**interface type**. The instance is a `*ClientImpl` constructed in
`KsefClient`'s constructor. Consumer experience:

- `client.permissions().grant(...)` — works (interface exported,
  method on interface).
- `new PermissionClient(...)` — fails (interface, can't instantiate).
- `new PermissionClientImpl(...)` — fails for named-module consumers
  (package not exported, type unreachable).

### Part 2 — `KsefClient.auth()`, `sessions()`, `security()` removed

`AuthClient`, `SessionClient`, `SecurityClient` stay in
`internal/client/<area>/`. They remain concrete classes (not split
into interface + impl) because no consumer-facing accessor returns
them. Their public methods are reachable from inside the SDK module
only.

Replacement public API on `KsefClient`:

- `authenticate()` — synchronous, polls challenge → init → status →
  redeem internally; throws on failure.
- `reauthenticate()` — clears state and re-runs the full flow; used
  internally on HTTP 401, available publicly for explicit refresh.
- `terminateAuth()` — explicit logout.
- `openSession(FormCode)` — opens an online session.
- `openBatchSession(...)` — opens a batch session (two overloads).

Operations that had no high-level public equivalent and were used
only by the demo's diagnostic probes (`listActiveSessions()`,
`getAuthenticationStatus(ref)`, `terminateSession(ref)`,
`refreshToken(refreshToken)` — direct refresh given a known refresh
token):

- **Not added to `KsefClient`.** They are diagnostic operations,
  not idiomatic consumer flows.
- Demo probes that used them now construct the internal client
  directly (`new AuthClient(client)`) — works because `ksef-demo` is
  an unnamed module and can read non-exported packages of its
  dependencies. This is *not* the recommended consumer pattern; demo
  is the SDK author's diagnostic harness, not a consumer template.
  Consumers writing against the public API will not have this access.

### Part 3 — Module-info cleanup

The `sdk.internal.client.auth.model` JPMS export (added in PR #29
with a TODO marker) is now removed. Auth response records
(`AuthenticationChallenge`, `AuthenticationStatus`, ...) are no
longer reachable from named-module consumers, and no high-level
`KsefClient` method returns them. Tests and demo continue to
reference them — both run against the SDK's own JAR within the
build, where JPMS encapsulation does not block reads.

## Why not Option A (documented-public)

Considered: keep `*Client` constructors public with
`@apiNote internal` Javadoc. Rejected — Javadoc is not enforcement.
Two paths to instantiate (`new PermissionClient(...)` vs
`client.permissions()`) means consumers will pick one, and one of
those paths is the wrong one. Documentation cannot prevent that.

## Why not Option B (access-key token pattern)

Considered: every `*Client` constructor takes a `KsefClient.AccessKey`
nested type with a private constructor. Rejected — adds a
nonsensical first parameter to every `*Client` signature, complicates
Javadoc, and is the kind of pattern a future reader will assume is
load-bearing security when it is just a visibility hack.

## Why not Option C (move + interface + factory + token combination)

Considered as a maximally-strict variant. Rejected — too heavy. The
chosen design (interface + impl, JPMS-enforced) gives identical
practical guarantees with one fewer layer of indirection.

## Consequences

### Positive

- `KsefClient` is now the only consumer entry point. Every domain
  operation is reached via `client.<feature>().<op>(...)`.
- 8 domain `*Client` types are interfaces — clean Javadoc surface,
  IDE-discoverable, future-proof for alternative implementations
  (e.g., async, mock).
- Implementation classes hidden from named-module consumers via
  JPMS — `new XxxClientImpl(...)` won't compile in consumer code.
- Three internal client accessors removed — no diagnostic-flavoured
  code paths in the public API.

### Negative / trade-offs

- Implementation classes are `public final` (Java requires this for
  cross-package instantiation by `KsefClient`). The encapsulation
  is JPMS-only; the SDK's own tests (in unnamed test module) can
  still see them. This is the intended trade-off — same access
  consumers' tests would have if they shared classpath.
- Demo runners that needed diagnostic operations now construct
  internal clients directly. Documented: demo is the SDK author's
  harness, not a consumer template.
- One interface per implementation doubles the type count for the 8
  domain clients (16 types instead of 8). Each interface is small
  (just method signatures), and the doubling is a one-time cost
  paid at this refactor.

### Deferred

- `KsefClient implements HttpRuntime` (ADR-013) leaks 6 transport-
  layer methods to the public API surface (`httpClient()`,
  `objectMapper()`, `retryHandler()`, `sessionContext()`,
  `readTimeout()`, `environment()`). Hiding these requires moving
  to a private inner `RuntimeAdapter` class — separate refactor
  tracked under finding #5 sub-3.
- Per-method Javadoc on the new interfaces — the bulk-split script
  preserved class-level Javadoc but lost per-method Javadoc.
  Tracked under PLAN A.2 (Javadoc audit).

## References

- ADR-008 — KsefSession / KsefBatchSession session abstractions
- ADR-012 — package structure (`domain/<feature>/`,
  `internal/client/<area>/`, `internal/runtime/`)
- ADR-013 — `HttpRuntime` interface (related: deferred sub-3)
- USER-REVIEW-REMARKS-2026-05-01-2130.md findings #3 and #5
- AWS SDK / Stripe SDK / Azure SDK — Pattern B reference
  implementations
