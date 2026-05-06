# ADR-024: Cross-package construction via non-exported reflective bridge

Date: 2026-05-04
Status: Accepted

## Context

Public session-handle types (`KsefSession`, `KsefBatchSession`,
`PreparedInvoiceExport`) live in the exported package
`sdk.domain.invoicing` so consumers can:

- import them by name,
- reference them as method return types,
- use them with `try-with-resources`.

But these handles are *constructed* with internal-typed parameters —
`SessionClient`, `BatchPackageBuilder.BatchPackage`, `HttpRuntime` —
that consumers must NOT see. Three approaches are technically possible:

1. **Public constructors with internal-typed parameters.** Rejected
   immediately: leaks `client.model.*Raw` and `internal.*` types in
   the binary surface and Javadoc. ADR-005 forbids this.
2. **Public factory class in an exported package** (`KsefSessionFactory`
   under `sdk.domain.invoicing`). Initial attempt during pre-1.0
   refactor. Rejected: the factory's *public methods* still take
   internal types — the leak just moved to a different public class.
3. **Package-private constructors + a non-exported bridge.** Adopted.

## Decision

The handle constructors are package-private (visible to
`sdk.domain.invoicing` callers only). Cross-package callers go through
`SessionHandleConstructor` which lives in
`sdk.internal.client.session` — a package NOT exported by
`module-info.java`. The bridge calls each handle's package-private
constructor via reflection (`Constructor.newInstance(...)` with
`setAccessible(true)`).

```java
// in sdk.internal.client.session.SessionHandleConstructor
public static KsefSession newOnlineSession(SessionClient client,
                                            String referenceNumber,
                                            byte[] aesKey,
                                            byte[] iv) {
    return ONLINE_SESSION_CTOR.newInstance(client, referenceNumber, aesKey, iv);
}
```

`Constructor` instances are looked up once at class-load time and
cached as `private static final` fields, so the per-call cost is
roughly equivalent to a direct invocation.

`PublicApiSurfaceTest` enforces the invariant: scans `sdk.*` for any
public constructor whose parameter types include `client.model.*Raw`,
`internal.*`, or any non-exported type. Zero exemptions allowed.

PMD's `AvoidAccessibilityAlteration` rule is suppressed on the single
`makeAccessible` helper in `SessionHandleConstructor` with an
explanatory `@SuppressWarnings` Javadoc citing this ADR.

## Alternatives considered

1. **JPMS `opens io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing
   to io.github.mgrtomaszzurawski.ksef`** in `module-info.java`. Same
   module so this is a no-op — JPMS only enforces module-boundary
   opens, not package-boundary access within a module. The reflection
   already works without `opens`.
2. **Make handles `final` with public constructors taking only public
   types**, then have the SDK wire internal state through setters.
   Rejected: invites partial-construction bugs (caller forgets a
   setter), defeats `final` immutability of the handle's state.
3. **Replace handle types with sealed interfaces** + internal record
   implementations. Rejected: consumers want to use `KsefSession` as
   a concrete type for variable declarations and try-with-resources;
   forcing them through an interface adds a casting layer for every
   `getInvoiceStatus()` / `close()` call.

## Consequences

- **Public types declared `final` cannot be `final` on the handle
  classes themselves** — JDK records and reflection-newInstance still
  work, but if a future ADR makes them `final`, the bridge stays
  intact.
- **Future module split (e.g. `ksef-client` ↔ `ksef-client-invoice`)
  obsoletes the bridge**: the reflection trick relies on same-module
  package-private access. If the layout splits, replace
  `SessionHandleConstructor` with a colocated package-level factory
  in the new home module, OR add a `module-info.java` `opens`
  directive scoped to the construction sub-package.
- **Handles cannot be subclassed by consumers.** Their constructors
  are package-private; no public constructor means no `extends`.
  This is intended — handles are concrete, stateful resources.
- **`PublicApiSurfaceTest` is the canonical gate.** If a new public
  type is added with internal-typed parameters and someone bypasses
  the bridge, the gate fails CI before Maven Central publish.

## Where it lives

- `internal/client/session/SessionHandleConstructor.java` — bridge,
  cached `Constructor<?>` instances, all reflection.
- `domain/invoicing/KsefSession.java`, `KsefBatchSession.java`,
  `PreparedInvoiceExport.java` — package-private constructors.
- `api/PublicApiSurfaceTest.java` — surface gate.
- `module-info.java` — exports `domain.invoicing`, does NOT export
  `internal.client.session`.
