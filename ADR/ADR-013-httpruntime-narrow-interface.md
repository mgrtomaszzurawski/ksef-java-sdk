## ADR-013: `HttpRuntime` narrow interface — break the transport→facade layering inversion

**Date:** 2026-05-01
**Status:** Accepted
**Last verified:** 2026-05-07
**Trigger:** PR #27 SonarQube/IDE inspection wave. `HttpSupport`
(`internal/runtime/transport/`) imported `KsefClient` (the top-level facade)
to read its base URL, HTTP client, retry handler, session context, object
mapper, read timeout, and to call `reauthenticate()` on a 401. The transport
layer reaching up to the facade was a textbook layering inversion and
prevented `HttpSupport` from being unit-testable without a fully-built
`KsefClient`.

## Context

Before this change, `HttpSupport` had:

```java
public final class HttpSupport {
    private final KsefClient ksef;        // <-- reached up to the facade
    public HttpSupport(KsefClient ksef) { this.ksef = ksef; }
    // ... uses ksef.baseUrl(), ksef.httpClient(), ksef.retryHandler(),
    //     ksef.sessionContext(), ksef.objectMapper(), ksef.readTimeout(),
    //     ksef.reauthenticate()
}
```

Problems this caused:

1. **Layering inversion.** `internal/runtime/transport/` is the lowest hand-
   written layer; `KsefClient` is the highest. Bottom importing top is the
   exact pattern static analyzers flag (java:S6818, similar SpotBugs/PMD
   smells). The compile-time graph was a strict cycle waiting to happen the
   moment `HttpSupport` was used from anywhere `KsefClient` couldn't be
   imported.
2. **Test plumbing.** A `HttpSupport` unit test had to either build a real
   `KsefClient` (requires environment + credentials + builder chain) or
   mock the entire facade — both heavy.
3. **Method-set sprawl on `KsefClient`.** Every accessor `HttpSupport`
   needed had to live as a public method on `KsefClient`, polluting its
   public surface with seven plumbing methods that consumers should never
   call (`baseUrl()`, `httpClient()`, `retryHandler()`, etc.).
4. **Pre-Phase-9 the situation was tolerable** because `KsefClient` and
   `HttpSupport` were both at the `sdk/` package root. After ADR-012 split
   them across `sdk/` and `sdk/internal/runtime/transport/`, the import
   crossed a JPMS encapsulation boundary in a direction the boundary was
   meant to prevent.

## Decision

Introduce a narrow `HttpRuntime` interface in
`sdk/internal/runtime/transport/` declaring only what `HttpSupport`
actually needs:

```java
public interface HttpRuntime {
    String        baseUrl();
    HttpClient    httpClient();
    SessionContext sessionContext();
    RetryHandler   retryHandler();
    ObjectMapper   objectMapper();
    Duration       readTimeout();
    void           reauthenticate();
}
```

`KsefClient` `implements HttpRuntime`. `HttpSupport` is rewritten to depend
on `HttpRuntime`:

```java
public final class HttpSupport {
    private final HttpRuntime runtime;
    public HttpSupport(HttpRuntime runtime) { this.runtime = runtime; }
}
```

All 11 call sites that build `new HttpSupport(this)` from inside
`KsefClient` keep working unchanged — `this` now satisfies `HttpRuntime`.

### Why an interface, not a parameter struct / record

- **Lazy state.** `sessionContext()` returns a *live* reference; its JWT
  rotates over the lifetime of a `KsefClient`. A snapshot record would
  freeze the JWT at construction time.
- **Lazy methods.** `reauthenticate()` is a *behavior*, not data. It
  mutates `KsefClient` (lazily re-runs the auth flow, updates
  `SessionContext`). No record can carry that.
- **Narrowness.** Seven methods is the actual minimum for `HttpSupport`.
  Anything more, and the abstraction has leaked. Anything less, and
  `HttpSupport` would need a back-channel.

### Why keep `HttpSupport` outside `KsefClient`

It is ~430 lines of HTTP plumbing — request building, header injection,
401 retry-with-reauth, response classification into the typed exception
hierarchy. Keeping it as a separate file under `internal/runtime/transport/`
keeps `KsefClient` (the facade) at a manageable size and lets
`HttpSupport` evolve without touching the public API.

### JPMS

`HttpRuntime` lives in `sdk/internal/runtime/transport/`, which is **not**
exported. Consumers cannot reference the interface, so they cannot supply
a fake `HttpRuntime` to swap in their own transport. That is intentional —
the contract is internal to the SDK. The test side uses the real
`KsefClient` against WireMock; no production code needs an alternate
`HttpRuntime` impl.

## Consequences

### Positive

- Compile-time dependency: `internal/runtime/transport/` no longer imports
  the `sdk/` package root. The cycle is broken structurally, not just by
  convention.
- `HttpSupport` is now unit-testable with a hand-rolled `HttpRuntime`
  fake (used internally for diagnostic tests).
- `KsefClient` keeps the 7 accessor methods, but their contract is now
  pinned by the `HttpRuntime` interface — a future refactor that wants to
  remove `httpClient()` from the facade can do so by creating a separate
  `HttpRuntime` implementation, no longer constrained to "whatever
  `KsefClient` exposes."
- SonarQube java:S6818 (and the equivalent IDE inspection) clean.

### Negative / trade-offs

- One extra type (`HttpRuntime` interface) for what is currently a
  single-implementation contract. Acceptable: the interface is 7 lines of
  signature, and the cost of the interface is far smaller than the cost
  of the inversion it removes.
- Anyone reading `KsefClient` now has to follow the `implements
  HttpRuntime` link to see why those 7 accessor methods are public. The
  Javadoc on each accessor and on the interface explains the role.
- The `HttpRuntime` accessors on `KsefClient` are *language-public* (they
  must be, to satisfy the interface) but conceptually internal. They
  carry an `@apiNote internal — not part of public SDK contract` Javadoc
  and JPMS keeps `HttpRuntime` itself unreachable from consumers.

## Alternatives considered

- **Move `HttpSupport` into `KsefClient` as a private inner class.**
  Rejected: ~430 lines of HTTP plumbing inside the facade kills
  readability and makes `KsefClient` impossible to navigate.
- **Pass primitives + lambdas to `HttpSupport` constructor.** Rejected:
  7 parameters in a constructor is an immediate "introduce parameter
  object" smell; a parameter object that also holds a `Runnable` for
  reauth and a live `SessionContext` reference is a poor man's
  interface — without the type-checking benefits.
- **Make `HttpSupport` extend `KsefClient`.** Rejected: nonsense
  inheritance, would still couple to the facade.

## References

- PR #27 — HttpRuntime interface introduction + the layering-inversion fix
- ADR-012 — package structure split that made the inversion visible
- ADR-005 — SDK overlay rule (consumers never reach `internal.*`)
- SonarQube rule java:S6818 (avoid layering inversion via type imports)
- `sdk/internal/runtime/transport/HttpRuntime.java` — the interface
- `sdk/KsefClient.java` — `implements HttpRuntime`
- `sdk/internal/runtime/transport/HttpSupport.java` — the consumer
