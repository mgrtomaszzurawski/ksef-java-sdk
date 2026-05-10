/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KsefIdentifierTest {

    private static final String VALID_NIP = "1111111111";
    private static final String SHORT_NIP = "123456789";
    private static final String NIP_WITH_LETTERS = "12345678AB";
    private static final String VALID_INTERNAL_ID = "1111111111-12345";
    private static final String VALID_NIP_VAT_UE = "1111111111-DE123456789";
    private static final String VALID_PEPPOL_ID = "PPL000123";
    private static final String BLANK = "   ";

    @Test
    void nip_whenValidValue_createsIdentifier() {
        // when
        KsefIdentifier identifier = KsefIdentifier.nip(VALID_NIP);

        // then
        assertEquals(KsefIdentifier.Type.NIP, identifier.type());
        assertEquals(VALID_NIP, identifier.value());
    }

    @Test
    void nip_whenTooShort_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> KsefIdentifier.nip(SHORT_NIP));
    }

    @Test
    void nip_whenContainsLetters_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> KsefIdentifier.nip(NIP_WITH_LETTERS));
    }

    @Test
    void nip_whenNull_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> KsefIdentifier.nip(null));
    }

    @Test
    void internalId_whenValidValue_createsIdentifier() {
        // when
        KsefIdentifier identifier = KsefIdentifier.internalId(VALID_INTERNAL_ID);

        // then
        assertEquals(KsefIdentifier.Type.INTERNAL_ID, identifier.type());
        assertEquals(VALID_INTERNAL_ID, identifier.value());
    }

    @Test
    void nipVatUe_whenValidValue_createsIdentifier() {
        // when
        KsefIdentifier identifier = KsefIdentifier.nipVatUe(VALID_NIP_VAT_UE);

        // then
        assertEquals(KsefIdentifier.Type.NIP_VAT_UE, identifier.type());
        assertEquals(VALID_NIP_VAT_UE, identifier.value());
    }

    @Test
    void peppolId_whenValidValue_createsIdentifier() {
        // when
        KsefIdentifier identifier = KsefIdentifier.peppolId(VALID_PEPPOL_ID);

        // then
        assertEquals(KsefIdentifier.Type.PEPPOL_ID, identifier.type());
        assertEquals(VALID_PEPPOL_ID, identifier.value());
    }

    @Test
    void canonicalConstructor_whenBlankValue_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new KsefIdentifier(KsefIdentifier.Type.PEPPOL_ID, BLANK));
    }

    @Test
    void canonicalConstructor_whenNullType_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefIdentifier(null, VALID_PEPPOL_ID));
    }

    @Test
    void canonicalConstructor_whenNullValue_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefIdentifier(KsefIdentifier.Type.PEPPOL_ID, null));
    }
}
