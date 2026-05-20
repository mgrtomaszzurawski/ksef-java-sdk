/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionListResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionsQueryRequest;
import java.util.stream.Stream;

/**
 * Public API surface for KSeF authentication-session management.
 *
 * <p>Obtain an instance via {@code KsefClient.authSessions()}. Authentication
 * itself is handled lazily by {@code KsefClient} on the first call that
 * needs it; this accessor exposes the explicit session-management
 * verbs (terminate, list, terminate-by-reference) and the diagnostic
 * {@link #lastChallengeClientIp()} hook used to autopin
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.policy.AuthorizationPolicy}
 * IP allow-lists.
 *
 * @since 0.1.0
 */
public interface AuthSessions {

    /**
     * Drive the KSeF challenge-response handshake now rather than on the
     * first protected call. Idempotent — no-op when already logged in.
     * Thread-safe.
     *
     * <p>Optional: the SDK authenticates lazily on the first call that
     * needs a session token, so this method exists for consumers who
     * want predictable startup latency or a fail-fast credentials check
     * (batch jobs, scheduled workers, Spring {@code @PostConstruct}
     * smoke tests).
     *
     * <p>Performs the full flow: request challenge → encrypt token or
     * sign with XAdES → poll auth status → redeem operation token for
     * access + refresh tokens.
     */
    void ensureLoggedIn();

    /**
     * Terminate the current authentication session.
     *
     * <p>Local session state is cleared regardless of whether the server
     * DELETE succeeds — wrapped in a try-finally so the SDK's cached
     * {@code authenticated} flag returns to {@code false} even if the
     * wire call throws. Any HTTP exception then propagates to the caller.
     *
     * <p>After this call, the next operation triggers a fresh
     * authentication flow (lazy auth).
     *
     * <p>Note: local authentication state is a <em>cached hint</em>.
     * Server-side session termination — by another user, an admin, TTL
     * expiry, or token revocation — is handled transparently through
     * the SDK's 401-driven reauth retry, which kicks in on the next
     * call regardless of what the local flag says.
     */
    void terminate();

    /**
     * Single-page auth-session query. Cursor-based paging — pass
     * {@code null} continuationToken for the first page, then re-issue
     * with the {@link AuthSessionListResult#continuationToken()} from
     * the previous response. Unlike the offset-based {@code query*}
     * methods on other facades, KSeF's {@code GET /auth/sessions}
     * endpoint exposes only cursor paging (R1-13 audit confirmed in
     * the OpenAPI spec).
     *
     * @param filter cursor + page size (page size 10–100; server default 10)
     * @return one page of sessions plus the next-page cursor
     */
    AuthSessionListResult queryAuthSessions(AuthSessionsQueryRequest filter);

    /**
     * Lazily paginate every active auth session for this consumer's KSeF
     * context, walking pages on demand via the {@code x-continuation-token}
     * cursor. Holds at most one page in heap; stops fetching as soon as the
     * caller's terminal operation is satisfied. Honours the request's
     * {@link AuthSessionsQueryRequest#pageSize() pageSize} on every page
     * fetch.
     *
     * <p>{@link AuthSessionsQueryRequest#continuationToken()} is
     * <em>ignored</em> — the SDK paginator always starts from the
     * beginning of the result set. Use {@link #queryAuthSessions} when
     * navigating from a known cursor.
     *
     * <p>Use {@link Stream#limit(long)} or
     * {@link Stream#takeWhile(java.util.function.Predicate)}
     * to bound the walk.
     */
    Stream<AuthSession> streamAuthSessions(AuthSessionsQueryRequest filter);

    /**
     * Terminate a specific auth session by its reference number.
     * Intended for terminating OTHER sessions in your KSeF context
     * (shown via {@link #streamAuthSessions(AuthSessionsQueryRequest)}).
     * To end your own session
     * use {@link #terminate()} for proper local state cleanup.
     *
     * <p>If you pass your own reference number here, the server kills
     * your session but the SDK's local {@code authenticated} flag stays
     * {@code true}. The next protected call returns 401 and the SDK's
     * 401-driven reauth retry transparently re-authenticates — one
     * extra round-trip and the consumer sees no error. Still, prefer
     * {@link #terminate()} for clarity.
     *
     * @param referenceNumber reference number of the session to terminate
     */
    void terminateSession(String referenceNumber);

    /**
     * The {@code clientIp} value reported by KSeF in the most recent
     * {@code /auth/challenge} response on this client.
     *
     * <p>If no challenge has been issued yet, this method triggers
     * lazy authentication (full challenge/redeem flow) before returning,
     * so a {@code String} is always available. Cost: when the client
     * hasn't authenticated yet, the call performs a complete auth
     * handshake. Subsequent calls reuse the cached value.
     *
     * <p>Use to autopin
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.policy.AuthorizationPolicy}
     * for token authentication: read this after the first authentication,
     * then build a fresh policy that whitelists exactly this IP and pass
     * it via
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials}
     * on subsequent authentications.
     *
     * @return the client IP from the latest challenge (non-null)
     */
    String lastChallengeClientIp();
}
