<!--
PR template for ksef-java-sdk.

Required sections: Summary, Spec citations, Test plan.
See ADR-022 for citation discipline.
-->

## Summary

<!--
1-3 sentences. What changes and why. The why should reference the
underlying motivation: spec gap, architecture decision, bug report,
performance concern. Avoid restating the diff.
-->

## Spec citations

<!--
List the REQ-IDs this PR closes or modifies, citing
context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md (or the latest
quarterly refresh). One bullet per REQ-ID with a one-line note on what
this PR does for that requirement.

If this PR doesn't touch spec behavior (pure refactor, doc-only,
internal cleanup), write "None — non-spec change" and explain why.

If this PR introduces a new spec requirement that wasn't in the audit,
add a new REQ-* entry to the audit in this same PR and cite it here.

Format:

- REQ-OFFLINE-003 — adds `hashOfCorrectedInvoice` field to
  `SendInvoiceCommand.TechnicalCorrection`, closes the documented korekta
  techniczna flow
- REQ-SESS-18 — `KsefNumber.parse(...)` rejects non-35-character input
- ADR-019 — implements `QrSigningService` per RSA-PSS / ECDSA auto-detection
-->

## Test plan

<!--
- [ ] `mvn verify --no-transfer-progress` green locally
- [ ] new tests added for each REQ-ID closed by this PR (cite test
      class:method per REQ-ID where possible)
- [ ] examples/ still compiles (covered by `mvn verify` after Step 1)
- [ ] no public API surface regression (PublicApiSurfaceTest stays green)
- [ ] (if applicable) `mvn exec:java -pl ksef-demo -Ddemo.mode=AUTH_SAFE`
      against demo environment
-->

## Backwards compatibility

<!--
Required for any PR that changes public API surface (new public type,
modified signature, removed deprecated method, etc).

- [ ] no public-API breaking change
- [ ] breaking change documented in CHANGELOG with migration note
- [ ] removed type was deprecated in a prior release (cite which)
-->

## Notes for reviewer

<!--
Optional. Anything non-obvious about the implementation, alternatives
considered, or follow-up work intentionally deferred.
-->
