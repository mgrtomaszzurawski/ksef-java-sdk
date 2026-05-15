/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Public API surface for KSeF authentication-session management.
 *
 * <p>Obtain an instance via {@code KsefClient.authSessions()}. Authentication
 * itself is handled lazily by {@code KsefClient} on the first call that
 * needs it; this accessor exposes the explicit session-management
 * verbs (terminate, list, terminate-by-reference) and the diagnostic
 * {@link #lastChallengeClientIp()} hook used to autopin
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy}
 * IP allow-lists.
 *
 * @since 1.0.0
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
     * <p>Clears all session state. After calling this, the next operation
     * triggers a fresh authentication flow (lazy auth).
     */
    void terminate();

    /**
     * Lazily paginate every active auth session for this consumer's KSeF
     * context, walking pages on demand via the {@code x-continuation-token}
     * cursor. Holds at most one page in heap; stops fetching as soon as the
     * caller's terminal operation is satisfied.
     *
     * <p>Use {@link Stream#limit(long)} or
     * {@link Stream#takeWhile(java.util.function.Predicate)}
     * to bound the walk.
     */
    Stream<AuthSession> streamAuthSessions();

    /**
     * Terminate a specific auth session by its reference number. Useful
     * for cleaning up orphaned sessions or terminating a session other
     * than the current one. Use {@link #terminate()} for the current
     * session.
     *
     * @param referenceNumber reference number of the session to terminate
     */
    void terminateSession(String referenceNumber);

    /**
     * The {@code clientIp} value reported by KSeF in the most recent
     * {@code /auth/challenge} response, or empty if no challenge has
     * been requested yet on the parent client.
     *
     * <p>Use to autopin
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy}
     * for token authentication: read this after the first authentication,
     * then build a fresh policy that whitelists exactly this IP and pass
     * it via
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials}
     * on subsequent authentications.
     *
     * @return the client IP from the latest challenge, or empty if no
     *     challenge has been issued yet
     */
    Optional<String> lastChallengeClientIp();
}
