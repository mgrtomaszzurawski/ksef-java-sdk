# ADR-014: `ApiPaths` — single source of truth for KSeF REST paths

**Date:** 2026-05-01
**Status:** Accepted
**Last verified:** 2026-05-02
**Trigger:** PR #27 SonarQube cleanup. Rule java:S1075 (string literals
that look like URIs) flagged 76 occurrences across the 11 domain clients.
Every client carried its own copies of `"/api/v2/<area>/..."` literals,
each `/api/v2` prefix duplicated in dozens of constants.

## Context

Before this change, the layout in each domain client was:

```java
// CertificateClient
private static final String PATH_CERTIFICATES        = "/api/v2/certificates";
private static final String PATH_CERTIFICATES_LIMITS = "/api/v2/certificates/limits";
private static final String PATH_ENROLLMENTS         = "/api/v2/certificates/enrollments";
// ... 5 more

// PermissionClient — 22 constants, each starting with "/api/v2/permissions/..."

// SessionClient — 14 constants, each starting with "/api/v2/sessions/..."

// ... 8 more domain clients, same pattern
```

Three problems with that:

1. **76 instances of the same literal prefix.** Bumping the major API
   version (`v2 → v3`) meant a search-and-replace across 11 files —
   exactly the scenario S1075 exists to flag. Easy to miss one;
   easier still to introduce a typo.
2. **Sub-path construction was concatenation soup.** Inside method
   bodies the pattern was `PATH_SESSIONS + "/" + ref + "/invoices"`,
   sometimes `PATH_SESSIONS + "/" + ref + "/invoices/" + invoiceRef`,
   sometimes `PATH_SESSIONS + ref + "/close"` (no separator!). Three
   different conventions for the same operation.
3. **No mechanical rule for whether a constant is a "leaf path" or a
   "prefix to extend"** — readers couldn't tell from the constant
   name alone whether `PATH_SESSIONS` was the endpoint or a prefix.

## Decision

Introduce `sdk/internal/runtime/transport/ApiPaths.java`:

```java
public final class ApiPaths {

    public static final String API_BASE = "/api/v2";

    public static final String AUTH         = API_BASE + "/auth";
    public static final String CERTIFICATES = API_BASE + "/certificates";
    public static final String INVOICES     = API_BASE + "/invoices";
    public static final String LIMITS       = API_BASE + "/limits";
    public static final String PEPPOL       = API_BASE + "/peppol";
    public static final String PERMISSIONS  = API_BASE + "/permissions";
    public static final String RATE_LIMITS  = API_BASE + "/rate-limits";
    public static final String SECURITY     = API_BASE + "/security";
    public static final String SESSIONS     = API_BASE + "/sessions";
    public static final String TESTDATA     = API_BASE + "/testdata";
    public static final String TOKENS       = API_BASE + "/tokens";

    public static String subPath(String base, String... segments) {
        StringBuilder path = new StringBuilder(base);
        for (String segment : segments) {
            path.append('/').append(segment);
        }
        return path.toString();
    }

    private ApiPaths() { }
}
```

### Two-level rule

1. **Domain prefix constants live in `ApiPaths`.** One per KSeF domain.
   Built once from `API_BASE`. Bumping `v2 → v3` is a one-line edit.
2. **Per-client sub-path constants live in the client itself**, built by
   string concatenation off the relevant `ApiPaths.*` prefix:

   ```java
   // CertificateClient
   private static final String PATH_CERTIFICATES = ApiPaths.CERTIFICATES;
   private static final String PATH_LIMITS       = ApiPaths.CERTIFICATES + "/limits";
   private static final String PATH_ENROLLMENTS  = ApiPaths.CERTIFICATES + "/enrollments";
   ```

   The literal under each constant no longer contains `/api/v2`, so
   S1075 is satisfied: there is exactly one URI-shaped string per
   domain, and it lives in `ApiPaths`.

3. **Dynamic sub-paths use `ApiPaths.subPath(base, segments...)`** —
   the helper handles separator placement so the three flavors of
   concatenation soup collapse to one form:

   ```java
   // before:  PATH_SESSIONS + "/" + ref + "/invoices/" + invoiceRef
   // after:   ApiPaths.subPath(PATH_SESSIONS, ref, "invoices", invoiceRef)
   ```

   Caller is responsible for any URL encoding of segments. The helper
   is not a URL builder; it is a separator-correct concatenator. This
   is enough because every dynamic segment in the SDK is either a KSeF
   reference number, NIP, PESEL, certificate serial, or other
   alphanumeric identifier already validated by `HttpSupport`'s
   `SAFE_PATH_SEGMENT` pattern.

### Why not a builder DSL

A `UriBuilder` / `path("a").path("b")` style was considered. Rejected:

- Three lines to build a path that should be one line.
- Stateful builders are easy to misuse (forget `.build()`, reuse a
  builder across calls).
- The set of paths is small and known; no need for fluent
  composition.
- `subPath(base, segments...)` is type-checked, side-effect-free, and
  reads top-to-bottom.

### Why not a single flat enum of all 75 paths

- Sub-paths with dynamic segments (`PATH_SESSIONS + "/" + ref +
  "/invoices"`) cannot be enum constants — they need a runtime value.
- Putting only the *prefixes* in `ApiPaths` and letting each client
  declare its own concrete sub-paths keeps the relationship
  domain-local. The next contributor adding a new permissions endpoint
  edits one file (`PermissionClient`) instead of two
  (`PermissionClient` + `ApiPaths`).
- 11 prefix constants is the actual minimum. 75 mostly-similar
  sub-path constants in one file would be a god-class for paths.

## Consequences

### Positive

- API major-version bump is a one-line change to `API_BASE`.
- 76 S1075 findings → 0.
- Dynamic-segment construction has one canonical form
  (`ApiPaths.subPath(...)`), used in 19 call sites.
- Path prefixes are discoverable in one place — a contributor adding a
  new domain knows to extend `ApiPaths` first.

### Negative / trade-offs

- Each domain client still owns ~5-22 path constants — they're shorter
  now, but the count is unchanged. Acceptable: the duplicated
  *prefix* was the real S1075 problem; per-client sub-paths are
  domain-specific by nature and belong with the client.
- `ApiPaths.subPath(...)` is a tiny helper, not a library — readers
  unfamiliar with the convention may reach for `String.format` or
  `String.join` first. Mitigated by the Javadoc on the method and the
  consistent usage across all 11 clients.
- Path constants in `internal/runtime/transport/` are language-public
  (`public static final`). JPMS keeps the package unexported, so
  consumers cannot reference `ApiPaths.SESSIONS` from outside the
  module. The encapsulation guarantee is preserved.

## References

- PR #27 — SonarQube cleanup wave (76× S1075 → 0)
- ADR-012 — package structure: `ApiPaths` lives in
  `internal/runtime/transport/` per the runtime/plumbing rule
- ADR-013 — `HttpRuntime` interface (related: same package)
- SonarQube rule java:S1075 — "URIs should not be hardcoded"
- `sdk/internal/runtime/transport/ApiPaths.java` — the constants + helper
- 75 call sites across 11 domain clients
