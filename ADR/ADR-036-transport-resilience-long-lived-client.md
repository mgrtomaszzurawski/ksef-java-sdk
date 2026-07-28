# ADR-036: Transport resilience for a long-lived KsefClient

**Date:** 2026-07-28
**Status:** Accepted
**Relates to:** [ADR-013](ADR-013-httpruntime-narrow-interface.md) (HttpRuntime seam), RCA `context/RCA/RCA-longlived-client-halfopen-h2-connection-no-recovery-2026-07-28-1415.md`

## Context

A production consumer holds one `KsefClient` for the process lifetime (the
intended usage — the client is designed to be reused). After ~13 days of uptime
every call began failing with `HttpTimeoutException`; 294 failures, **0
successes for 6 days**, recovered only by a process restart.

Root cause (see RCA): `KsefClient` builds a single `java.net.http.HttpClient`
once and holds it for life. Its default protocol is **HTTP/2**, which
multiplexes every request over **one** connection to the origin. An edge/NAT
(KSeF is behind Imperva) silently **half-closed** the idle connection — no
`RST`, no `FIN`, no HTTP/2 `GOAWAY` — so the JDK kept the dead connection in its
pool and reused it; each request timed out after the 30s read timeout. Because
the first call in authentication is a public-key fetch that throws before the
auth state flips to "authenticated", the client wedged permanently on that call.
A `curl` from the same egress worked throughout (fresh connection); the restart
worked only because it built a new client with a fresh pool.

Key constraint: `java.net.http.HttpClient` exposes **no** per-connection
eviction, **no** validate-after-inactivity, **no** custom `SocketFactory`, and
**no** per-client idle-timeout — only JVM-global system properties. The only
lever a library can pull to guarantee a fresh connection is to **build a new
`HttpClient`** (a new pool).

## Decision

Introduce an internal `ManagedHttpClient` (in `internal.runtime.transport`) that
owns the lifetime of the shared client and makes a long-lived `KsefClient`
self-heal, **staying on `java.net.http` with no new dependency**:

1. **Transport-failure recovery (the guarantee).** An `IOException` on send
   (timeout, connection reset) rebuilds the client on a fresh pool so the next
   call is healthy; for an **idempotent** request (GET/HEAD) the send is retried
   once on the new client. This lives **below** the business `RetryPolicy`, so it
   fires even at `RetryPolicy.maxAttempts(1)` and does not change 429/5xx/`Retry-After`
   behaviour. Non-idempotent requests (POST/PUT/PATCH/DELETE) are **not** replayed
   — the client is still rebuilt, so the next call recovers, but no double submit.

2. **Idle TTL (soft prevention).** Before a send, if the client has been idle
   longer than a configurable TTL (default 60s), it is rebuilt so a connection
   the edge may have silently dropped during an idle window is never reused.
   `Duration.ZERO` disables it. Kept infrequent on purpose — a new `HttpClient`
   allocates a selector thread (and is not `AutoCloseable` on JDK 17), so the
   idle rebuild is an optimisation, not the guarantee.

3. **Public `KsefClient.reconnect()`** — forces a fresh transport on the next
   call without discarding the client or its auth state (defence in depth).

`HttpSupport` (the single JSON/domain/auth send choke point) routes through the
managed client; `HttpRuntime.httpClient()` returns the current instance so any
at-use-time reader tracks a rebuild.

## Alternatives considered

- **Switch the HTTP client to Apache HttpClient 5 / OkHttp** — these expose
  `evictIdleConnections`, `validateAfterInactivity`, and configurable keepalive
  as first-class features, i.e. they solve this category cleanly. **Rejected for
  now** on the SDK's minimise-dependencies principle (`java.net.http` is JDK
  core). Parked as the candidate durable fix if the workaround proves
  insufficient; this ADR would then be superseded, not edited (ADR immutability).
- **Force HTTP/1.1** (`.version(HTTP_1_1)`) — a connection *pool* instead of one
  multiplexed connection removes the total-outage amplifier. **Not adopted**: it
  is blast-radius reduction, not a fix (H1 connections also go half-open), and
  the idle-TTL + rebuild path handles the connection liveness regardless of
  protocol. May be added later as belt-and-suspenders.
- **Health-check-on-borrow** (probe before every request) — rejected as
  overkill and latency overhead for every call.
- **Rely on the existing retry** — `RetryHandler` already marks transport
  failures retryable, but it is gated by `RetryPolicy` (no retry at
  `maxAttempts(1)`) and retries on the **same** dead pooled connection, so it
  cannot heal this failure.

## Consequences

- A long-lived `KsefClient` recovers from a silently dropped connection within
  the next call (idempotent) or the call after (non-idempotent), no restart.
- New public API: `KsefClient.reconnect()` and `KsefClient.Builder.idleConnectionTtl(Duration)`
  (`@since 0.1.2`). Pre-1.0, additive.
- Streaming batch/export paths (`BatchSubmissionFlow`, `PreparedInvoiceExport`)
  still send on the raw client fetched per-operation; they are short-lived
  per-operation objects (not exposed to the 13-day-idle failure) and pick up a
  rebuilt client on their next operation. Routing their streaming sends through
  the managed retry is a possible follow-up, not required to close the incident.
- Idle rebuilds allocate a new `HttpClient` (selector thread); the default TTL
  (60s) keeps this infrequent, and the old client is released to GC.
