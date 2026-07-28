/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A single partial advance payment received before an advance invoice — the
 * typed view of a {@code Fa/ZaliczkaCzesciowa} element. Used when an advance
 * invoice settles several partial payments received on different dates.
 *
 * @param amount the gross amount of the partial advance received ({@code P_15Z})
 * @param receiptDate the date the partial advance was received ({@code P_6Z})
 * @param exchangeRate the currency exchange rate applied to this partial advance ({@code KursWalutyZW}); null when the invoice is in PLN
 *
 * @since 0.1.2
 */
public record PartialAdvance(
        BigDecimal amount,
        LocalDate receiptDate,
        @Nullable BigDecimal exchangeRate) {

    private static final String ERR_NULL_AMOUNT = "amount must not be null";
    private static final String ERR_NULL_DATE = "receiptDate must not be null";

    public PartialAdvance {
        Objects.requireNonNull(amount, ERR_NULL_AMOUNT);
        Objects.requireNonNull(receiptDate, ERR_NULL_DATE);
    }
}
