# ADR-020: `ksef-client-testkit` philosophy — fixtures, not backdoors

Date: 2026-05-03
Status: Accepted — closure verified 2026-05-05 for 1.0.0 (KsefClientInternals removed; no testkit backdoor module needed because no public-static seams remain; the deferred 0.2.x extraction is moot)

## Context

The 0.1.x line of the SDK shipped two test-only seams in the **main**
`ksef-client` artifact:

- `KsefClient.activateSessionForTests(token, ref, refreshToken)` — stuffs
  a fake session into the client's `SessionContext` so tests can skip the
  full challenge-response auth flow.
- `KsefClientInternals` — a deprecated public static class exposing
  `runtime(KsefClient)` and `sessionContext(KsefClient)` so SDK-internal
  tests can construct internal clients directly.

Both are marked `@Deprecated(since = "0.1.0", forRemoval = true)`. They
exist because the SDK's own tests historically reached into private state
to set up scenarios.

For 1.0.0 these seams must not be part of the released main artifact.
Consumers must not see them, even as deprecated.

The naive plan is "move both into a new `ksef-client-testkit` Maven
module." That solution is incomplete. JPMS named-module access rules make
"package-private hooks reachable from a sibling module" non-trivial, and
moving the seam to another jar still leaves a public backdoor open — just
in a different artifact.

## Decision

`ksef-client-testkit` is a fixture module, **not** a backdoor module. The
test seams are removed by changing the tests, not by relocating the
seams.

Sequencing is strict:

1. **Rewrite each SDK-internal test** that currently uses
   `activateSessionForTests(...)` or `KsefClientInternals.*`. Each test
   migrates to one of four target shapes:
   - **Type A — full WireMock auth flow.** Test exercises the public API
     path. Rewrite to drive `/auth/challenge` →
     `/auth/xades-signature` (or `/auth/ksef-token`) → `/auth/{ref}` →
     `/auth/token/redeem` end-to-end against WireMock stubs.
   - **Type B — direct internal runtime construction.** Test is unit-testing
     an internal client. Use the package-private `HttpRuntime`
     constructor or a test-only factory. This stays inside the SDK module,
     where `internal/runtime/transport/` is reachable.
   - **Type C — pure mapper/unit test.** Test is testing pure transformation
     logic. Drop WireMock; instantiate the function under test directly.
   - **Type D — testing private state too directly.** Test is asserting
     things public API does not promise. Rewrite to test observable
     behavior or delete the test.
2. **Once SDK's own tests no longer reference the seams,** remove both
   `KsefClient.activateSessionForTests(...)` and `KsefClientInternals` from
   the main artifact.
3. **Optionally publish `ksef-client-testkit`** as a new Maven module
   containing **consumer-facing fixtures**:
   - Pre-built WireMock stubs for common scenarios (authenticated session
     ready, batch upload happy path, export ready, rate-limited response,
     terminal failure).
   - Deterministic crypto fixtures (known AES key, known certificate, known
     SHA-256 inputs).
   - Helpers for spinning up a fake KSeF endpoint locally.

The testkit's purpose is to make consumer tests easy to write, not to
expose private SDK state.

## Consequences

- The main `ksef-client` 1.0.0 artifact has zero public test seams. Public
  API surface tests assert no `sdk.internal.*` types reachable from public
  signatures.
- SDK-internal tests live within `ksef-client/src/test/`. They can reach
  internal types via Java package-private access because they're in the
  same module. They do not need a public seam.
- If `ksef-client-testkit` ships, its public types are **fixtures** —
  WireMock stub builders, deterministic credential builders, fake-server
  builders. None of them mutate `KsefClient` private state. If a fixture
  feels like it needs to mutate a `KsefClient`, the test design is wrong
  and goes back to the four-type classification above.
- The 0.1.x deprecated public seams are removed in the same step that
  publishes 1.0.0; they never reach a stable consumer.

## Rejected alternatives

1. **Move `KsefClientInternals` to `ksef-client-testkit` unchanged.**
   Rejected because that's a renamed backdoor, not a removed backdoor.
2. **Keep `KsefClientInternals` deprecated in 1.0.x.** Rejected because the
   1.0 release is the moment to clean the surface. Carrying a
   `forRemoval = true` accessor through a stable line means every consumer
   sees a public class telling them "this is going away" for the entire
   1.0 lifetime.
3. **Use reflection in the testkit to reach private state.** Rejected
   because reflection-based fixtures break under JPMS strong encapsulation
   and create silent runtime failures.

## Test classification — when to use which type

| Test smell | Likely type | Migration |
|---|---|---|
| Mocks `/auth/*` URLs and asserts full auth round-trip | A | already correct shape; might just need to remove `activateSessionForTests` setup |
| Constructs `AuthClient`/`SessionClient`/etc directly with a runtime | B | `HttpRuntime` package-private ctor |
| Asserts return value of a `from(*Raw)` factory | C | drop runtime entirely |
| Asserts internal `SessionContext` field values | D | rewrite to test observable behavior or delete |

## Spec citations

- `ksef-docs/uwierzytelnianie.md` — auth flow (drives Type A migrations)

## Related

- ADR-005 — SDK overlay on generated code
- ADR-012 — package structure (`sdk.internal.*` not exported)
- ADR-013 — `HttpRuntime` narrow interface
