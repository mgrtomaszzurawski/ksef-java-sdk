/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import java.util.Objects;

/**
 * Thrown by {@code KsefSession.close()} / {@code KsefBatchSession.close()}
 * when polling reaches the maximum attempt count without observing a terminal
 * server status. Surfaces the uncertainty to the caller instead of letting
 * try-with-resources exit normally on indeterminate state.
 *
 * <p>Carries the session reference number and the last observed status code
 * so the caller can decide whether to wait longer, abandon, or retrieve UPO
 * out-of-band.
 */
public final class KsefSessionPollingTimeoutException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_TEMPLATE =
            "Session %s polling timed out after %d attempts; last observed status code: %s";
    private static final String NO_STATUS_OBSERVED = "<none>";

    private final String referenceNumber;
    private final int attempts;
    private final Integer lastObservedStatusCode;

    public KsefSessionPollingTimeoutException(String referenceNumber, int attempts,
                                              Integer lastObservedStatusCode) {
        super(formatMessage(referenceNumber, attempts, lastObservedStatusCode), (Throwable) null);
        this.referenceNumber = Objects.requireNonNull(referenceNumber, "referenceNumber");
        this.attempts = attempts;
        this.lastObservedStatusCode = lastObservedStatusCode;
    }

    public String referenceNumber() {
        return referenceNumber;
    }

    public int attempts() {
        return attempts;
    }

    public Integer lastObservedStatusCode() {
        return lastObservedStatusCode;
    }

    private static String formatMessage(String referenceNumber, int attempts,
                                        Integer lastObservedStatusCode) {
        String code = lastObservedStatusCode == null ? NO_STATUS_OBSERVED : String.valueOf(lastObservedStatusCode);
        return String.format(MESSAGE_TEMPLATE, referenceNumber, attempts, code);
    }
}
