/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Single line item shared between the FA(2) and FA(3) typed builders.
 *
 * <p>Models the common business information for a {@code FaWiersz}
 * element: row ordinal, description, classification (GTIN, PKWiU),
 * quantity, unit price net, total net amount, VAT rate, gross amount
 * and VAT amount. Fields outside this common surface (GTU, accise,
 * foreign-currency, P_12 sub-types, indeks, uuid) remain accessible
 * via the {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa3InvoiceDocument#unsafeJaxbView()}
 * / {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa2InvoiceDocument#unsafeJaxbView()}
 * JAXB escape hatch.
 *
 * @param rowNumber row ordinal — 1-based, {@code NrWierszaFa}
 * @param description product / service description ({@code P_7})
 * @param gtin GTIN / barcode ({@code GTIN}) — may be null
 * @param pkwiu PKWiU classification ({@code PKWiU}) — may be null
 * @param unitOfMeasure unit symbol ({@code P_8A}) — may be null
 * @param quantity quantity ({@code P_8B}) — may be null
 * @param netUnitPrice unit price net ({@code P_9A}) — may be null
 * @param netAmount total net amount ({@code P_11})
 * @param vatRate VAT rate token ({@code P_12}) — e.g. {@code "23"}, {@code "8"},
 *     {@code "5"}, {@code "0"}, {@code "zw"}, {@code "np"}
 * @param grossAmount total gross amount ({@code P_11A}) — may be null on FA(2)
 *     when the schema branch omits it; FA(3) typically always populates
 * @param vatAmount VAT amount ({@code P_11Vat}) — may be null; usually populated
 *     on FA(3) and on FA(2) write-side when explicitly set
 *
 * @since 0.1.0
 */
public record InvoiceLineItem(
        int rowNumber,
        String description,
        @Nullable String gtin,
        @Nullable String pkwiu,
        @Nullable String unitOfMeasure,
        @Nullable BigDecimal quantity,
        @Nullable BigDecimal netUnitPrice,
        BigDecimal netAmount,
        String vatRate,
        @Nullable BigDecimal grossAmount,
        @Nullable BigDecimal vatAmount) {

    private static final String ERR_NULL_DESCRIPTION = "description must not be null";
    private static final String ERR_NULL_NET_AMOUNT = "netAmount must not be null";
    private static final String ERR_NULL_VAT_RATE = "vatRate must not be null";
    private static final String ERR_BAD_ROW_NUMBER = "rowNumber must be >= 1";
    private static final int MIN_ROW_NUMBER = 1;

    public InvoiceLineItem {
        Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        Objects.requireNonNull(netAmount, ERR_NULL_NET_AMOUNT);
        Objects.requireNonNull(vatRate, ERR_NULL_VAT_RATE);
        if (rowNumber < MIN_ROW_NUMBER) {
            throw new IllegalArgumentException(ERR_BAD_ROW_NUMBER);
        }
    }
}
