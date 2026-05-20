/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceTypeTest {

    @Test
    void isCorrection_returnsTrue_forKor() {
        assertTrue(InvoiceType.KOR.isCorrection());
    }

    @Test
    void isCorrection_returnsTrue_forKorZal() {
        assertTrue(InvoiceType.KOR_ZAL.isCorrection());
    }

    @Test
    void isCorrection_returnsTrue_forKorRoz() {
        assertTrue(InvoiceType.KOR_ROZ.isCorrection());
    }

    @Test
    void isCorrection_returnsTrue_forKorPef() {
        assertTrue(InvoiceType.KOR_PEF.isCorrection());
    }

    @Test
    void isCorrection_returnsTrue_forKorVatRr() {
        assertTrue(InvoiceType.KOR_VAT_RR.isCorrection());
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceType.class, names = {"VAT", "ZAL", "ROZ", "UPR", "VAT_PEF", "VAT_PEF_SP", "VAT_RR"})
    void isCorrection_returnsFalse_forNonCorrectionTypes(InvoiceType type) {
        assertFalse(type.isCorrection(), type.name() + " must not be classified as correction");
    }
}
