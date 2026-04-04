/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;

/**
 * Shared helper methods for demo runners.
 */
final class RunnerHelper {

    static final int POLL_INITIAL_DELAY_MS = 2000;
    static final int POLL_MAX_DELAY_MS = 8000;
    static final int POLL_TIMEOUT_MS = 60000;
    static final int POLL_BACKOFF_MULTIPLIER = 2;

    private RunnerHelper() { }

    /**
     * Extract a detailed error message from an exception, including KSeF response body if available.
     */
    static String errorMessage(Exception exception) {
        if (exception instanceof KsefException ksefEx && ksefEx.responseBody() != null) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage()
                    + " | body: " + ksefEx.responseBody();
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
