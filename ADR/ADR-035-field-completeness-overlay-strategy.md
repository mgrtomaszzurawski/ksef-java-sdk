# ADR-035: Field-completeness overlay strategy (mirror the XSD)

**Date:** 2026-07-26
**Status:** Accepted
**Extends:** [ADR-005](ADR-005-sdk-overlay-on-generated-code.md), [ADR-030](ADR-030-typed-invoice-flat-accessors.md)

## Context

The typed overlay (ADR-005: immutable records built via `from(jaxb)`
factories) originally covered only the common-case subset of the FA(2)/FA(3)
invoice schema. A field-depth audit (2026-07-26, `tools/coverage/field-coverage.py`)
measured the real gap: of the accessors the generated JAXB tree exposes, the
SDK read **~18% of FA(3) leaf fields**. A consumer needing any of the other
~82% was forced to `unsafeJaxbView()` — for a strongly-typed SDK whose promise
is "you never touch the raw XML", an unreachable field is data loss (the P_11A
line-item drop, #97, was one instance).

Closing that gap is not one change but ~15 sections of work, so the overlay
strategy had to be decided **once, from the whole forward map**, not per
section. The map (75 owning types with ABSENT leaves) shows:

- Only **two "hub" records grow flat**: the invoice header (`Fa`, +15 scalars)
  and the line item (`FaWiersz`, +12 scalars).
- The other **~73 types are XSD sub-complex-types** (`Platnosc`, `Transport`,
  `Zamowienie`, `Rozliczenie`, addresses, person identifiers, bank accounts,
  attachment blocks up to 6 levels deep) — new records regardless of any
  construction-shape choice.

Crucially, the hub fields (`P_13_6_1`, `P_15_ZK`, `CN`, `P_10`, …) are declared
**flat on their parent in the XSD** — they are not sub-elements. Grouping them
into an invented "details" record would fabricate structure the schema does not
have, contradicting the project rule that spec vocabulary and structure win.

## Decision

**Mirror the XSD.** The typed overlay is extended to the ABSENT sub-tree under a
single rule, applied consistently to every section:

1. **A flat XSD scalar becomes a flat record field** (ADR-030). The two hub
   records grow flat; construction ergonomics for a widened record are solved by
   a **fluent builder** (the SDK-wide convention), not by inventing nesting and
   not by a back-compat convenience constructor (forbidden pre-1.0).
2. **An XSD sub-complex-type becomes its own nested immutable record**, nested
   as the schema nests. Cohesive groups the schema itself models as elements
   (`Platnosc`, `Transport`, `Rozliczenie`, …) are their own records.
3. **Read-first.** Records are populated via `from(jaxb)` factories / document
   accessors; this is what moves the field-coverage numerator. Write/builder
   parity is delivered per section, following read, so a field is both readable
   and settable.
4. **KSeF boolean markers** (`xsd:byte` with 1 = yes, 2 = no) map to
   `@Nullable Boolean`; absent stays `null`.

This is not a new architecture — it is the fuller application of ADR-005 that
built the covered 18%.

## Consequences

- `InvoiceLineItem` grows to the full flat `FaWiersz` scalar surface and gains
  `InvoiceLineItem.builder()`; its 20 positional call sites migrate to the
  builder (no convenience shim). First instance of the rule (B.T1).
- Progress is measured, not asserted: each section states its real
  `field-coverage.py` before/after ratio; an ABSENT leaf is presumed data loss
  to fix, demoted to a documented defer only with a concrete spec reason.
- `100%` DEPTH is not the literal target (the denominator over-counts schema
  branches KSeF never populates); the target is that every remaining ABSENT
  leaf is either mapped or an explicitly justified exception.
- The doctrine governs the whole field-completeness effort (line item, header
  VAT summary, payment, transaction conditions, corrections, order, settlement,
  attachments, …), keeping the growing overlay spec-faithful and predictable.
