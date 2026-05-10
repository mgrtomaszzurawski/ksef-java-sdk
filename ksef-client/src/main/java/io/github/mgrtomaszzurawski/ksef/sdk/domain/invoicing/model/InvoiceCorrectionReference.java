/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reference to the invoice being corrected — required when the
 * containing FA(2)/FA(3) invoice declares a correction
 * {@code RodzajFaktury} (KOR / KOR_ZAL / KOR_ROZ).
 *
 * <p>Maps onto the {@code DaneFaKorygowanej} sub-element of the
 * {@code Fa} block.
 *
 * @param originalInvoiceNumber invoice number of the corrected
 *     document (P_2A)
 * @param originalInvoiceDate issue date of the corrected document
 *     (DataWystFaKorygowanej)
 *
 * @since 1.0.0
 */
public record InvoiceCorrectionReference(
        String originalInvoiceNumber,
        LocalDate originalInvoiceDate) {

    private static final String ERR_NULL_NUMBER = "originalInvoiceNumber must not be null";
    private static final String ERR_NULL_DATE = "originalInvoiceDate must not be null";

    public InvoiceCorrectionReference {
        Objects.requireNonNull(originalInvoiceNumber, ERR_NULL_NUMBER);
        Objects.requireNonNull(originalInvoiceDate, ERR_NULL_DATE);
    }
}
