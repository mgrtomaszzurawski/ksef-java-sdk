# ADR-002: Java 17 baseline

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

Need to choose minimum Java version. Options considered:
- Java 11: Maximum compatibility. Official SDK targets 11.
- Java 17: Records, sealed classes, text blocks, strong LTS adoption.
- Java 21: Virtual threads, pattern matching. Newest LTS but lower enterprise adoption.

## Decision

Target Java 17 as minimum.

## Consequences

- Can use records for immutable model types (critical for SDK public API)
- Can use sealed classes for exception hierarchy
- Can use text blocks in tests
- Wide enterprise adoption (Java 17 LTS since Sep 2021)
- Cannot use virtual threads (Java 21+) — not needed for HTTP client SDK
