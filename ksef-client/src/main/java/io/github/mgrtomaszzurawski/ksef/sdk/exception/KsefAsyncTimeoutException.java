/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when the SDK's internal async-polling helper exhausts its
 * timeout budget before the polled server-side operation reaches a
 * terminal state.
 *
 * <p><strong>Distinct from transport timeout</strong> (which the SDK
 * folds into {@link KsefServerException}): a server-side operation
 * polled here <em>may</em> have completed or be in progress on the
 * server when this exception is thrown — the SDK only signals that
 * the local wait gave up. Callers may poll the same reference number
 * later if they want to wait longer, or accept the indeterminate state
 * and retrieve results out of band.
 *
 * <p>Surfaced verbatim from synchronous operations that internally
 * poll, e.g. {@code Permissions.grant*} / {@code revoke*},
 * {@code Certificates.requestNewCertificate}, {@code OnlineSession.close()},
 * and the batch submission flow after their per-call {@code Duration}
 * budget is exhausted.
 *
 * <p>The reference-number / attempt-count / last-observed status fields
 * are populated when the SDK was polling a known operation reference
 * (sessions, certificate enrollments, etc.). They are null when the
 * timeout fired before any operation reference was assigned.
 *
 * @since 1.0.0
 */
public class KsefAsyncTimeoutException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_TEMPLATE =
            "%s polling timed out after %d attempts; last observed status code: %s";
    private static final String NO_STATUS_OBSERVED = "<none>";

    private final @Nullable String referenceNumber;
    private final int attempts;
    private final @Nullable Integer lastObservedStatusCode;

    /**
     * Construct with a free-form message (used when the timeout fired
     * before an operation reference was assigned).
     */
    public KsefAsyncTimeoutException(String message) {
        super(message, null);
        this.referenceNumber = null;
        this.attempts = 0;
        this.lastObservedStatusCode = null;
    }

    /**
     * Construct with full polling context — used when the SDK was
     * polling a known operation reference (session close, certificate
     * enrollment, batch submission). Callers can inspect
     * {@link #referenceNumber()}, {@link #attempts()}, and
     * {@link #lastObservedStatusCode()} to decide remediation.
     */
    public KsefAsyncTimeoutException(String referenceNumber, int attempts,
                                     @Nullable Integer lastObservedStatusCode) {
        super(formatMessage(referenceNumber, attempts, lastObservedStatusCode), (Throwable) null);
        this.referenceNumber = Objects.requireNonNull(referenceNumber, "referenceNumber");
        this.attempts = attempts;
        this.lastObservedStatusCode = lastObservedStatusCode;
    }

    public @Nullable String referenceNumber() {
        return referenceNumber;
    }

    public int attempts() {
        return attempts;
    }

    public @Nullable Integer lastObservedStatusCode() {
        return lastObservedStatusCode;
    }

    private static String formatMessage(String referenceNumber, int attempts,
                                        @Nullable Integer lastObservedStatusCode) {
        String code = lastObservedStatusCode == null ? NO_STATUS_OBSERVED : String.valueOf(lastObservedStatusCode);
        return String.format(MESSAGE_TEMPLATE, referenceNumber, attempts, code);
    }
}
