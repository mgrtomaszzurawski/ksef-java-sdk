# ADR-009: Demo-app purpose shift — from API exploration to SDK test harness

**Date:** 2026-04-18
**Status:** Accepted
**Last verified:** 2026-05-07

## Context

The `ksef-sample` module (demo-app) was created in Phase 6 as an **API exploration tool**.
Each runner exercised KSeF REST endpoints directly, managing all mechanisms manually:
challenge-response authentication, AES key generation, invoice encryption, SHA-256 hashing,
polling loops with backoff, 415 session-busy retry, stale session cleanup, re-authentication.

This was the right approach during Phases 3-7 — the SDK didn't have high-level abstractions
yet, and we needed to validate each endpoint's behavior against the live test environment.
The demo-app was effectively a REST API test harness with the SDK as a thin HTTP wrapper.

After Phase 8 (ADR-008), the SDK provides `KsefSession`, `KsefCredentials`, `FormCode`,
and 25 validated builders. Consumers never touch `CryptoService`, `AuthClient`, or `*Raw`
types. The mechanisms that runners managed manually are now internal to the SDK.

## Decision

The demo-app shifts purpose from **REST API exploration** to **SDK public API test harness
and usage demonstration**.

### Before (Phase 6, PR #8)

Runners used internal SDK classes directly:

```java
// SessionRunner — 230 lines, managed everything manually
byte[] aesKey = CryptoService.generateAesKey();
byte[] iv = CryptoService.generateIv();
byte[] encKey = CryptoService.encryptWithPublicKey(aesKey, context.ksefPublicKey());
OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw()
    .formCode(new FormCodeRaw().systemCode("FA").schemaVersion("2").value("FA (2)"))
    .encryption(new EncryptionInfoRaw().encryptedSymmetricKey(encKey).initializationVector(iv));
OnlineSession session = client.sessions().openOnline(request);
// ... manual encrypt, hash, send, poll, close retry, UPO retrieval ...
```

Purpose: validate that each KSeF endpoint works as documented.

### After (Phase 8, PR #11-#13)

Runners use the SDK public API exclusively:

```java
// SessionRunner — 8 separately reported operations through public API
KsefSession session = client.openSession(FormCode.FA2);  // report: openSession
session.send(invoiceXml);                                  // report: send
session.invoiceStatus(invoiceRef);                         // report: invoiceStatus
session.status();                                          // report: status
session.invoices();                                        // report: invoices
session.failedInvoices();                                  // report: failedInvoices
session.close();                                           // report: close
session.upo(invoiceRef);                                   // report: upo
```

Purpose: validate that the SDK works end-to-end against the live server, and serve as
a usage example for SDK consumers.

### Key design rules

1. **Runners use only public API** — `KsefClient`, `KsefSession`, `KsefCredentials`,
   `FormCode`, domain clients (`invoices()`, `tokens()`, `permissions()`, etc.),
   and builders. No internal classes (`AuthClient`, `SessionClient`, `CryptoService`).

2. **Per-operation reporting preserved** — each SDK operation is called and reported
   separately. If send fails but open succeeded, the report shows exactly that.

3. **Same error visibility** — when an SDK operation fails, the exception message tells
   the consumer what went wrong. Runners catch and report per-operation, same as before.

4. **Authentication tested through lifecycle** — AuthRunner tests authenticate, terminate,
   re-authenticate. This validates the full session lifecycle without accessing internals.

## Consequences

- Demo-app is now a faithful representation of how a real consumer uses the SDK
- If demo-app works against the live server, a consumer's code will work too
- We can no longer test individual sub-steps of multi-step flows (e.g., cannot separately
  test challenge request vs token encryption vs status polling — `authenticate()` is one call)
- If `authenticate()` fails, the exception must be clear enough to diagnose the sub-step.
  This puts pressure on SDK exception messages to be specific (validated in Phase 7).
- CertProbe and ValidationProbe remain as standalone utilities for deep endpoint
  exploration when needed — they use `client.auth()` and `client.sessions()` directly
  (these accessors are public but documented as internal).

---

## Amendment 2026-05-01 — module renamed `ksef-sample` → `ksef-demo`

The body above refers to the module as `ksef-sample`; that artifactId
was renamed to `ksef-demo` to match the official CIRFMF/ksef-client-java
SDK naming and to align with the internal `Demo*` class vocabulary
(`DemoApp`, `DemoContext`, `DemoSession`, `DemoState`, `DemoMode`).
Purpose-shift decision unchanged — the rename only completes the
naming consistency.

> **Amendment (2026-05-07):** the `client.auth()` and `client.sessions()`
> accessors mentioned above were removed before 1.0.0. ADR-016 supersedes
> this ADR's assumption that those accessors remain public.
