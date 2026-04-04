/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe holder for KSeF session state.
 * Stores the JWT token and session reference number obtained during authentication.
 * Updated atomically on auth, refresh, and session termination.
 */
public final class SessionContext {

    private static final String ERR_NO_SESSION = "No active KSeF session. Authenticate first.";

    private final AtomicReference<SessionState> state = new AtomicReference<>();

    /**
     * Set session state after successful authentication.
     */
    public void activate(String token, String referenceNumber, OffsetDateTime expiry) {
        state.set(new SessionState(token, referenceNumber, expiry));
    }

    /**
     * Update token after refresh (keeps same reference number).
     */
    public void refreshToken(String newToken, OffsetDateTime newExpiry) {
        SessionState current = state.get();
        if (current == null) {
            throw new IllegalStateException(ERR_NO_SESSION);
        }
        state.set(new SessionState(newToken, current.referenceNumber(), newExpiry));
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

    private record SessionState(String token, String referenceNumber, OffsetDateTime expiry) {
    }
}
