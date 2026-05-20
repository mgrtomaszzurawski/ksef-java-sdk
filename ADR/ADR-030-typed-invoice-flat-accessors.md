# ADR-030: Typed Invoice / InvoiceDocument flat-accessor surface

**Date:** 2026-05-09
**Status:** Accepted
**Last verified:** 2026-05-09
**Supersedes (in part):** ADR-005 §"consumers never import from `client.*` / `xml.*`"

> Note: an internal task description referred to this decision as
> "ADR-022". ADR-022 was already taken (REQ-ID citation discipline);
> this one ships as ADR-030 — content unchanged.

## Context

PR12b shipped four typed authoring classes (`Fa2Invoice`,
`Fa3Invoice`, `PefInvoice`, `PefKorInvoice`) and PR14 shipped four
read-side counterparts (`Fa2InvoiceDocument`, `Fa3InvoiceDocument`,
`PefInvoiceDocument`, `PefKorInvoiceDocument`). Each class wrapped a
JAXB-generated tree (`Faktura`, `InvoiceType`, `CreditNoteType`) and
exposed accessors that returned JAXB raw types directly:
`header() : TNaglowek`, `sellerIdentity() : TPodmiot1`,
`accountingSupplierParty() : SupplierPartyType`, etc.

Consequences observed:

- `module-info.java` had to `exports` and `opens` not just the four
  root JAXB packages but also their UBL sub-packages
  (`xml.pef.cac`, `xml.pef.cbc`, `xml.pefkor.ext`, …) — 24 sub-package
  opens in total — because consumers needed reflective + compile-time
  reach into them.
- The SDK's public API surface inherited the entire UBL Common
  Aggregate / Basic Components type ecosystem. A consumer that called
  `pefInvoice.lines()` got back `List<InvoiceLineType>` and had to
  navigate `getInvoicedQuantity().getValue()`,
  `getItem().getName().getValue()`, and so on.
- A consumer that called `fa3Invoice.sellerIdentity()` had to know to
  call `getNIP()` and `getNazwa()` on the returned `TPodmiot1` —
  Polish-cased getters generated from the XSD.
- Generated JAXB types are mutable. Returning them directly from a
  read-side document made the read-side mutable in practice.

A previous proposal to overlay the entire JAXB tree with a parallel
hierarchy of SDK records (~11 records, ~1500 LOC) was rejected as
YAGNI: it doubled the surface, added a drift surface (every JAXB
field change required a record update), and risked round-trip data
loss because the SDK records would by definition cover only the fields
the SDK chose to expose.

## Decision

Each typed Invoice / InvoiceDocument class:

1. Keeps the JAXB tree as a private final field — JAXB remains the
   canonical state.
2. Exposes **flat primitive accessors** (`String`, `BigDecimal`,
   `LocalDate`, `OffsetDateTime`, ISO 4217 currency code as `String`,
   wire-level enum tokens as `String`) for the common-case fields a
   consumer actually wants: invoice number, issue date, currency,
   seller / buyer NIP and name, total amounts, invoice type code.
3. Exposes one SDK record collection per repeating XSD element —
   `List<InvoiceLineItem>` for FA(2)/FA(3) `<FaWiersz>`,
   `List<PefInvoiceLine>` for UBL `<cac:InvoiceLine>`,
   `List<PefCreditNoteLine>` for UBL `<cac:CreditNoteLine>`. The
   records hold primitives only; they do not nest JAXB types.
4. Keeps **one** JAXB escape-hatch accessor — `faktura() : Faktura`,
   `invoice() : InvoiceType`, `creditNote() : CreditNoteType` — for
   consumers who need fields the flat accessors do not surface.

Accessors are read-through (no caching, no parallel state). When the
JAXB tree has a null sub-element, the accessor returns `null` (or
`Optional.empty()` for the one optional `netTotal()` field that is
genuinely optional in the XSD).

For the line-item collection accessors: a JAXB row that lacks any
required SDK-record field is skipped rather than fabricated with
sentinels. Skipping is reversible (the consumer can read the same
row through `faktura().getFa().getFaWiersz()` if needed); fabricated
sentinels would silently corrupt downstream calculations.

## Consequences

### Positive

- The SDK public API surface stops leaking the UBL CAC/CBC/EXT/UDT
  type ecosystem. `module-info.java` exports drops from 12 `xml.*`
  packages to 4 (the four root packages, retained because the
  escape-hatch accessors return types from them).
- `module-info.java` opens drops from 30 `xml.*` packages to 6
  (4 invoice roots + `upo` + `auth`, the latter two used only by
  internal mappers).
- Read-side documents stop leaking JAXB mutability — primitives are
  immutable.
- The escape-hatch is opt-in. A consumer that uses only the flat
  accessors never imports a JAXB type.
- No drift surface between the wire schema and the SDK: there is no
  parallel record hierarchy to keep in sync.
- No round-trip data loss: `xml()` returns the bytes the SDK
  marshalled / received, byte-for-byte.

### Negative / trade-offs

- The four root `xml.*` packages (`xml.fa2`, `xml.fa3`, `xml.pef`,
  `xml.pefkor`) ARE on the consumer-visible surface. A consumer that
  uses the escape-hatch will import `Faktura`, `InvoiceType`,
  `CreditNoteType` directly. This is a deliberate scope-down of the
  ADR-005 rule "consumers never import from `xml.*`": the rule now
  applies only to UBL sub-packages, not to the four roots.
- The flat accessors expose enum values as wire-level `String`
  tokens (`"VAT"`, `"KOR_ZAL"`, …) rather than typed enums. This is
  intentional: the SDK does not own the enum vocabulary — it is
  defined by the XSD and may be extended by the schema authority
  (Ministry of Finance) at any time. Returning the wire string keeps
  the SDK forward-compatible without a per-extension SDK release.
- Adding a new flat accessor for a previously-unsurfaced field is a
  one-line edit per typed class (read-through), but it must be
  remembered as a backlog item when the schema changes.

## Pattern reference

This is the same pattern AWS SDK for Java v2 uses for
service-generated request / response types: the SDK exposes the
generated types directly, with one fluent builder per type and one
typed read-through accessor per field. The SDK does not overlay a
separate record hierarchy on top of every generated type. KSeF SDK
does the same for the four typed Invoice / InvoiceDocument classes,
with the additional simplification that XSD-generated JAXB types are
not idiomatic Java records and are therefore wrapped one level deeper
to flatten Polish-cased getters and JAXB nesting into primitive
accessors.

## References

- ADR-005 — SDK overlay rule (relaxed for the four `xml.*` roots)
- ADR-021 — public-API tiers (the escape-hatch is the "endpoint" tier
  for invoice authoring / read)
- PR12b — typed Invoice authoring classes (introduced raw accessors)
- PR14 — typed InvoiceDocument read-side classes (introduced raw
  accessors)
- PR21 — this decision (replaced raw accessors with flat
  primitives + collection records)
