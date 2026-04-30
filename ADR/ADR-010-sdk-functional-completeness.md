# ADR-010: SDK functional completeness — identifier types, auto-refresh, batch automation

**Date:** 2026-04-21
**Status:** Accepted
**Extends:** ADR-008 (API redesign)

## Context

After Phase 8 redesign (ADR-008), a functional audit revealed three gaps where the SDK
forced consumers to handle complexity that the SDK had everything needed to handle itself:

1. **Identifier types — NIP-only.** KSeF API supports four context identifier types
   (`NIP`, `INTERNAL_ID`, `NIP_VAT_UE`, `PEPPOL_ID`), but `KsefCredentials` exposed only
   `String nip()`. Consumers using non-NIP identifiers (e.g. EU VAT UE, Peppol IDs) had
   no way to authenticate through the SDK.

2. **Token expiry — manual recovery.** When the JWT expired mid-operation, the SDK threw
   `KsefSessionExpiredException` and the consumer had to catch it, call
   `client.authenticate()`, and retry. ADR-008 established the lazy-auth pattern
   ("if auth is needed, auth happens automatically") for first-use, but didn't extend it
   to recovery from mid-session expiry.

3. **Batch session — 80% manual.** `KsefClient.openBatchSession(FormCode, BatchFileSpec)`
   required the consumer to: build the ZIP from invoices, encrypt it with the session AES
   key, split into ≤100 MB parts, compute SHA-256 hashes, and construct `BatchFileSpec`.
   The SDK had everything needed (AES key, crypto, hash) but pushed the work to the consumer.

The user audit observation was direct: "to jest SDK więc nie mogę robić gapów w
funkcjonalności nawet z której nie korzystam" ("this is an SDK so I can't have functional
gaps even in features I don't use myself").

## Decision

### 1. KsefIdentifier — sealed identifier system

Introduce `KsefIdentifier` as a typed identifier with four supported types:

```java
public record KsefIdentifier(Type type, String value) {
    public enum Type { NIP, INTERNAL_ID, NIP_VAT_UE, PEPPOL_ID }

    public static KsefIdentifier nip(String nip);          // 10-digit validation
    public static KsefIdentifier internalId(String id);    // non-blank
    public static KsefIdentifier nipVatUe(String id);      // non-blank
    public static KsefIdentifier peppolId(String id);      // non-blank
}
```

`KsefCredentials.identifier()` is the canonical accessor. The legacy `String nip()` is
deprecated but kept as a default method that returns `identifier().value()` for NIP type
or throws `IllegalStateException` for non-NIP types. All three credential implementations
(`KsefTokenCredentials`, `KsefCertificateCredentials`, `KsefPkcs12Credentials`) gain
identifier-based constructors while keeping the original `(token, String nip)` signature
for backward compatibility (delegates to `KsefIdentifier.nip(nip)` internally).

`AuthClient` was updated to thread the identifier through both XAdES and token flows,
mapping `KsefIdentifier.Type` to the corresponding `AuthenticationContextIdentifierTypeRaw`
enum value.

### 2. Auto-recovery on 401 — extending lazy auth

`HttpSupport`'s authenticated methods (`getAuthenticated`, `postJsonAuthenticated`, etc.)
now retry exactly once on HTTP 401:

1. Send the request with the current JWT
2. If response is 401: call `KsefClient.reauthenticate()` (resets `authenticated` flag,
   clears public key cache and JWT, re-runs full authenticate flow)
3. Rebuild the request with the fresh token from `SessionContext`
4. Send again
5. If second attempt also returns 401, propagate the original `KsefAuthException`

The reauth call is wrapped in `tryReauthenticate()` that swallows reauth failures so
the consumer sees the original 401 (more diagnostic value than "reauth failed because X").

This is a strict generalization of ADR-008's lazy auth: "if auth is needed, auth happens
automatically" now applies to mid-session expiry as well as first-use. The retry budget
is exactly one — runaway loops are impossible.

Non-authenticated methods (`get`, `postJson`, etc.) are unchanged. The pre-existing
`RetryHandler` (for 429 / 5xx / network errors) is independent and unchanged.

### 3. Automatic batch packaging — SDK does the work

New overload:

```java
public synchronized KsefBatchSession openBatchSession(FormCode formCode, List<byte[]> invoiceXmls)
```

Internally:
1. Generate AES key + IV (session encryption)
2. Encrypt AES key with KSeF public key (RSA-OAEP)
3. Build a ZIP package: each invoice as `<sha256-base64url>.xml` entry
4. Encrypt the entire ZIP with AES-256-CBC
5. Compute SHA-256 of the encrypted ZIP
6. Split encrypted bytes into parts of ≤100 MB each
7. Compute SHA-256 of each part
8. Build `BatchFileSpec` with all metadata
9. Open batch session via `SessionClient.openBatch()`
10. Wrap result in `KsefBatchSession` carrying the encrypted part bytes

`KsefBatchSession.uploadParts()` PUTs each encrypted part to its corresponding
`PartUploadRequest.url` with the server-supplied headers. The consumer's full flow:

```java
try (KsefBatchSession batch = client.openBatchSession(FormCode.FA2, invoiceXmls)) {
    batch.uploadParts();
    batch.close();  // SDK polls until KSeF finishes processing
}
```

The original `openBatchSession(FormCode, BatchFileSpec)` overload is preserved for
advanced use cases where the consumer has a pre-built encrypted ZIP (e.g. constructed
by a separate ETL system).

The `BatchPackageBuilder` helper is package-private — it's an internal building block
of the automated overload, not a public API.

## Consequences

### Positive
- Consumer code for batch send is now `~5 lines`, matching the simplicity of online send
- `KsefSessionExpiredException` no longer surfaces to consumers in the common case;
  long-running backends don't need explicit refresh logic
- PEPPOL_ID and EU VAT UE consumers can now use the SDK
- All four SDK self-imposed contracts of "no manual crypto", "no manual polling",
  "no Raw types", and "no manual auth lifecycle" now hold for batch sessions too

### Negative / trade-offs
- `KsefCredentials.nip()` becoming `@Deprecated` is a soft warning; consumers using it
  will see a deprecation notice but the API still works
- The auto-refresh adds one HTTP round-trip cost on token expiry. Token TTL is server-controlled,
  so we can't avoid this — but it's better than failing the operation
- `BatchPackageBuilder` is internal but tested directly (package-private test). Future
  changes to KSeF's batch ZIP layout would only require updating this one class

### Defaults preserved
- The advanced `openBatchSession(FormCode, BatchFileSpec)` overload is kept for power users
- The single-attempt 401 retry policy is fixed (not configurable). If consumers need
  different behavior they can wrap in their own retry logic
- Backward-compatible constructors `(token, String nip)` ensure existing code keeps working

## Reference
- PR #17 — implementation
- ADR-008 — original session abstraction, credentials, lazy auth
- ADR-005 — SDK overlay rule that "Raw types are internal" (now extended to identifier enums)
