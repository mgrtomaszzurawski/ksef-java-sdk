# ADR-005: SDK overlay on generated code

**Date:** 2026-04-03
**Status:** Accepted (amended 2026-05-09 — see Amendment for `xml.*` root scope)
**Last verified:** 2026-05-09

## Context

Generated code from OpenAPI is functional but not ergonomic: mutable POJOs, no validation, no retry, no session awareness, no exception mapping.

## Decision

Hand-written SDK layer wraps generated code. Generated classes get `Raw` suffix via `modelNameSuffix` config. SDK exposes immutable records as public API.

```
Generated (internal)              SDK record (public)
SessionStatusResponseRaw  ──→    SessionStatus
InvoiceMetadataRaw         ──→    InvoiceMetadata
...
```

Consumers never import from `client.*` packages.

Originally this rule extended to `xml.*` packages as well — the
JAXB-generated invoice trees were a hidden internal detail. Per the
2026-05-09 amendment below (and ADR-030), the rule now applies only
to UBL sub-packages of `xml.*` (e.g. `xml.pef.cac`,
`xml.pefkor.cbc`); the four root JAXB packages (`xml.fa2`,
`xml.fa3`, `xml.pef`, `xml.pefkor`) ARE on the public surface,
exclusively as return types of the escape-hatch accessors
`Fa2Invoice.faktura()`, `Fa3Invoice.faktura()`, `PefInvoice.invoice()`,
`PefKorInvoice.creditNote()` and their read-side InvoiceDocument
counterparts.

## Consequences

- Generated code can be regenerated without breaking public API
- SDK layer adds: null guards, exception mapping, retry, session management
- JPMS module-info.java does NOT export `client.*` packages
- Mapping code (`from()` factories) must be maintained when spec changes — but this is mechanical and verifiable with tests

## Scope

KSeF has:
- Complex auth flows (multi-step challenge-response)
- Session lifecycle (open → operate → close)
- Encryption (crypto wrapping of invoice payloads)
- Heterogeneous operations (not just CRUD)

The overlay is therefore thicker and more varied than a uniform Client+Builder triplet would be.

---

## Amendment 2026-05-09 — `xml.*` root packages on consumer surface (ADR-030)

The original "consumers never import from `client.*` / `xml.*`" rule
was relaxed for the four root JAXB packages —
`xml.fa2`, `xml.fa3`, `xml.pef`, `xml.pefkor`. Rationale:

- The typed Invoice / InvoiceDocument classes (`Fa2Invoice`,
  `Fa3Invoice`, `PefInvoice`, `PefKorInvoice` and their `*Document`
  read-side counterparts) keep one JAXB escape-hatch accessor each
  (`faktura()`, `invoice()`, `creditNote()`). Those accessors return
  types from the four root packages.
- A parallel SDK record overlay over the entire JAXB tree was rejected
  as YAGNI (~1500 LOC, drift surface, round-trip-loss risk). See
  ADR-030 for the full decision.
- Common-case fields are surfaced via flat primitive accessors
  (`String`, `BigDecimal`, `LocalDate`, `OffsetDateTime`); only
  uncommon fields need the escape-hatch.

Sub-packages of those four roots (`xml.pef.cac`, `xml.pef.cbc`,
`xml.pefkor.ext`, …) remain internal — `module-info.java` does not
export them and they are not reachable by JPMS consumers via any
public accessor.

See ADR-030 for the detailed design and consequences.
