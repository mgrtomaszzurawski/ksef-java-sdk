# ADR-001: Generate from specs, not wrap official SDK

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

The official KSeF Java SDK (CIRFMF/ksef-client-java) exists and provides:
- 276 hand-written model classes (NOT generated from the OpenAPI spec that CIRFMF themselves publish)
- A single `DefaultKsefClient` class with ~100 methods
- Crypto and signing services (~450 LOC of real value)
- No retry, no pagination abstraction, no typed exception hierarchy

A community fork (alapierre/ksef-client-java-mf-fork) fixes bugs and updates dependencies but preserves the same architecture.

Meanwhile, CIRFMF publishes a complete OpenAPI 3.0.4 spec (73 endpoints, 283 schemas) and XSD schemas in the ksef-docs repository.

## Decision

Build our own SDK by generating from the official specs (OpenAPI + XSD), not wrapping the official SDK.

## Consequences

### Positive
- Models are always synchronized with the spec — no drift from hand-written POJOs
- Freedom to design the SDK overlay (clients, builders, retry, exceptions) without inheriting bad abstractions
- Can publish to Maven Central without GitHub Packages authentication hassle
- Spec updates can be automated: pull new spec → regenerate → run tests → release

### Negative
- Must reimplement crypto (~300 LOC) and signing (~100 LOC) services
- No community around this specific SDK initially
- Must validate generated models against live KSeF test environment ourselves

### Risks
- The OpenAPI spec published by CIRFMF might have errors or be incomplete — must verify against TE
- Spec might diverge from actual API behavior (documentation bugs)
