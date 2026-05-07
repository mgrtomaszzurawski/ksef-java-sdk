# ADR-008: API redesign — session abstraction, credentials, lazy auth

**Date:** 2026-04-18
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

Phase 7 delivered full endpoint coverage (10 domain clients, 81 records, 6 builders) but the public API
still leaks implementation complexity to consumers:

1. **Raw types in public signatures** — `SessionClient.openOnline(OpenOnlineSessionRequestRaw)`,
   `SessionClient.sendInvoice(ref, SendInvoiceRequestRaw)`. Violates ADR-005 promise.
2. **Manual crypto** — consumer must call `CryptoService.generateAesKey()`, `encryptAes()`,
   `MessageDigest.getInstance("SHA-256")` directly. Cryptographic plumbing is not SDK business logic.
3. **No session object** — consumer threads `referenceNumber + aesKey + initVector` between calls
   via their own state management (e.g. `DemoContext` in ksef-sample).
4. **Polling copy-pasted** — auth status, session status, close-with-415-retry all re-implemented
   in consumer code (`SessionRunner`: 230 lines of ceremony for one invoice).
5. **KsefOperations too thin** — only covers token auth + single FA(2) invoice. Hardcoded polling
   constants, no multi-invoice support, no configurable form code.

The SDK is at "we can use endpoints" level but not at "we sensibly operate endpoints, taking away
from the user management of mechanisms like tokens, encryption, etc."

## Decision

### 1. KsefCredentials — sealed interface with three implementations

```java
public sealed interface KsefCredentials
    permits KsefTokenCredentials, KsefCertificateCredentials, KsefPkcs12Credentials {
    String nip();
}
```

- `KsefTokenCredentials(String ksefToken, String nip)` — for KSeF token auth
- `KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, String nip)` — for XAdES
- `KsefPkcs12Credentials(Path keystorePath, char[] password, String nip)` — convenience for PKCS#12 files

Credentials are passed at `KsefClient.builder().credentials(creds)` construction time.
`authenticate()` dispatches on the sealed type via `switch` — token flow vs XAdES signing flow.

### 2. KsefSession — AutoCloseable session scope

New class that owns session crypto state and provides clean invoice operations:

- Created via `client.openSession(FormCode)` — handles key generation, encryption, session open
- `send(byte[] invoiceXml)` — encrypts with session AES key, computes hashes, sends
- `close()` — handles 415 "session busy" retry, polls until processing complete
- AutoCloseable — `close()` called automatically in try-with-resources

Consumer never touches `CryptoService`, `SendInvoiceBuilder`, `OnlineSessionBuilder`, AES keys,
IVs, or hashes. One active session per client (KSeF constraint: one online session per NIP).

### 3. Lazy authentication

`authenticate()` is called automatically on first operation that needs auth (e.g. `openSession()`).
Handles the full flow internally: challenge → encrypt/sign → poll status → redeem tokens.
Can also be called explicitly for early validation.

If auth fails, exception propagates with clear message to the programmer.

### 4. FormCode enum

Replaces string triplets (`systemCode`, `schemaVersion`, `value`) with a typed enum:
`FA2, FA3, PEF3, PEF_KOR3, FA_RR_1_0E, FA_RR_1_1E, RR_1_0E, RR_1_1E` plus `custom()` factory.

### 5. Internal vs public API boundary

**Public (exported):**
- `KsefClient`, `KsefSession`, `KsefCredentials` + implementations, `FormCode`
- `KsefEnvironment`, `RetryPolicy`
- Domain clients for non-session operations: `InvoiceClient`, `PermissionClient`, `TokenClient`,
  `CertificateClient`, `LimitsClient`, `RateLimitClient`, `TestDataClient`
- `sdk.model` — 81 immutable records
- `sdk.model.builder` — request builders (no Raw types)
- `sdk.exception` — exception hierarchy

**Internal (not exported):**
- `AuthClient`, `SessionClient`, `SecurityClient` — used by KsefClient/KsefSession internally
- `HttpSupport`, `SessionContext` — plumbing
- `CryptoService`, `SigningService`, `CertificateLoader` — crypto
- `OnlineSessionBuilder`, `SendInvoiceBuilder` — used by KsefSession internally
- `client.model.*Raw` — generated types (already not exported)

### 6. No raw escape hatch

If someone wants raw HTTP/JSON access, they use the official CIRFMF SDK. This SDK provides a
clean, opinionated API. No `client.raw()` accessor or similar backdoor.

### 7. KsefOperations removed

Its functionality is absorbed: `authenticateWithToken()` → `KsefClient.authenticate()`,
`sendInvoice()` → `KsefSession.send()`, key caching → internal to KsefClient.

## Consequences

- Consumer code for "send invoice" goes from ~50 lines (manual crypto + polling) to ~5 lines
- Breaking change for anyone using `KsefOperations`, `SessionClient.openOnline()`, or
  `AuthClient` directly — acceptable since SDK is pre-1.0
- Domain clients for admin operations (permissions, tokens, etc.) stay public but method
  signatures updated to use builders instead of Raw types where builders exist
- Batch sessions (`SessionClient.openBatch()`) not exposed in this redesign — different
  mechanism (ZIP upload), rarely used. Can be added later as `KsefBatchSession`.

## Notes

KSeF auth is multi-step challenge-response (not transparent Basic Auth), and sessions
hold crypto state. Hence explicit `authenticate()` (lazy) + `KsefSession` (scoped) instead
of a transparent-auth builder.
