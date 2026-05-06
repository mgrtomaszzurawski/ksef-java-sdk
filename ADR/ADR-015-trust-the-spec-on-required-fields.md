# ADR-015: Trust the OpenAPI spec on `required` fields — drop dead defensive null-checks

**Date:** 2026-05-02
**Status:** Accepted
**Last verified:** 2026-05-02
**Trigger:** USER-REVIEW-REMARKS finding #6 — IDE inspection
*"Condition `raw.getUsage() != null` is always 'true'"* on
`PublicKeyCertificate.from()`. A mechanical scan against
`openapi/open-api.json` showed **45 dead null-checks** across 30+ `from()`
factories where the corresponding spec field is marked `required: true`.

## Context

The OpenAPI generator produces `client.model.*Raw` classes with
`@jakarta.annotation.Nonnull` on every getter for a `required` schema
field. Despite this contract, hand-written `from(XxxRaw)` factory
methods in `sdk.*` defensively guarded those getters with
`raw.getX() != null` ternaries — defaulting to `List.of()` or `0` or
`null` if the contract were violated.

```java
// shape 1 — list with empty fallback
List<X> mapped = raw.getCertificates() != null
        ? raw.getCertificates().stream().map(X::from).toList()
        : List.of();

// shape 2 — scalar with default fallback
raw.getCode() != null ? raw.getCode() : 0
raw.getCertificateType() != null ? raw.getCertificateType().getValue() : null
```

A scan classified every such check by cross-referencing the schema's
`required` array:

- **45 dead** — the spec marks the field required; the check is
  unreachable under the contract.
- **8 live** — the spec marks the field optional; the check is
  legitimate.
- **0 ambiguous** — every site classified deterministically.

SonarQube's `java:S2589` ("Boolean expressions should not be gratuitous")
should fire on the 45, but didn't propagate the
`@jakarta.annotation.Nonnull` annotation across the package boundary
between generated `client.model` and hand-written `sdk.*` consumers.
IntelliJ's data-flow analysis caught it; the static-analysis pipeline
did not.

## Decision

**Trust the spec.** Delete the 45 dead null-checks. A KSeF response that
violates the spec contract on a `required` field is a server bug — let
it surface as `NullPointerException` from the offending line, not as a
silent `List.of()` / `0` / `null` masking the problem.

### Carve-out for known server violations

If a documented bug report (RCA) records a specific field where the
KSeF server has been observed returning null despite the spec marking
it required, that field's null-check stays in place with an inline
pointer:

```java
// RCA-XX: server observed returning null despite spec marking required;
// retain defensive default until upstream fixes the contract.
raw.getCode() != null ? raw.getCode() : 0
```

The carve-out is **per-field**, not per-class. Defensive checks must be
justified one at a time.

### Why not "defensive coding always wins"

Defensive coding against contract violations:

- **Hides bugs.** Silent `List.of()` looks identical to "no items
  returned" in the consumer's code path. The actual data loss is
  invisible until production reconciliation finds the discrepancy.
- **Bloats the codebase.** 45 ternaries × ~3 lines each = 135 lines of
  dead defensive code. Each line adds a future maintenance vector
  (lint warnings, refactor mistakes, reviewer cognitive load).
- **Discourages contract enforcement.** If we don't trust the spec,
  there's no incentive to keep the spec correct.

The right escalation path on an actual server violation:

1. NPE surfaces in test or production telemetry.
2. RCA / bug report recorded.
3. Issue filed with KSeF (CIRFMF/ksef-docs).
4. Per-field defensive check added with the RCA reference.

This makes contract violations *visible*, not silent.

### Why not `@SuppressWarnings("ConstantConditions")` everywhere

That's the worst of both worlds — the dead branch stays, the inspection
gets muted, the next reader can't tell if the suppression was justified
or pasted in haste. Either fix the inspection (delete the branch) or
keep the branch with a documented reason. No third option.

### Configuration follow-up

SonarQube didn't catch the 45 — its cross-package nullability
inference doesn't propagate `@jakarta.annotation.Nonnull` from
`client.model.*Raw` to `sdk.*` consumers. Worth investigating
`<sonar.java.libraries>` configuration so future regressions get
caught automatically. Recorded as a Phase 9 follow-up under finding
#6 action items.

### Test stub follow-up

After deletion, one test (`SessionClientTest.getInvoiceStatus_*`)
failed because its stub response omitted a `required` field
(`ordinalNumber`). The test was passing previously only thanks to the
dead defensive check that returned `0`. Test stub corrected to match
the spec contract — exactly the diagnostic value of removing the
checks: hidden test gaps surface immediately.

## Consequences

### Positive

- 40 lines of dead defensive code removed across 37 files
  (the original 45 figure included ~5 if-block style checks not
  rewritten in this pass — those are pure shape-3 conditional blocks
  on optional-feeling fields and were left for manual case-by-case
  review).
- Server contract violations now fail loud instead of masking.
- One existing test stub corrected to comply with the spec contract.
- Future regressions detectable: an inspection warning means the
  spec contract changed (field became optional) or the code is
  out-of-sync with the spec.
- Aligns with the existing rule (CLAUDE.md): "Don't add error
  handling, fallbacks, or validation for scenarios that can't
  happen."

### Negative / trade-offs

- A real KSeF server bug that violates the spec on a required field
  will now produce NPE in consumer code rather than being masked.
  Mitigation: the RCA carve-out, plus the SDK's typed exception
  hierarchy still catches malformed JSON before it reaches the
  factory (deserialization fails first if the field is missing
  rather than null).
- Reviewers checking spec contracts must know to use the carve-out
  pattern rather than reverting to defensive style.

## References

- USER-REVIEW-REMARKS-2026-05-01-2130.md finding #6 — full list of
  45 dead checks and 8 live checks
- ADR-005 — SDK overlay rule (`*Raw` types are internal)
- SonarQube rule java:S2589 — "Boolean expressions should not be
  gratuitous"
- IntelliJ inspection "Constant Conditions"
