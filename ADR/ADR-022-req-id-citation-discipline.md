# ADR-022: REQ-ID citation discipline

Date: 2026-05-03
Status: Accepted

## Context

Eight rounds of post-hoc Codex review surfaced spec gaps the SDK had
shipped. Each round read `ksef-docs/` markdown files we didn't read
before writing the corresponding code, and found cases where:

- a spec-mandated field was missing (`hashOfCorrectedInvoice`)
- a spec-mandated validator was missing (KSeF number CRC-8)
- a spec-mandated wire format was wrong (KOD II signing payload had
  `https://` prefix that shouldn't be there)
- spec-allowed-but-not-implemented features were silently absent (XAdES
  profiles beyond BASELINE-B, alternative digests)

A 2026-05-03 internal audit extracted 223 concrete spec requirements
with REQ-IDs and classified each as ✅ ok / ⚠️ untested / ❌ missing.
That audit closed the gap for one moment in time.

Without process discipline the gap reopens within 2-3 PRs:

- new code lands without checking spec
- audit goes stale as `ksef-docs` evolves
- next review round finds new violations
- retro-fix loop resumes

## Decision

Every spec-touching change in this codebase cites a REQ-ID from the
audit (or introduces a new REQ-ID + audit entry as part of the change).

Rules:

1. **Pull request descriptions** include a "Spec citations" section
   listing the REQ-IDs the PR closes or modifies. The
   `.github/PULL_REQUEST_TEMPLATE.md` ships with this section.
2. **Commit messages** for spec-touching commits cite REQ-ID(s) or
   ADR-N(s). Example: `Add hashOfCorrectedInvoice to SendInvoiceRequest
   (REQ-OFFLINE-003)`. Non-spec commits (refactor, internal cleanup) do
   not need citations.
3. **New public types** require either a REQ-ID (the spec defines the
   functionality) or an ADR (architecture decision being implemented).
   Public types with neither are blocked.
4. **The audit is refreshed quarterly** against `ksef-docs` HEAD.
   Refresh process:
   - `cd /workspace/ksef-sdk/ksef-docs && git pull`
   - re-run the spec-conformance sweep (see audit methodology section)
   - update REQ entries: new spec sections add new REQ-IDs; removed spec
     sections deprecate the corresponding REQ-IDs; changed spec sections
     update the existing REQ entry
   - bump the audit's "Date" header
   - commit the updated audit with message
     `chore: refresh spec audit against ksef-docs <commit>`
5. **The SDK's quarterly audit refresh** is itself tracked. If a quarter
   passes without a refresh, that's a process failure.

## What "spec-touching" means

A change is spec-touching if it modifies or adds:

- a path constant in `ApiPaths`
- a HTTP request shape (header, body field, query parameter)
- a response model or `from(*Raw)` mapper
- a session/batch/export workflow step
- a cryptographic primitive use (algorithm, key size, IV size)
- a validation rule on consumer input that mirrors a spec rule
- a QR URL or signing payload
- an error/exception that maps to a specific server response code
- a builder field that maps to a spec-defined request field

A change is **not** spec-touching if it modifies:

- internal naming, package structure, comments, formatting
- test infrastructure (mock setup, fixture builders) without changing
  what's tested
- pom.xml dependency versions (unless dependency change affects spec
  behavior)
- documentation that paraphrases existing material

## Consequences

- PR review bandwidth shifts from "did Codex find a spec violation we
  missed?" to "does this PR's spec citation match the change?". Reviewer
  can verify by clicking the REQ-ID link to the audit and reading the
  spec citation.
- New consumer-visible features always have a spec source of truth, not
  a vibe.
- The audit becomes the project's living spec map. A consumer asking
  "does this SDK support [thing X from KSeF docs]?" gets a yes/no
  grounded in REQ-IDs, not opinion.
- Cost: every spec-touching commit takes one extra minute to look up the
  REQ-ID. That cost is paid back the first time it prevents a regression.

## Rejected alternatives

1. **Trust developers to read the spec without process.** Rejected
   because eight review rounds prove this doesn't work in practice. The
   spec is too large; without forcing function, contributors skip the
   relevant section.
2. **Run Codex on every PR.** Rejected because it's the same retroactive
   pattern at higher cost. Codex finds violations after they're written;
   the cure is to write fewer violations, not detect more.
3. **Generate REQ-IDs automatically from spec.** Rejected because the
   spec is in Polish prose, not structured data. Mechanical extraction
   loses the "what does the SDK actually need to do" judgment that the
   audit captures.

## Spec citations

The spec itself doesn't mandate this discipline; this is internal SDK
process driven by the 2026-05-03 spec-conformance audit.

## Related

- ADR-019 — KOD II signing scheme (cites REQ-QR-14 through REQ-QR-18)
- ADR-020 — testkit philosophy (cites Type A migrations using
  `ksef-docs/uwierzytelnianie.md`)
- ADR-021 — public API tiers (constrains what new types can land
  without REQ-ID)
