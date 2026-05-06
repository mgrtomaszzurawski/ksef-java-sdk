/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.time.Duration;

/**
 * Thrown when KSeF rejects a session interaction (most commonly the
 * first {@code send(...)} or status poll) with status 415 because the
 * server is still in the post-termination cooldown window for the
 * authenticated NIP. Codex 2026-05-05 #9.
 *
 * <p>Empirical observation (see
 * {@code context/RCA/RCA-session-cooldown-consecutive-runs-2026-04-04-2105.md}):
 * after terminating an online session, opening a new one for the same
 * NIP within ~30 seconds yields a session that opens with a reference
 * number but immediately reports status 415 and refuses to accept
 * invoices. A 5-second wait is not enough; 60 seconds usually is.
 *
 * <p>The {@link #suggestedRetryAfter()} accessor returns the SDK's
 * recommended wait — exposed for explicit retry policies. Consumers
 * may also retry their own way; the exception is purely advisory.
 *
 * @see KsefException
 *
 * @since 1.0.0
 */
public class KsefSessionCooldownException extends KsefException {

    /**
     * Empirically-observed minimum wait before re-attempting a fresh
     * online session for the same NIP after termination. The KSeF spec
     * does not document the cooldown — this is calibrated from
     * pre-1.0 validation runs.
     */
    public static final Duration TYPICAL_COOLDOWN = Duration.ofSeconds(60);

    /**
     * KSeF status code indicating "session not in a state that allows
     * the requested operation" — observed on the first {@code send(...)}
     * after a too-soon reopen.
     */
    public static final int COOLDOWN_STATUS_CODE = 415;

    private static final long serialVersionUID = 1L;

    private final Duration suggestedRetryAfter;

    /**
     * Predicate helper: returns {@code true} when the supplied status
     * code matches the empirically-observed cooldown signal. Consumers
     * catching {@code KsefServerException} can detect cooldown via
     * {@code KsefSessionCooldownException.isCooldownStatus(e.statusCode())}.
     */
    public static boolean isCooldownStatus(int statusCode) {
        return statusCode == COOLDOWN_STATUS_CODE;
    }

    public KsefSessionCooldownException(String message) {
        this(message, TYPICAL_COOLDOWN);
    }

    public KsefSessionCooldownException(String message, Duration suggestedRetryAfter) {
        super(message, null);
        this.suggestedRetryAfter = suggestedRetryAfter;
    }

    /**
     * The SDK's suggested minimum wait before retrying. Consumers may
     * override with their own retry policy.
     */
    public Duration suggestedRetryAfter() {
        return suggestedRetryAfter;
    }
}
