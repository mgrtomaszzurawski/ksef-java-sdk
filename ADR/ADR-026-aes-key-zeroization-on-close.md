# ADR-026: Zeroize AES key + IV on session close

Date: 2026-05-04
Status: Accepted
Last verified: 2026-05-07

## Context

The KSeF online and batch session protocols negotiate a fresh
AES-256-CBC key + IV per session. The SDK caches that material in
memory for the session's lifetime so each `send(invoiceXml)` /
`uploadParts()` call can encrypt without re-asking the server.

CWE-316 ("Cleartext Storage of Sensitive Information in Memory") is
the relevant class of issue: when the session is closed, the cached
key and IV are no longer needed, but the `byte[]` they live in stays
referenced (and therefore non-garbage-collectable) until the holding
object itself becomes unreachable. On a JVM with a long-lived
client and many short sessions, that leaves a parade of decrypted-key
arrays on the heap, vulnerable to a heap dump or post-mortem
analysis.

## Decision

Every session-handle `close()` zeroes the AES material before
returning:

```java
@Override
public void close() {
    if (closed) {
        return;
    }
    closed = true;
    try {
        // ... protocol close ...
    } finally {
        Arrays.fill(aesKey, (byte) 0);
        Arrays.fill(initVector, (byte) 0);
    }
}
```

Applied symmetrically across:

- `KsefSession.close()` — online session AES material,
- `KsefBatchSession.close()` — batch session AES material,
- `PreparedInvoiceExport.close()` — export AES material.

`KsefClient.close()` additionally clears `publicKeyCache` (RSA public
keys for SymmetricKeyEncryption / TokenEncryption), `sessionContext`
(bearer + refresh tokens), and `lastChallengeClientIp`. The cached
public keys are not strictly secret but make heap inspection harder
when cleared.

`EncryptionMaterial` accessors (`aesKey()`, `initVector()`) return
the zeroed clones rather than throwing after close. Throwing was
considered and rejected: callers may pull material defensively for
audit logs or resumable upload state and a thrown exception would
break unrelated code paths.

## Alternatives considered

1. **Use `SecretKey` (JCE) and rely on JVM cleanup.** Java's
   `SecretKeySpec` does not zero its internal byte array on
   collection. `KeyParameter.cleanup()` (BouncyCastle) is opt-in and
   we still need the raw bytes to feed `Cipher.update()`. Standard
   library does not solve this for us.
2. **Pin material in a `MemorySegment` (Foreign Function Memory API).**
   Would let us call `MemorySegment.fill(0)` on a malloc'd region.
   Rejected: the FFM API is preview/incubator-status across the Java
   17 baseline (ADR-002). Adds a runtime dependency for marginal
   gain over `Arrays.fill`.
3. **Do nothing; trust the JVM's GC to eventually collect.** Rejected.
   Long-lived consumer applications (IDE plugins, ETL daemons) accumulate
   session-aged secrets in the heap arbitrarily long. Heap-dump-based
   forensic exposure is a real CVE class for SDKs of this kind.

## Consequences

- **`Arrays.fill` is best-effort, not a guarantee.** The JVM may
  have moved the array via a generational GC compaction before
  `close()` runs; the original bytes remain in the freed region until
  overwritten by the allocator. This is the same caveat that applies
  to every Java SDK doing best-effort secret zeroization — the
  alternative ("don't try") is strictly worse.
- **`close()` must be called.** Try-with-resources is the contract;
  consumers who forget pay full CWE-316 cost. The SDK's own session
  abstractions implement `AutoCloseable` so `try (... = client.openSession())`
  is the canonical usage pattern.
- **Tests assert the invariant.** `LifecycleZeroizationTest` uses
  reflection to read the underlying `byte[]` after `close()` and
  asserts every byte is zero. Regression-protected.
- **The same pattern must extend to any future session type.** When
  ADR-XXX adds a new session abstraction with cached AES material,
  `close()` MUST zeroize. A new public type with cached symmetric key
  material that does NOT implement this pattern is a release blocker.

## Where it lives

- `domain/invoicing/KsefSession.close()`
- `domain/invoicing/KsefBatchSession.close()`
- `domain/invoicing/PreparedInvoiceExport.close()`
- `KsefClient.close()` — cache + token + last-challenge IP clear
- `api/LifecycleZeroizationTest.java` — reflection-based assertion gate
