# Known KSeF server behaviours

This document lists KSeF server behaviours that consumers will hit at
runtime. They are **not SDK bugs** and are not always reflected in the
upstream `ksef-docs` repository. The SDK either handles them transparently
(where noted) or surfaces them as typed exceptions so consumers can react.

Last verified against `api-demo.ksef.mf.gov.pl` and `api-test.ksef.mf.gov.pl`
on 2026-05-07.

## Authentication

### Authentication is asynchronous (HTTP 450 transient)

After `POST /auth/xades-signature` or `POST /auth/ksef-token`, calling
`POST /auth/token/redeem` returns HTTP 450 until the server finishes
challenge validation (~1–4 s typical). The SDK polls `GET /auth/{ref}`
internally before redeem; consumers see only the final outcome.

### XAdES terminate requires prior token redeem

Calling `terminateAuth()` immediately after async XAdES authentication
(without a successful `redeemTokens` between) returns HTTP 500. The SDK
tracks state in `SessionContext` and gates the call.

### Authentication session timeout

Access tokens expire on the server side. The SDK transparently
re-authenticates on HTTP 401 — consumers do not need to wrap calls in
`try/catch` for token refresh.

## Sessions

### Online session cooldown after termination (~30–60 s)

Opening a new online session for the same NIP within ~30 seconds of a
previous session's termination returns a session that *opens* with a
reference number but enters status 415 immediately and rejects invoice
submissions. The SDK surfaces this via `KsefSessionCooldownException`
on `openSession(...)` for fast-fail clarity. Auto-retry with backoff is
left to the consumer's policy.

### Status 415 is the normal "open" state during processing

After `close()`, the server returns status 415 while batch validation is
still in flight. This is not a failure; the SDK polls until terminal
inside `KsefSession.close()` / `KsefBatchSession.close()`.

### Status 440 = session cancelled by server

Empty sessions left open past the inactivity threshold are cancelled with
status 440 ("Sesja anulowana"). The SDK surfaces this as
`KsefSessionTerminalFailureException` on `close()`.

## Invoices

### Form code is environment-gated

`FormCode.FA2` is only accepted on TEST. DEMO and PROD reject it at
session-open time. The SDK preflights this client-side via
`FormCode.assertAllowedOn(KsefEnvironment)` so misconfigured consumers
fail fast with a clear message instead of seeing a server-side schema
rejection on the first invoice.

### KSeF number validation is strict

35 characters; uppercase hex segments; `uuuuMMdd` date with the actual
issue date; CRC-8 checksum (polynomial `0x07`, init `0x00`). Server
rejects mismatches with HTTP 400. The SDK's `KsefNumber` value object
applies the same rules client-side so malformed numbers fail before they
hit the wire.

### Retention expiry returns HTTP 410 Gone

Per KSeF retention policy, invoice content older than the retention
window is removed. The server returns HTTP 410 Gone on
`getByKsefNumber(...)` for retired invoices. The SDK surfaces this as
`KsefRetentionExpiredException` (subtype of `KsefNotFoundException`),
so existing `catch (KsefNotFoundException ...)` blocks still handle it.

## Certificates

### Certificate operations require certificate-based authentication

Token-based authentication cannot reach the certificate enrollment
endpoints. Consumers must authenticate with `KsefCertificateCredentials`
or `KsefPkcs12Credentials` to use cert-domain APIs. The SDK logs a WARN
when `enroll()` / `getEnrollmentData()` is invoked on a token session;
the server-side typed error remains the authoritative outcome.

### Certificate serial number is populated asynchronously

`enroll(...)` accepts the CSR and returns a reference number; the issued
certificate's serial number is populated asynchronously. Consumers must
poll `getEnrollmentStatus(ref)` until terminal, or use the `*AndAwait`
helpers shipped in 1.0.

### `certificates/enrollments` crashes on invalid `certificateType`

The server returns HTTP 500 (not a typed validation error) when
`certificateType` is outside the documented enum. The SDK validates the
enum client-side so this only fires for callers bypassing the typed
builder.

### Monthly certificate quota: 12 enrollments / 6 active

Per taxpayer NIP. The `ksef-demo` runner skips enroll/revoke by default
to preserve the quota; opt in with `-Ddemo.cert.test=true`.

## Permissions

### `subjectDetails` is required despite spec marking it optional

`POST /permissions/persons/grants` rejects requests without
`subjectDetails` even though the OpenAPI schema marks the field
optional. The SDK populates it automatically from the subject
identifier; external callers using the raw types must include it.

### `pageSize` is not validated on permission query endpoints

The server accepts arbitrary `pageSize` values (including 0 or negative)
without an error response. The SDK clamps to documented bounds in the
typed builders.

## Validation errors

### `exceptionCode 21405` — per-field validation

Returned with dot-notation paths (e.g. `filters.dateRange.from`).
Server validates all fields in one pass and returns every error at
once. The SDK maps these to `KsefValidationException`.

### `exceptionCode 21001` — JSON parsing

Returned for type mismatches, invalid enum values, malformed date
strings. Maps to `KsefValidationException`.

### Unknown fields are silently ignored

The server does not reject requests carrying fields outside the
schema. Consumers using the raw types should treat this as advisory
rather than relying on it.

## Batch sessions

### `subjectDetails` ignored when grouping by entity

For batch packages with mixed entity references, the server uses the
first valid `subjectDetails` it finds and ignores the rest. The SDK
surfaces this as advisory; the documented contract is that all parts
of a batch belong to the same NIP.

### Empty batches reject with `exceptionCode 21205`

`close()` on a batch session that uploaded zero invoice parts returns
"package must not be empty". Use `BatchSessionRunner` only when at least
one part is uploaded.

### Aggregate caps: 50 parts, 5 GB pre-encryption

Enforced at session open, not per-part. The SDK validates this client-side
in `PreparedBatchPackage` (`KsefLimits.MAX_BATCH_PARTS`,
`KsefLimits.MAX_BATCH_TOTAL_BYTES`) for fast-fail clarity.

## Rate limits

Rate limits exist per environment and per taxpayer; the SDK surfaces 429
responses as `KsefRateLimitException`. The current limits are returned
by `client.rateLimits().getRateLimits()`.

## Where things live in the SDK

| Server behaviour | SDK handling | Typed exception |
|---|---|---|
| 401 token expired | Auto re-auth | – |
| 415 cooldown / 440 cancelled / non-200 terminal | Polling until terminal | `KsefSessionCooldownException`, `KsefSessionTerminalFailureException` |
| 410 retention | Surfaced typed | `KsefRetentionExpiredException` |
| 429 rate limit | Surfaced typed | `KsefRateLimitException` |
| 21405 / 21001 validation | Mapped | `KsefValidationException` |
| 5xx server error | Surfaced typed | `KsefServerException` |
| Network / TLS | Surfaced typed | `KsefNetworkException` |

For implementation context see the relevant ADRs:

- [ADR-008](ADR/ADR-008-api-redesign-session-abstraction.md) — session
  abstractions
- [ADR-011](ADR/ADR-011-batch-encryption-and-polling-semantics.md) —
  batch encryption + polling
- [ADR-015](ADR/ADR-015-trust-the-spec-on-required-fields.md) —
  carve-out policy when the server diverges from spec
