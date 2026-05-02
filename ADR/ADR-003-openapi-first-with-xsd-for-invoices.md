# ADR-003: OpenAPI-first for REST, XSD/JAXB for invoice XML

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

KSeF has two distinct data formats:
1. **REST API** (JSON) — session management, auth, permissions, queries. Described by OpenAPI 3.0.4 spec.
2. **Invoice documents** (XML) — actual invoice content sent within sessions. Described by XSD schemas (FA(2), FA(3), PEF, RR).

These are orthogonal: REST models describe API request/response envelopes, XSD models describe invoice business content.

## Decision

Use two code generators:
- `openapi-generator-maven-plugin` for REST API models (JSON ↔ Java)
- `maven-jaxb2-plugin` (or XJC task) for invoice XML models (XML ↔ Java)

Both run at build time. Neither output is hand-edited.

## Consequences

- REST models regenerated from `open-api.json` when API version changes
- Invoice models regenerated from XSD when schema version changes
- Two separate `target/generated-sources/` directories (openapi + xjc)
- SDK overlay can wrap both, or expose JAXB models directly for invoice building
- Build requires both generators configured in pom.xml
