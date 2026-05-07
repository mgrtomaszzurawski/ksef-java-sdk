# ADR-012: Package structure — `domain/` for functionality, `internal/{client,runtime}/` for plumbing

**Date:** 2026-05-01
**Status:** Accepted
**Last verified:** 2026-05-02
**Trigger:** PR #25, #26, dev commit `59d3703`. The pre-Phase-9 state had 27
files at the `sdk/` package root, 90 records flat in `sdk/model/`, 25 builders
flat in `sdk/model/builder/` — file count, not semantics, was the de-facto rule
for whether something got a sub-package.

## Context

After Phase 8 closed (PR #12) the layout was:

```
sdk/
  KsefClient.java + 26 other top-level types
  crypto/        (CryptoService, CertificateLoader)
  signing/       (SigningService)
  http/          (HttpSupport)
  exception/
  model/         <- 90 records flat
    builder/     <- 25 builders flat
  paging/        <- unused (ADR-005 resolved decision #7)
```

User pushback at the start of this session:

1. The `sdk/` root mixed entry-points (`KsefClient`), abstractions
   (`KsefSession`, `KsefBatchSession`), credential types, configuration types,
   and 11 domain clients — readers had no signal of which were public-API
   entry types vs. internal mechanism wrappers.
2. `model/` and `model/builder/` collapsed *all* 90 records / 25 builders
   regardless of which functionality they served. Finding "everything for
   permissions" required searching by name prefix in a flat 90-element list.
3. `internal/` was a named bucket for "infrastructure" that nonetheless mixed
   two distinct categories — wrappers over KSeF endpoints used only by
   `KsefClient` (`AuthClient`, `SessionClient`, `SecurityClient`) and
   cross-cutting plumbing (`crypto`, `signing`, `http`, `batch helper`).
4. `internal/auth/` (one bucket) and `authentication/` (another bucket) were
   confusing: same domain, two different sub-packages, no rule for which goes
   where.

NoviCloud SDK uses `resources/<domain>/` for domain clients, but that pattern
fits CRUD resources, not the KSeF set of cross-cutting *functionalities*
(permissions, certificates, tokens, sessions, etc.). The decision below
adopts the spirit of NoviCloud's grouping while renaming it.

## Decision

### 1. Three top-level groupings

```
sdk/
├── KsefClient.java                  # the only entry point in sdk/ root
├── package-info.java
├── config/                          # configuration types: KsefEnvironment, KsefIdentifier, RetryPolicy
├── common/                          # shared types: StatusInfo, TokenInfo, public-key types
├── exception/                       # 8 typed exceptions
│
├── domain/                          # PUBLIC functionality buckets (8)
│   ├── authentication/  (KsefCredentials + 3 implementations)
│   │   └── model/       (10 auth-flow response models)
│   ├── invoicing/       (KsefSession, KsefBatchSession, FormCode)
│   │   ├── builder/     (4 builders)
│   │   ├── model/       (~26 records)
│   │   ├── batch/       (BatchFileSpec)
│   │   └── qrcode/      (QrCodeService)
│   ├── permissions/     (PermissionClient + builder/ + model/)
│   ├── tokens/          (TokenClient + builder/ + model/)
│   ├── certificates/    (CertificateClient + builder/ + model/)
│   ├── peppol/          (PeppolClient + model/)
│   ├── limits/          (LimitsClient, RateLimitClient + model/)
│   └── testdata/        (TestDataClient + builder/ + model/)
│
└── internal/                        # NOT exported via JPMS
    ├── client/                      # endpoint wrappers used only by KsefClient
    │   ├── auth/        (AuthClient, SessionContext)
    │   ├── session/     (SessionClient)
    │   └── security/    (SecurityClient)
    └── runtime/                     # cross-cutting infrastructure
        ├── transport/   (HttpSupport, HttpRuntime, RetryHandler, ApiPaths)
        ├── crypto/      (CryptoService, CertificateLoader)
        ├── signing/     (SigningService)
        └── batch/       (BatchPackageBuilder)
```

### 2. Sub-split convention inside `domain/<feature>/`

Strict rule (regardless of file count):
- *Headline* types (clients, credentials, session abstractions) live at the
  bucket root.
- *Builders* live in `<bucket>/builder/`.
- *Records* (response models) live in `<bucket>/model/`.

Sentinel packages (`config/`, `common/`, `exception/`) keep flat layouts —
they hold one *kind* of type each.

### 3. `internal/` is two distinct sub-buckets

- `internal/client/<area>/` — REST endpoint wrappers that exist solely to be
  called by `KsefClient` under the hood. The user does not invoke them
  directly. JPMS does not export this tree.
- `internal/runtime/<purpose>/` — purely cross-cutting plumbing. Transport,
  crypto, signing, batch helper. JPMS does not export this tree either.

The split makes the conceptual boundary explicit. Mixing both in a flat
`internal/` blurred it.

### 4. `KsefClient` is the only file at `sdk/` root

Everything else is reachable via the high-level facade or via accessor
methods on it (`client.permissions()`, `client.openSession()`, etc.). Putting
session abstractions, credentials, FormCode, environment, identifier types
at the package root signaled "use these directly" — when in fact most of
them are used *through* `KsefClient`.

### 5. JPMS exports

Every `sdk.*` package outside `sdk.internal.*` is exported via
`module-info.java`. The `internal` tree is invisible to consumers. The
rule was not strictly enforced before (some `internal` types were
package-public for cross-package access from `KsefClient`). After the
restructure, those types remain `public` for the language-level access,
but JPMS prevents consumer reach.

## Consequences

### Positive

- A reader looking for "how do I work with feature X" finds everything
  (client + builders + models) in one folder.
- Adding a new feature has an obvious template — copy any existing
  `domain/<feature>/` shape.
- The `internal/client/` vs `internal/runtime/` split tells the next
  contributor what category their new helper belongs to, which prevents the
  internal/ folder from becoming a junk drawer.
- Bumping the API version (v2 → v3) is co-located with `internal/runtime/
  transport/ApiPaths` (per ADR-014); domain clients don't carry version
  literals.
- `package-info.java` per exported package gives Javadoc-tool a hook for
  package-level docs.

### Negative / trade-offs

- 154 `package` declarations changed; ~200 import sites updated. One-time
  cost, paid in PR #25 + dev commit `59d3703`.
- `module-info.java` exports list got long (one line per
  `domain/<feature>/{,builder,model}` package). Trade-off accepted —
  explicit is better than wildcard for SDK API surface.
- Internal types that were package-private before (e.g.
  `BatchPackageBuilder`, `KsefSession` constructor) had to widen to `public`
  for cross-package access. JPMS still prevents consumer reach (per ADR-005),
  so the encapsulation guarantee is unchanged. Each widened type carries an
  inline `@apiNote internal — not part of public SDK contract` comment.

## References

- PR #25 — initial per-functionality grouping
- PR #26 — uniform sub-split convention + `FormCode` collision rename
- dev commit `59d3703` — `domain/` parent + `internal/{client,runtime}` split
- ADR-005 — SDK overlay rule (consumers never reach generated `client.*`)
- ADR-013 — `HttpRuntime` interface (related: how the transport layer sits
  in `internal/runtime/transport/`)
- ADR-014 — `ApiPaths` centralisation (uses the `internal/runtime/transport/`
  bucket)
- NoviCloud SDK `resources/<domain>/` pattern — inspiration, adapted to
  KSeF's functionality-not-resource shape

---

## Amendment 2026-05-02 — credentials promoted to `config/`, auth records pushed to `internal/`

User-review pass revealed two follow-up moves that refine this decision
without overturning it:

1. **`KsefCredentials` + 3 implementations moved from
   `domain/authentication/` to `config/`.** Rationale: from the
   consumer setup-flow standpoint, credentials are configuration handed
   to `KsefClient.builder()` — same role as `KsefEnvironment`,
   `KsefIdentifier`, `RetryPolicy`. Treating them as a domain bucket was
   misleading because the bucket had no client/builder/service of its
   own, only input + output records.

2. **Authentication response records moved from
   `domain/authentication/model/` to `internal/client/auth/model/`,**
   next to their only producer `AuthClient`. Currently still exported
   via `module-info` to keep `KsefClient.auth()` accessor working;
   removal of that export tracked under finding #3 / fork (a) (planned:
   ADR-016).

3. **`domain/authentication/` package deleted entirely.**

Net effect: `domain/` now only contains buckets that genuinely have a
client at the bucket root (invoicing, permissions, tokens, certificates,
peppol, limits, testdata = 7 buckets). Sub-split convention §2 of this
ADR remains unchanged — credentials stayed compliant with "headline
types live at the bucket root" simply by being relocated to `config/`,
which itself follows the sentinel-package rule (one *kind* of type per
package).
