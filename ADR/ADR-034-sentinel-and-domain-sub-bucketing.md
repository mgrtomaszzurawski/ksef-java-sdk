# ADR-034: Sentinel and domain sub-bucketing when themes diverge

**Date:** 2026-05-20
**Status:** Accepted
**Supersedes:** [ADR-012](ADR-012-package-structure-domain-and-internal-split.md)

## Context

ADR-012 (2026-05-01) established the SDK package layout: `domain/<feature>/`
for functional buckets, `internal/{client,runtime}/` for plumbing, sentinel
packages (`config/`, `common/`, `exception/`) flat with the rule "one kind
of type per package."

That rule held when `config/` contained three classes (`KsefEnvironment`,
`KsefIdentifier`, `RetryPolicy`) — all configuration knobs. It broke a day
later: the 2026-05-02 amendment in ADR-012 promoted credentials from
`domain/authentication/` to `config/`, mixing two kinds (knobs +
credentials). By R3 audit (2026-05-20) `config/` held seven kinds:
environment selectors, sealed credentials hierarchy, identifier types,
behavioural policies (retry, feature, authorization), invoice-type
registry, diagnostic snapshot types, enum tags. ADR-012's sentinel rule
was de facto dead but never updated.

A parallel pressure appeared in `domain/invoicing/`: the bucket grew to
32 root-level types (vs 1 in every other domain bucket) because invoicing
covers more spec surface than any other functionality. Splitting it into
sub-themes (session/, document/, offline/, archive/) inside the bucket
would aid readability without changing the ADR-012 contract that the
bucket is the unit of functionality.

R3 also confirmed `common/` was misnamed: after KsefLimits + TokenInfo
moved to internal, only `StatusInfo` (envelope) and `KsefNumber` (value
object) remained — both legitimate cross-domain types but not "utilities"
in the Apache-Commons sense the name suggests. Renaming to `core/`
followed the AWS SDK V2 convention.

## Decision

### 1. Sentinel packages MAY sub-split when they aggregate multiple themes

The "one kind of type per package" rule applies to **leaf packages**, not
the sentinel directory. When a sentinel package accumulates types from
multiple distinct themes, the right move is to sub-split — each sub-bucket
holds one theme.

`config/` example:

```
sdk/config/
  KsefEnvironment.java               (env selector — singleton theme)
  KsefInvoiceTypes.java              (custom type registry — singleton)
  KsefClientConfig.java              (diagnostic snapshot root)
  KsefCredentialsDescriptor.java        (snapshot field)
  AuthMethod.java                       (enum tag in descriptor)
  package-info.java

  credentials/                       (auth credentials theme — 6 classes)
    KsefCredentials.java                  (sealed interface)
    KsefTokenCredentials.java
    KsefCertificateCredentials.java
    KsefPkcs12Credentials.java
    KsefIdentifier.java
    CertificateSubjectIdentifier.java

  policy/                            (behavioural knobs theme — 4 classes)
    RetryPolicy.java
    FeaturePolicy.java
    UpoVersion.java                       (enum field in FeaturePolicy)
    AuthorizationPolicy.java
```

### 2. Domain buckets MAY sub-split when one bucket far exceeds peers

Most `domain/<feature>/` buckets have one or a few headline types plus
`builder/` + `model/` sub-buckets. When a single domain bucket grows
significantly larger than its peers (rule of thumb: > 12 root-level
files), sub-themes can be introduced inside the bucket:

```
domain/invoicing/
  Invoices.java                       (facade root)
  FormCode.java                       (cross-cutting primitive)
  package-info.java

  session/                            (session lifecycle theme)
  document/                           (Invoice + InvoiceDocument hierarchy)
  offline/                            (offline issuance + builder)
  archive/                            (read-side workflow types)
  builder/                            (external builders)
  qrcode/                             (QR utilities — pre-existing)
  sync/                               (incremental sync — pre-existing)
```

The ADR-024 reflective-bridge constraint (sealed impls must share the
package of their permitting interface) is preserved: each sealed
hierarchy lives entirely inside a single sub-bucket.

### 3. Sub-split depth is ONE level only

A sentinel or domain bucket may sub-split once. Sub-buckets do NOT
further sub-split. If a sub-bucket grows large enough to merit splitting
again, that signals the parent bucket should be reconsidered at the
top level.

### 4. `domain/<feature>/` sub-bucket conventions still apply within sub-themes

The ADR-012 sub-split convention (headlines at root, builders in
`builder/`, records in `model/`) applies recursively — when a domain
sub-bucket (e.g. `domain/invoicing/document/`) needs builders or records,
they live in that sub-bucket's own `builder/` or `model/` directory, not
the outer bucket's.

## Consequences

### Positive

- The "one kind per package" rule is genuinely true at every leaf
  package — no more silent contradictions like ADR-012's amendment.
- `domain/invoicing/`'s 32 root files become 5 thematic sub-buckets,
  cutting cognitive load for new readers.
- New SDK contributors finding "the credentials" (six classes) or
  "the policy knobs" (four classes) get a single coherent folder rather
  than scrolling a flat sentinel.
- Sub-split depth limit of one prevents drift into nested-package mazes.

### Negative

- Existing imports break. Mechanical sed updates are required across the
  codebase (~100 files for the R3 transition).
- `module-info.java` gains additional `exports` lines per new sub-bucket.
- ADR-012 is superseded after only 19 days, signalling the layout was
  still mid-design; future ADRs in this area should be drafted with
  awareness that sub-bucketing was likely from the start.

### Rejected alternatives

1. **Per-credential-type config sub-buckets** (`config.cert`,
   `config.token`, `config.pkcs12`): rejected. Each would hold 1–2 files
   and the shared `KsefCredentials` interface + `KsefIdentifier` would
   be in a sibling location, scattering the auth theme. The current
   `config/credentials/` co-locates the whole sealed hierarchy plus its
   shared identifier types in one folder.
2. **Leave `config/` flat with 16 classes**: rejected. The sentinel rule
   was de facto dead, and the perception of one folder full of every
   "configuration thing" doesn't survive 16-class scale.
3. **Sub-split `domain/invoicing/` into a separate top-level domain bucket**
   (`domain/invoicing-session/`, `domain/invoicing-document/`, etc.):
   rejected. Invoicing is one functional area in spec terms; splitting
   it across multiple buckets misrepresents the domain.

## Cross-references

- [ADR-012](ADR-012-package-structure-domain-and-internal-split.md) —
  superseded; the original layout decision.
- [ADR-024](ADR-024-cross-package-construction-via-reflective-bridge.md) —
  reflective bridge constraint preserved across sub-bucketing.
- [ADR-005](ADR-005-sdk-overlay-on-generated-code.md) — `*Raw` types
  remain in `internal/`; sub-bucketing does not change the public/internal
  split rule.
