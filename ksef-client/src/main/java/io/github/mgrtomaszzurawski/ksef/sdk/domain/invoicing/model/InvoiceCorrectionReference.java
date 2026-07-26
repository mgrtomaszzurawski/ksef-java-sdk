/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Reference to the invoice being corrected — required when the
 * containing FA(2)/FA(3) invoice declares a correction
 * {@code RodzajFaktury} (KOR / KOR_ZAL / KOR_ROZ).
 *
 * <p>Maps onto a {@code DaneFaKorygowanej} sub-element of the
 * {@code Fa} block. In the XSD {@code NrFaKorygowanej} and
 * {@code DataWystFaKorygowanej} are mandatory; {@code NrKSeFFaKorygowanej}
 * is part of a choice and is absent when the corrected invoice was never
 * issued through KSeF.
 *
 * @param originalInvoiceNumber invoice number of the corrected
 *     document ({@code NrFaKorygowanej})
 * @param originalInvoiceDate issue date of the corrected document
 *     ({@code DataWystFaKorygowanej})
 * @param originalKsefNumber KSeF number of the corrected document
 *     ({@code NrKSeFFaKorygowanej}) — null when it was not in KSeF
 *
 * @since 0.1.0
 */
public record InvoiceCorrectionReference(
        String originalInvoiceNumber,
        LocalDate originalInvoiceDate,
        @Nullable String originalKsefNumber) {

    private static final String ERR_NULL_NUMBER = "originalInvoiceNumber must not be null";
    private static final String ERR_NULL_DATE = "originalInvoiceDate must not be null";

    public InvoiceCorrectionReference {
        Objects.requireNonNull(originalInvoiceNumber, ERR_NULL_NUMBER);
        Objects.requireNonNull(originalInvoiceDate, ERR_NULL_DATE);
    }

    /**
     * Convenience constructor for corrections of an invoice that was not
     * issued through KSeF (no {@code NrKSeFFaKorygowanej}).
     */
    public InvoiceCorrectionReference(String originalInvoiceNumber, LocalDate originalInvoiceDate) {
        this(originalInvoiceNumber, originalInvoiceDate, null);
    }
}
