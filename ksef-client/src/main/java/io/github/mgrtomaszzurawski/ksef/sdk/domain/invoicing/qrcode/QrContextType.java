/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

/**
 * Authorising context-identifier kind for KOD II QR verification URLs.
 *
 * <p>The wire form embedded in the URL path is the exact spelling listed in
 * <a href="https://github.com/CIRFMF/ksef-docs/blob/main/kody-qr.md">kody-qr.md</a>
 * — accessed via {@link #wireValue()} instead of {@link Enum#name()} because
 * KSeF uses {@code PascalCase} on the wire while Java enum names use
 * {@code UPPER_SNAKE}.
 */
public enum QrContextType {
    /** Polish tax identification number (10 digits). */
    NIP("Nip"),
    /** Internal context identifier — used when an invoice is filed under a non-NIP context. */
    INTERNAL_ID("InternalId"),
    /** EU VAT identifier for cross-border issuers. */
    NIP_VAT_UE("NipVatUe"),
    /** Peppol participant identifier (e.g. {@code 0007:1234567890}). */
    PEPPOL_ID("PeppolId");

    private final String wireValue;

    QrContextType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** KSeF-canonical wire spelling embedded in KOD II URLs. */
    public String wireValue() {
        return wireValue;
    }
}
