/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnavailableException;

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
    public static SubmittedInvoice<Invoice> sendWithOfflineFallback(OnlineSession session,
                                                           Invoice invoice,
                                                           KsefCertificate certificate,
                                                           OfflineMode mode,
                                                           OfflineSigningContext context) {
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
            var offline = OfflineInvoice.fromInvoice(invoice, certificate, mode, context);
            return session.sendOfflineInvoice(offline);
        }
    }
}
