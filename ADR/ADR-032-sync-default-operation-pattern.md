# ADR-032 — Sync-default operation pattern (no manual polling everywhere)

Status: **Accepted** (2026-05-17)

Supersedes the Tier 2 portion of [ADR-021](ADR-021-public-api-tiers.md);
ADR-021 status amended to "Amended by ADR-032".

## Context

The KSeF API exposes several asynchronous operations that return a
reference number which the client polls until a terminal status is
reached. ADR-021 introduced a two-tier model:

- **Tier 1 — Workflow API**: SDK hides polling. Examples:
  `session.send()` (polls per-invoice internally),
  `batch.submit()` (polls until terminal then downloads UPOs).
- **Tier 2 — Endpoint API**: raw endpoint shapes returning a reference
  number, leaving polling to the caller for custom orchestration
  (different cadence, partial flows, building blocks).

In practice Tier 2 turned out to be **dead surface**. A pre-1.0 audit
of every Tier 2 use site (certificate enrollment, permission grants
and revokes, token generation) found:

- Every consumer of Tier 2 wrote the same poll loop SDK could have
  written for them (see demo/example code prior to this ADR).
- No real custom-orchestration cases existed (parallel batch,
  webhook-driven completion, partial flows, etc.).
- The "self-imposed no manual polling" contract from
  [ADR-010](ADR-010-sdk-functional-completeness.md) was being
  contradicted by Tier 2 leaking polling responsibility back to the
  consumer.

The pattern was speculative design that never found real users.

## Decision

**Sync default everywhere.** Every async-by-spec KSeF operation
exposes only a synchronous workflow surface. The SDK polls the
backing status endpoint internally and returns when the operation
reaches a terminal state.

Concretely:

- `Certificates.requestNewCertificate(KeyPair)` — composes
  pre-flight (`getLimits`), subject pull
  (`requiredCsrSubjectFromClientSession`, now internal),
  CSR construction, enroll POST, poll-until-terminal,
  retrieve. Returns a complete `RetrievedCertificate`. See
  [R1-20 in the round-1 feedback log](#) for the user-facing rationale.
- `Permissions.grant*` (7 methods covering person, entity,
  authorization, indirect, subunit, EU entity admin, EU entity) —
  return `PermissionOperationStatus` after polling.
- `Permissions.revoke*` (2 methods) — same.
- `Tokens.generate` — returns the terminal state directly.

Each sync method comes in two overloads:

```java
RetrievedCertificate requestNewCertificate(KeyPair keyPair);
RetrievedCertificate requestNewCertificate(KeyPair keyPair, Duration timeout);

PermissionOperationStatus grantPerson(PersonPermissionGrantRequest request);
PermissionOperationStatus grantPerson(PersonPermissionGrantRequest request, Duration timeout);
```

The single-arg overload uses a sensible default timeout (5 min for
permissions, 5 min for certificate enrollment unless the spec says
otherwise). The `Duration timeout` overload lets consumers tune per
call without polluting `KsefClient.Builder` with a global polling
budget.

### Removed from the public surface

- `Certificates.enroll(CertificateEnrollRequest)` — replaced by the
  workflow wrapper.
- `Certificates.getEnrollmentStatus(String)` — internal-only after
  this ADR (used by the workflow wrapper's poll loop).
- `Certificates.requiredCsrSubjectFromClientSession()` —
  internal-only after this ADR (called by the workflow wrapper
  during CSR construction).
- `Permissions.getOperationStatus(String)` — internal-only after
  this ADR.
- `sdk.common.KsefAsync` and `sdk.common.KsefAsyncStatus` —
  internal utilities (no longer part of a public Tier 2 polling
  contract).

### Kept on the public surface

- Tier 1 query/stream patterns are unaffected (these are not
  reference-number-based async; they are paged synchronous reads).
- `Certificates.getLimits()` stays as a standalone pre-check
  (consumers may want to inspect quota without enrolling).
- `Certificates.retrieve(List<CertificateSerialNumber>)` stays —
  fetch-by-serial is a separate use case from "enroll a new
  certificate".
- `Certificates.revoke` stays.
- All `query*` and `stream*` Permissions methods stay.

## Consequences

- The two-tier mental model in ADR-021 collapses to a single tier:
  workflow API. Documentation simplifies; consumer code shortens.
- Breaking change for any code that was hand-rolling poll loops on
  cert enrollment, permission grants, or token generation. Pre-1.0
  the SDK has no published consumers, so this is consequence-free
  for now.
- The `Duration timeout` overload pattern formalises per-operation
  timeout control without "magic" global config knobs. Consumers
  who want bulk timeout consistency can pass the same `Duration`
  through their orchestration layer.
- Demo and examples lose ~60% of their manual-polling boilerplate
  (cert flows shrink to a single call).

## Alternatives considered

1. **Status quo (keep Tier 2)** — rejected. Six months of "Tier 2
   exists in case someone wants it" with zero consumers means the
   pattern is wasted surface.
2. **`CompletableFuture<T>` returns** — rejected. Users who need
   async wrapping can do it themselves with `ExecutorService` or
   virtual threads (Java 21+); embedding async in the SDK's public
   surface adds a transitive concurrency model the consumer didn't
   ask for.
3. **Builder-level `pollPolicy` config** — rejected. Global state
   for what is a per-operation timeout, plus the magic-config-knob
   smell.

## References

- [ADR-021](ADR-021-public-api-tiers.md) — original two-tier model
  (this ADR amends).
- [ADR-010](ADR-010-sdk-functional-completeness.md) — "no manual
  polling" self-contract (this ADR reasserts at full SDK scope).
- [ADR-011](ADR-011-batch-encryption.md) — batch-side sync-by-design.
