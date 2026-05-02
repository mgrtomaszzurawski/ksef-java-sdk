# ADR-006: Separate SDK library and sample app

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

Same rationale as NoviCloud ADR-001. SDK library must have clean dependency footprint. Demo/sample code must not pollute the published artifact.

## Decision

Two Maven modules:
- `ksef-client` — published SDK jar. No application dependencies.
- `ksef-sample` — demo app with examples. `maven.deploy.skip=true`. Can have Spring Boot or other app dependencies.

## Consequences

- SDK jar has minimal transitive dependencies
- Sample app demonstrates real usage patterns against KSeF TE
- Sample app can be used for manual integration testing with test certificates

---

## Amendment 2026-05-01 — module renamed `ksef-sample` → `ksef-demo`

The "sample-app" terminology used throughout this ADR was renamed to
`ksef-demo` to align with the official CIRFMF/ksef-client-java SDK
naming convention and with the internal `Demo*` class vocabulary
already in use (`DemoApp`, `DemoContext`, `DemoSession`, etc.). The
decision and module split documented above remain unchanged — only
the artifactId moved. See finding #11 in
`context/USER-REVIEW-REMARKS-2026-05-01-2130.md` for full reasoning.
