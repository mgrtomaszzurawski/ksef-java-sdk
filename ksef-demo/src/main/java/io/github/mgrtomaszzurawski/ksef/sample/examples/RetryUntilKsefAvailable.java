/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
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
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnavailableException;

/**
 * Wait-and-retry pattern for {@link KsefUnavailableException}.
 *
 * <p>Useful when a consumer prefers to keep retrying online submission
 * over a few minutes before falling back to offline issuance — for
 * example because the local certificate is offline or because they
 * want to avoid the offline workflow entirely.
 *
 * <p>Use {@link OfflineFallbackPattern} instead when the consumer must
 * keep issuing invoices regardless of KSeF availability.
 */
public final class RetryUntilKsefAvailable {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final long INITIAL_BACKOFF_MILLIS = 1_000L;
    private static final long MAX_BACKOFF_MILLIS = 30_000L;
    private static final int BACKOFF_MULTIPLIER = 2;
    private static final String ERR_NULL_SESSION = "session must not be null";
    private static final String ERR_NULL_INVOICE = "invoice must not be null";
    private static final String ERR_GAVE_UP = "KSeF still unavailable after %d attempts";

    private RetryUntilKsefAvailable() {
    }

    /**
     * Send {@code invoice} through {@code session.sendInvoice(...)},
     * retrying on {@link KsefUnavailableException} up to
     * {@value #DEFAULT_MAX_ATTEMPTS} times with exponential backoff.
     *
     * @return the {@link SubmittedInvoice} returned by the first
     *     successful attempt
     * @throws KsefUnavailableException when every attempt failed; the
     *     final attempt's exception is rethrown
     * @throws InterruptedException if the calling thread is interrupted
     *     while sleeping between retries
     */
    public static SubmittedInvoice sendWithRetry(OnlineSession session, Invoice invoice)
            throws InterruptedException {
        if (session == null) {
            throw new NullPointerException(ERR_NULL_SESSION);
        }
        if (invoice == null) {
            throw new NullPointerException(ERR_NULL_INVOICE);
        }
        long backoffMillis = INITIAL_BACKOFF_MILLIS;
        KsefUnavailableException lastFailure = null;
        for (int attempt = 1; attempt <= DEFAULT_MAX_ATTEMPTS; attempt++) {
            try {
                return session.sendInvoice(invoice);
            } catch (KsefUnavailableException unavailable) {
                lastFailure = unavailable;
                if (attempt == DEFAULT_MAX_ATTEMPTS) {
                    break;
                }
                Thread.sleep(backoffMillis);
                backoffMillis = Math.min(backoffMillis * BACKOFF_MULTIPLIER, MAX_BACKOFF_MILLIS);
            }
        }
        throw new KsefUnavailableException(
                String.format(ERR_GAVE_UP, DEFAULT_MAX_ATTEMPTS),
                lastFailure);
    }
}
