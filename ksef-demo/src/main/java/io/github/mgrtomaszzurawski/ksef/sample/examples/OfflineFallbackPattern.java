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

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnavailableException;
import java.time.LocalDate;

/**
 * Convert-after-failure pattern: catch
 * {@link KsefUnavailableException}, build an {@link OfflineInvoice}
 * with KOD I + KOD II QR codes, and submit through the offline
 * channel. The offline submission sets the wire-level
 * {@code offlineMode=true} so KSeF applies the correct
 * issuance-deadline rules.
 *
 * <p>This is the "always issue, even when KSeF is down" pattern —
 * compare with {@link RetryUntilKsefAvailable} which simply waits for
 * KSeF to recover.
 */
public final class OfflineFallbackPattern {

    private static final String ERR_NULL_SESSION = "session must not be null";
    private static final String ERR_NULL_INVOICE = "invoice must not be null";
    private static final String ERR_NULL_CERTIFICATE = "certificate must not be null";

    private OfflineFallbackPattern() {
    }

    /**
     * Try {@code session.sendInvoice(invoice)} first. On
     * {@link KsefUnavailableException}, build an
     * {@link OfflineInvoice} with the supplied certificate and submit
     * it through {@link OnlineSession#sendOfflineInvoice(OfflineInvoice)}.
     *
     * <p>The {@code mode} parameter governs the legal classification —
     * pass {@link OfflineMode#OFFLINE_24} for consumer-chosen offline,
     * {@link OfflineMode#KSEF_UNAVAILABILITY} when KSeF announced an
     * unavailability window, and {@link OfflineMode#KSEF_EMERGENCY}
     * when KSeF entered emergency mode.
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static SubmittedInvoice sendWithOfflineFallback(OnlineSession session,
                                                           Invoice invoice,
                                                           KsefCertificate certificate,
                                                           OfflineMode mode,
                                                           QrEnvironment environment,
                                                           QrContextType contextType,
                                                           String contextValue,
                                                           String sellerNip,
                                                           LocalDate issueDate) {
        if (session == null) {
            throw new NullPointerException(ERR_NULL_SESSION);
        }
        if (invoice == null) {
            throw new NullPointerException(ERR_NULL_INVOICE);
        }
        if (certificate == null) {
            throw new NullPointerException(ERR_NULL_CERTIFICATE);
        }
        try {
            return session.sendInvoice(invoice);
        } catch (KsefUnavailableException unavailable) {
            OfflineInvoice offline = OfflineInvoice.fromInvoice(
                    invoice, certificate, mode, environment, contextType,
                    contextValue, sellerNip, issueDate);
            return session.sendOfflineInvoice(offline);
        }
    }
}
