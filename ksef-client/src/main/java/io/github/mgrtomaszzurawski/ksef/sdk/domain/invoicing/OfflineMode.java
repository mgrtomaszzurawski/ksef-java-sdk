/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

/**
 * KSeF offline-mode classification carried at SDK level.
 *
 * <p>All three values collapse to the same wire-level marker —
 * {@code offlineMode=true} — so the enum is SDK-side metadata used by
 * consumers to track which legal regime (and which submission deadline)
 * applies to the invoice. KSeF does not differentiate the modes on the
 * REST request body itself; the differentiation is observable only via
 * the announced unavailability windows on the public KSeF status page
 * and on the buyer-facing visualisations rendered with KOD I + KOD II.
 *
 * <p>Spec citations:
 * {@code ksef-docs/offline/automatyczne-okreslanie-trybu-offline.md},
 * {@code ksef-docs/offline/awaria-i-niedostepnosc.md}, REQ-OFFLINE-001
 * through REQ-OFFLINE-007.
 *
 * @since 1.0.0
 */
public enum OfflineMode {

    /**
     * Consumer-chosen offline window ("offline24"). The consumer issues
     * an invoice locally with KOD II and is required to upload it to
     * KSeF no later than the next business day after issuance. Legal
     * basis: art. 106nf ust. 1 ustawy o VAT (Polish VAT act); spec
     * {@code automatyczne-okreslanie-trybu-offline.md}.
     */
    OFFLINE_24,

    /**
     * KSeF announced unavailability ("niedostępność"). The consumer
     * issues invoices offline with KOD II during the announced window
     * and must upload them to KSeF no later than the next business day
     * after the end of the unavailability window. Legal basis:
     * art. 106nh ust. 1 ustawy o VAT; spec
     * {@code awaria-i-niedostepnosc.md}.
     */
    KSEF_UNAVAILABILITY,

    /**
     * KSeF emergency mode ("awaria"). The consumer issues invoices
     * offline with KOD II during the emergency window and has 7
     * business days from the end of the emergency to upload them to
     * KSeF. Legal basis: art. 106ng ust. 1 ustawy o VAT; spec
     * {@code awaria-i-niedostepnosc.md}.
     */
    KSEF_EMERGENCY;

    /** Wire-level value all three modes collapse to. */
    private static final String WIRE_VALUE_TRUE = "true";

    /**
     * Wire-level marker for the {@code offlineMode} field on the
     * {@code SendInvoiceRequest} body. Always {@code "true"} regardless
     * of the SDK-side mode classification — KSeF does not differentiate
     * the three modes at the REST layer.
     */
    public String wireValue() {
        return WIRE_VALUE_TRUE;
    }
}
