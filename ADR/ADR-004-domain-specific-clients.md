# ADR-004: Domain-specific clients, not a god-class

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

The official SDK puts all 73 operations into one `DefaultKsefClient` class. This makes it hard to discover operations, test individual domains, and reason about session state.

The KSeF API has 10 clear domains: auth, sessions, invoices, permissions, tokens, certificates, limits, rate-limits, security, testdata.

## Decision

One client class per API domain. A top-level `KsefClient` (or `KsefSdk`) provides access to domain clients.

```java
KsefClient ksef = KsefClient.builder()...build();
ksef.auth().challenge(nip);
ksef.sessions().openOnline(request);
ksef.invoices().queryMetadata(filters);
ksef.permissions().grantPerson(request);
```

## Consequences

- Each domain client is independently testable
- Discoverability via IDE autocomplete on domain accessors
- Session token management is cross-cutting — must be shared across domain clients (injected, not duplicated)
- Some domains may be very small (rate-limits: 1 operation, security: 1 operation) — acceptable, consistency over size optimization

## Open questions

- Should `testdata` domain be included in the main SDK or as a separate test-support module?
- How to handle session state sharing between domain clients (shared context object? token holder?)
