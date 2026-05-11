/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceSellerTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String OTHER_NIP = "9876543210";
    private static final String SELLER_NAME = "Acme";
    private static final String PEPPOL_ID = "ABCD1234";
    private static final String NIP_VAT_UE = "PL1111111111";
    private static final String INTERNAL_ID = "1111111111-00001";

    @Test
    void matches_returnsTrue_whenNipIdentifierEqualsSellerNip() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertTrue(seller.matches(KsefIdentifier.nip(SELLER_NIP)));
    }

    @Test
    void matches_returnsFalse_whenNipIdentifierDiffersFromSellerNip() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertFalse(seller.matches(KsefIdentifier.nip(OTHER_NIP)));
    }

    @Test
    void matches_returnsFalse_whenIdentifierIsPeppol() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertFalse(seller.matches(KsefIdentifier.peppolId(PEPPOL_ID)));
    }

    @Test
    void matches_returnsFalse_whenIdentifierIsNipVatUe() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertFalse(seller.matches(KsefIdentifier.nipVatUe(NIP_VAT_UE)));
    }

    @Test
    void matches_returnsFalse_whenIdentifierIsInternalId() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertFalse(seller.matches(KsefIdentifier.internalId(INTERNAL_ID)));
    }

    @Test
    void matches_returnsFalse_whenSellerNipIsNull() {
        InvoiceSeller seller = new InvoiceSeller(null, SELLER_NAME);
        assertFalse(seller.matches(KsefIdentifier.nip(SELLER_NIP)));
    }

    @Test
    void matches_throwsNpe_whenIdentifierIsNull() {
        InvoiceSeller seller = new InvoiceSeller(SELLER_NIP, SELLER_NAME);
        assertThrows(NullPointerException.class, () -> seller.matches(null));
    }
}
