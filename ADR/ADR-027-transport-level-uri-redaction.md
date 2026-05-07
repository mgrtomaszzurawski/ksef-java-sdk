# ADR-027: Transport-level URI redaction and HTTPS-only presigned URLs

Date: 2026-05-04
Status: Accepted

## Context

Two classes of sensitive data leak through transport-level logging
and exception messages:

1. **Presigned URL query strings** carry short-lived authorization
   tokens (`?sig=...`, `?skoid=...`, `?skt=...`, `?se=...`,
   `?sp=...`). Anyone who reads a SDK DEBUG log line with the URL
   intact can replay the URL until expiry — typically a few hours.
   This is the same threat model that makes AWS/Azure/GCP SDK docs
   warn about logging presigned URLs.
2. **NIP path segments** are PII. Endpoints like
   `/v2/permissions/grant/{nip}` or `/v2/sessions/{ref}/by-nip/{nip}`
   embed the taxpayer ID directly in the URL. Application logs that
   capture HTTP request URIs leak NIPs into log aggregators,
   screenshots, error reports, and crash dumps.

Earlier wave-1 fix added query-string redaction at one log site
(`HttpSupport.execute`) but several other sites (exception
constructors, `KsefBatchSession` upload log, `PreparedInvoiceExport`
download log) still logged raw URIs. Wave-2 review caught the gap.

## Decision

Every URI that crosses into a log line OR an exception message routes
through a single utility `UriRedaction` in
`internal.runtime.transport`:

```java
public final class UriRedaction {
    public static String redactQuery(URI url);          // strips ?...
    public static String redactNipSegments(URI url);    // /v2/.../{10-digits}/... → /v2/.../***1234/...
    public static String redactBoth(URI url);           // both
}
```

Call sites that route through it:

- `HttpSupport.execute(...)` — DEBUG log on every request,
- `HttpSupport` exception constructors that interpolate the URI,
- `KsefBatchSession.uploadSinglePart` — DEBUG log "Uploaded part %d to %s",
- `PreparedInvoiceExport.streamDownloadDecryptVerify` — DEBUG log on each part,
- `KsefException` subclasses that include the URI in `getMessage()`.

Static analysis enforces it. A custom PMD rule would be ideal; in
the absence of one, code review and a `grep` for raw `request.uri()`
in log/exception code paths catches drift. New transport-touching
code MUST route through `UriRedaction` from the first commit.

HTTPS-only assertion. KSeF only ever returns HTTPS presigned URLs,
but a misbehaving or man-in-the-middle presigner could in principle
hand back `http://`. Both upload and download paths assert the scheme
explicitly:

```java
if (url.getScheme() == null || !SCHEME_HTTPS.equalsIgnoreCase(url.getScheme())) {
    throw new KsefException(ERR_INSECURE_PART_URL + redactQuery(url), null);
}
```

The exception message uses `redactQuery` so the rejection log line
itself does not leak the (now-attacker-controlled) URL parameters.

## Alternatives considered

1. **Suppress the entire URI from logs** — log only path and method.
   Rejected: the path component is the operationally useful part for
   debugging "which endpoint timed out", and stripping the host
   makes it impossible to distinguish multi-region drift.
2. **Mask via a custom SLF4J converter** (`%redacted{request.uri}`).
   Rejected: requires every log message to use a dedicated
   parameter and bind a specific layout; one missed usage and the
   raw URI ships. Routing through `UriRedaction` at the call site is
   harder to bypass.
3. **Trust callers to configure their logback patterns.** Rejected:
   the SDK is a library, not an app. We cannot reasonably assume
   consumer logback discipline. Default-secure is the policy.

## Consequences

- **Every URI-emitting log line in transport code MUST call a
  `UriRedaction` method.** New endpoints touching presigned URLs
  inherit the obligation.
- **`KsefException` constructors that take a URI MUST redact** before
  storing in `getMessage()` — otherwise re-raising the exception
  through `LOGGER.error("...", ex)` leaks the unredacted URI via
  stack-trace serialization.
- **HTTPS assertion adds one branch per upload/download.** Negligible
  perf cost; the guard message itself uses `redactQuery` so the
  rejection event does not reveal the malicious URL params.
- **Diagnostic full-URI logging is deliberately not gated behind a
  `-Dksef.debug.unredacted=true` flag.** A previous round added
  TRACE-level full-response-body logging during F1 wire-shape
  capture; it dumped JWTs from `/auth/token/*`. Removed entirely
  rather than gated, because the re-add cost is trivial when
  genuinely needed and the always-on attack surface is unacceptable.
  See memory `feedback_yagni_strictness.md`.
- **Tests cover both directions.** `UriRedactionTest` asserts the
  positive (queries / NIP segments are stripped) and the regression
  (URLs without sensitive content pass through unchanged so logs
  stay diagnostic).

## Where it lives

- `internal/runtime/transport/UriRedaction.java` — `redactQuery`,
  `redactNipSegments`, `redactBoth`, plus the
  `NIP_PREFIX_SEGMENT` regex with bounded character classes
  (anti-ReDoS).
- `internal/runtime/transport/HttpSupport.java` — every URI log /
  exception goes through it.
- `domain/invoicing/KsefBatchSession.java` — upload-side scheme guard
  + log redaction.
- `domain/invoicing/PreparedInvoiceExport.java` — download-side
  scheme guard + log redaction.
- `internal/runtime/transport/UriRedactionTest.java` — gate.
