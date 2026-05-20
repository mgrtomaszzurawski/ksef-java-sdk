# ADR-033 — Permissions: sealed `PermissionGrantRequest` + single `grant(req)` dispatch

**Status**: accepted
**Date**: 2026-05-19
**Amends**: ADR-032 (sync-default operation pattern), specifically the
"Permissions.grant\* (7 methods)" ratification on lines 51-54.

## Context

R2-11a sealed-vs-open audit revealed that the `Permissions` interface
exposed 14 grant methods (7 typed grants × 2 overloads — sync default
and explicit `Duration timeout`). ADR-032 ratified the 7-named-methods
pattern as deliberate (each name maps 1:1 to a KSeF wire endpoint
with distinct required fields per `ksef-docs/uprawnienia.md`).

User feedback during R2-11a discussion:
- The 7 separate wire endpoints justify 7 separate request *types*
  (distinct required fields). That stays.
- The 7 separate consumer-facing *method names* duplicate information
  the request type already carries. The SDK can dispatch on the
  request type internally; the consumer picks the type by builder
  navigation.
- Sealing the parent interface is cheap: future spec evolution (KSeF
  adding a permission category) lands as an SDK minor-version bump
  that extends the `permits` clause + adds the corresponding wire
  dispatch.

User explicitly chose sealed over open-registry: "co do permissions
zrob to na ten moment jako sealed interface zmienienie go na otwarty
to nie jest dużo roboty". Per-case decision, not project-wide rule —
invoice-type registry (R2-6 ext) stays open because the per-domain
trade-offs differ.

## Decision

Introduce a sealed parent `PermissionGrantRequest` in
`sdk.domain.permissions.model` permitting the existing 7 concrete
request records. Collapse the 14 grant methods on the `Permissions`
interface to 2 methods:

```java
PermissionOperationStatus grant(PermissionGrantRequest request);
PermissionOperationStatus grant(PermissionGrantRequest request, Duration timeout);
```

`PermissionsImpl.grant(...)` dispatches via Java pattern matching:

```java
return switch (request) {
    case PersonPermissionGrantRequest r -> dispatchGrant(PATH_GRANT_PERSON,
            mappers.toPersonRaw(r), OP_GRANT_PERSON, timeout);
    case EntityPermissionGrantRequest r -> dispatchGrant(PATH_GRANT_ENTITY,
            mappers.toEntityRaw(r), OP_GRANT_ENTITY, timeout);
    // ... 5 more arms — exhaustive on sealed parent
};
```

Wire endpoints stay separate per spec (7 different KSeF URLs with
different required-field schemas). Only the SDK consumer-facing
surface is unified.

### Kept from ADR-032

- Sync-default polling pattern with 5-minute default timeout.
- The dual-overload pattern (`grant(req)` + `grant(req, Duration)`).
  Per-call timeout tuning stays in the method signature — different
  permissions have different terminal-state latencies in practice and
  a single global Builder field cannot serve "interactive cert
  enroll" + "batch session of 10k invoices" + "REST permission grant"
  simultaneously (R2-11 Finding A reject reasoning).
- `revokePermission(permissionId)` and `revokeAuthorization(permissionId)`
  stay as two separate methods — they map to two separate KSeF DELETE
  endpoints (`/permissions/common/grants/{id}` vs
  `/permissions/authorizations/grants/{id}`) and the SDK cannot infer
  which from a permission ID alone.

### Removed from the public surface

- `grantPerson`, `grantEntity`, `grantAuthorization`, `grantIndirect`,
  `grantSubunit`, `grantEuEntityAdmin`, `grantEuEntity` — replaced by
  the single sealed-dispatch `grant(request)` method (× 2 overloads).

## Consequences

- 14 grant methods on the public surface collapse to 2. Net surface
  size drops from 35 to 23 user-facing methods on the `Permissions`
  interface.
- IDE autocomplete on `permissions.grant(` shows the sealed parent
  type; consumers navigate to the concrete builder
  (`PersonPermissionGrantBuilder.create()...`) per their use case.
- Compile-time exhaustiveness in the SDK: adding an 8th
  `permits` member without extending the `switch` becomes a compile
  error.
- Pre-1.0 breaking change for any code calling the named methods; per
  CLAUDE.md release-status policy this is consequence-free (no
  published consumers).
- Demo runner, examples, and unit tests update from
  `permissions.grantPerson(req)` to `permissions.grant(req)`. 25
  call-sites total in the repo.

## Alternatives considered

1. **Status quo (7 named methods)** — rejected. The 7 names mirror
   wire endpoint paths but consumer-facing redundancy was real: each
   name is fully implied by the concrete request type. Sealed
   dispatch preserves all type safety without the duplication.
2. **Open interface + registry (analogous to `KsefInvoiceTypes`)** —
   rejected. The R2-6 ext invoice registry was justified by a real
   user-extension use case (custom typed wrapper around standard FA3
   XML). For permissions, KSeF accepts only the 7 spec-defined types
   — any unknown grant request would be rejected wire-side. Open
   extension would invite footguns without serving a real need. KSeF
   spec evolution lands via SDK minor-version bumps.
3. **Single `grant()` with all fields in one mega-request type** —
   rejected. Different grant types have non-overlapping required
   fields; one mega-type would require nullable everything plus
   runtime validation. Sealed pattern keeps compile-time guarantees.

## References

- ADR-032 — sync-default operation pattern (this ADR amends the
  Permissions section).
- ADR-021 — original two-tier API model (superseded by ADR-032).
- `ksef-docs/uprawnienia.md` — KSeF permissions spec (983 lines, 7
  distinct grant endpoints).
- R2-11a audit:
  `context/claude-code-FINDINGS-2026-05-19-permissions-audit-revised.md`
  (audit's Alternative #1: sealed interface, accepted).
