/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;

/**
 * Shared helper methods for demo runners.
 */
final class RunnerHelper {

    static final int POLL_INITIAL_DELAY_MS = 2000;
    static final int POLL_TIMEOUT_MS = 60000;
    static final int POLL_BACKOFF_MULTIPLIER = 2;

    private RunnerHelper() { }

    /**
     * Extract a detailed error message from an exception, including KSeF response body if available.
     */
    static String errorMessage(Exception exception) {
        if (exception instanceof KsefException ksefException && ksefException.responseBody() != null) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage()
                    + " | body: " + ksefException.responseBody();
        }
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    /**
     * Calculate elapsed time in milliseconds since start.
     */
    static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
