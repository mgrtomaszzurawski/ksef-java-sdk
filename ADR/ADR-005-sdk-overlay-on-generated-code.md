# ADR-005: SDK overlay on generated code

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-07

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
