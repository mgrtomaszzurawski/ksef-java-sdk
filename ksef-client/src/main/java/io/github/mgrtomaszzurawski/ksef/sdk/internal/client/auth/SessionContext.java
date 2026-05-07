/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * Thread-safe holder for KSeF session state.
 * Stores the JWT token and session reference number obtained during authentication.
 * Updated atomically on auth, refresh, and session termination.
 *
 * @since 1.0.0
 */
public final class SessionContext {

    private static final String ERR_NO_SESSION = "No active KSeF session. Authenticate first.";

    private final AtomicReference<SessionState> state = new AtomicReference<>();

    /**
     * Set session state after successful authentication.
     */
    public void activate(String token, String referenceNumber, OffsetDateTime expiry) {
        state.set(new SessionState(token, referenceNumber, expiry, null));
    }

    /**
     * Replace the access token (and its expiry) after a successful refresh,
     * preserving the current reference number and refresh token.
     */
    public void updateAccessToken(String newToken, OffsetDateTime newExpiry) {
        state.updateAndGet(current -> {
            if (current == null) {
                throw new IllegalStateException(ERR_NO_SESSION);
            }
            return new SessionState(newToken, current.referenceNumber(),
                    newExpiry, current.refreshToken());
        });
    }

    /**
     * Persist the refresh token obtained via the redeem-tokens flow so future
     * re-authentications can prefer the {@code /auth/token/refresh} endpoint
     * over a full challenge-response cycle.
     */
    public void storeRefreshToken(String token) {
        state.updateAndGet(current -> {
            if (current == null) {
                throw new IllegalStateException(ERR_NO_SESSION);
            }
            return new SessionState(current.token(), current.referenceNumber(),
                    current.expiry(), token);
        });
    }

    /**
     * Refresh token captured from the last redeem-tokens response, or
     * {@code null} when no refresh token is available.
     */
    public @Nullable String refreshToken() {
        SessionState current = state.get();
        return current == null ? null : current.refreshToken();
    }

    /**
     * Clear session state on termination.
     */
    public void clear() {
        state.set(null);
    }

    /**
     * Get current Bearer token. Throws if no active session.
     */
    public String token() {
        SessionState current = state.get();
        if (current == null) {
            throw new IllegalStateException(ERR_NO_SESSION);
        }
        return current.token();
    }

    /**
     * Get current session reference number. Throws if no active session.
     */
    public String referenceNumber() {
        SessionState current = state.get();
        if (current == null) {
            throw new IllegalStateException(ERR_NO_SESSION);
        }
        return current.referenceNumber();
    }

    /**
     * Check if a session is currently active.
     */
    public boolean isActive() {
        return state.get() != null;
    }

    /**
     * Check if the session has expired based on the expiry timestamp.
     */
    public boolean isExpired() {
        SessionState current = state.get();
        if (current == null || current.expiry() == null) {
            return true;
        }
        return OffsetDateTime.now().isAfter(current.expiry());
    }

    private record SessionState(String token,
                                String referenceNumber,
                                OffsetDateTime expiry,
                                @Nullable String refreshToken) {
    }
}
