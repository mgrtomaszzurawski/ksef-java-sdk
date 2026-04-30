# ADR-011: Batch session encryption algorithm + session polling semantics

**Date:** 2026-04-30
**Status:** Accepted
**Trigger:** Live KSeF demo run (PR #21) revealed two architectural misunderstandings.

## Context

Both decisions were initially implemented incorrectly because the relevant KSeF protocol
details were inferred from spec snippets rather than verified against the live backend.
WireMock unit tests passed because they only validate HTTP request shape, not the
semantic behavior of decryption + reassembly + state machines on the server side.
Live demo run exposed both errors.

## Decision 1 — Batch session: split-then-encrypt, not encrypt-then-split

### What we got wrong

The initial `BatchPackageBuilder` (PR #17) implemented:
1. Build full ZIP from invoices
2. Encrypt the entire ZIP with AES-256-CBC + PKCS#7 (one `Cipher.doFinal()` call)
3. Split the resulting ciphertext into parts of `maxPartSize` bytes
4. Upload parts to KSeF; KSeF was expected to concatenate the ciphertext parts and
   decrypt as a single block

This is a natural assumption — encryption is conceptually "wrap the whole thing", and
splitting after produces parts that re-concatenate byte-for-byte to the original
ciphertext. Standard pattern for "encrypted and chunked".

KSeF returned `Błąd dekompresji pierwotnego archiwum` (decompression error) for every
batch upload because this pattern does NOT match the protocol.

### What KSeF requires

Per `sesja-wsadowa.md` and the export endpoint description in `open-api.json`:

> "Paczka faktur jest dzielona na części o maksymalnym rozmiarze [N]MB. Każda część
> jest zaszyfrowana algorytmem AES-256-CBC z dopełnieniem PKCS#7."

Translation: "The package is divided into parts of max [N]MB. **Each part is encrypted
with AES-256-CBC + PKCS#7 padding**."

The split happens on the **plaintext ZIP**, not the ciphertext. Each part is then
encrypted **independently** with a fresh `Cipher.doFinal()` — meaning each part has
its own PKCS#7 padding and its own ciphertext output (~16 byte overhead per part).

KSeF reassembly:
1. Download each part
2. Decrypt each part separately (each `Cipher.doFinal()` produces plaintext slice)
3. Concatenate decrypted plaintext slices → original ZIP
4. Verify total `fileSize` and `fileHash` against reassembled ZIP
5. Unzip → invoices

### `BatchFileSpec` semantics correction

The spec object passed in the open-batch-session request describes:
- `fileSize` / `fileHash` — describe the **unencrypted** ZIP. KSeF verifies these
  after reassembling decrypted parts. Initially the SDK reported the encrypted total
  size — wrong.
- `parts[i].fileSize` / `parts[i].fileHash` — describe the **encrypted** part bytes
  (so KSeF can verify upload integrity before decryption). This was always correct.

### The 100 MB limit

Spec says: "Maksymalny dozwolony rozmiar części **przed zaszyfrowaniem** to 100MB."
The 100 MB limit applies to the plaintext chunk size. The encrypted part is a few
bytes larger (PKCS#7 padding rounds up to next 16-byte block).

### Fix in PR #21

`BatchPackageBuilder`:
1. Build ZIP to temp file (unchanged)
2. Read the ZIP in chunks of `maxPartSize` plaintext bytes
3. Encrypt each chunk separately via `CryptoService.encryptAes()` — fresh `doFinal()`
4. Write encrypted chunk to a part temp file with per-part SHA-256
5. Maintain running SHA-256 of the unencrypted ZIP for the spec's `fileHash`
6. Spec carries plaintext total size + hash + per-part encrypted metadata

### Why this matters going forward

This is the kind of protocol detail that's easy to revert if a future contributor
"refactors" the code without understanding why each chunk has its own `doFinal()`.
The natural simplification ("just do one encrypt, split after") will reintroduce the
bug. The class javadoc and this ADR document the requirement.

---

## Decision 2 — Session polling terminates on any terminal status, not only success

### What we got wrong

`KsefSession.pollUntilComplete()` (called from `close()`) and the equivalent in
`KsefBatchSession` originally polled until status code == 200 (success):

```java
if (code != null && code == STATUS_CODE_OK) {
    return;
}
```

If the session never reaches 200 (e.g. all invoices fail validation → terminal code
445 "Błąd weryfikacji, brak poprawnych faktur"), polling ran the entire timeout
budget and then logged "polling timed out — UPO may not be available yet". The
consumer never learned that the session had reached a definitive failure state
several seconds in — the SDK kept hoping for success that would never come.

### What KSeF actually does

KSeF session status codes (from `open-api.json` and live observation):

| Code | Meaning | Type |
|------|---------|------|
| 100 | Sesja otwarta (open) | Intermediate |
| 170 | Sesja zamknięta, w trakcie przetwarzania (closing) | Intermediate |
| 200 | Sesja zakończona pomyślnie (success) | Terminal (success) |
| 415 | Błąd odszyfrowania klucza (key decryption error) | Terminal (failure) |
| 430 | Błąd dekompresji pierwotnego archiwum (batch decompression error) | Terminal (failure) |
| 440 | Sesja anulowana, brak faktur (cancelled, no invoices) | Terminal (failure) |
| 445 | Błąd weryfikacji, brak poprawnych faktur (validation failure) | Terminal (failure) |

Pattern: codes < 200 are intermediate; codes ≥ 200 are terminal (success or failure).
The session machine never goes backwards from a terminal state.

### Decision

Polling stops when the session reaches **any terminal state** (code ≥ 200):

```java
if (code != null && code >= STATUS_CODE_OK) {
    if (code == STATUS_CODE_OK) {
        LOG.info("Session {} processing complete", referenceNumber);
    } else {
        LOG.warn("Session {} reached terminal failure state — code={} description={}", ...);
    }
    return;
}
```

Logging differentiates success from failure with the description, so consumers can
diagnose. The `close()` method itself does not throw on terminal failure — the
session was successfully closed at the protocol level; the failure is invoice-level.
Consumers query `session.failedInvoices()` to see what went wrong.

### Why this matters

- Failed sessions return immediately instead of after a 5-minute timeout
- Logs surface the actual failure reason instead of a vague timeout
- The sample app's "FAIL summary" now reflects real KSeF failures, not SDK timeouts
- The polling budget (`STATUS_POLL_MAX_ATTEMPTS`) is now a true safety net for
  genuinely stalled sessions, not a deadline for "hopeful success"

## Consequences

- **Breaking-ish:** The batch fix changes what `BatchFileSpec.fileSize` /
  `BatchFileSpec.fileHash` mean. Any consumer that was building these manually
  (rather than going through `openBatchSession(FormCode, List<byte[]>)`) needs to
  switch from "encrypted total" to "plaintext total". Public API surface is
  unchanged; only the contract documentation changed.
- **No breaking change for polling:** `close()` still returns void without throw on
  failure; consumer behavior is unchanged. Only the timing changed (faster failure)
  and the logged messages.
- **Live testing remains the gate:** Both bugs were undetectable via WireMock or
  unit tests. Per ADR-009, demo-app runs against the live MoF backend are the
  validation gate. We added the missing runners and reran (PR #20, #21).

## Reference

- PR #21 — implementation
- ADR-008 — original session abstraction (introduced KsefBatchSession)
- ADR-009 — demo-app as live validation gate (the reason these bugs were caught)
- KSeF docs: https://github.com/CIRFMF/ksef-docs/blob/main/sesja-wsadowa.md
- `open-api.json` schema `BatchFileInfo` — fileSize / fileHash / fileParts contract
