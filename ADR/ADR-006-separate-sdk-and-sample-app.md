# ADR-006: Separate SDK library and sample app

**Date:** 2026-04-03
**Status:** Accepted

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
