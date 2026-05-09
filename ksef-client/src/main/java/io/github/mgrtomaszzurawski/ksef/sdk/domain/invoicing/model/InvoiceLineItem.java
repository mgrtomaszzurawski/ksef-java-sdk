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
 * <p>Models the minimum information needed for a {@code FaWiersz}
 * element: row ordinal, description, quantity, unit price net, total
 * net amount and VAT rate. Fields outside this minimum (GTIN, PKWiU,
 * GTU, accise, foreign-currency) are deferred to the
 * {@code Invoice.fromXml(...)} escape hatch.
 *
 * @param rowNumber row ordinal — 1-based, NrWierszaFa
 * @param description product / service description (P_7)
 * @param unitOfMeasure unit symbol (P_8A) — may be null
 * @param quantity quantity (P_8B) — may be null
 * @param netUnitPrice unit price net (P_9A) — may be null
 * @param netAmount total net amount (P_11)
 * @param vatRate VAT rate token (P_12) — e.g. {@code "23"}, {@code "8"},
 *     {@code "5"}, {@code "0"}, {@code "zw"}, {@code "np"}
 *
 * @since 1.0.0
 */
public record InvoiceLineItem(
        int rowNumber,
        String description,
        @Nullable String unitOfMeasure,
        @Nullable BigDecimal quantity,
        @Nullable BigDecimal netUnitPrice,
        BigDecimal netAmount,
        String vatRate) {

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
