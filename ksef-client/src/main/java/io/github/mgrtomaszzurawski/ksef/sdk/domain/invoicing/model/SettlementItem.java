/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One additional-settlement line under {@code Fa/Rozliczenie} — a charge
 * ({@code Obciazenia}) added to, or a deduction ({@code Odliczenia})
 * subtracted from, the invoice total {@code P_15}. Both fields are
 * mandatory in the XSD.
 *
 * @param amount the charged / deducted amount ({@code Kwota})
 * @param reason the reason for the charge / deduction ({@code Powod})
 *
 * @since 0.1.0
 */
public record SettlementItem(BigDecimal amount, String reason) {

    private static final String ERR_NULL_AMOUNT = "amount must not be null";
    private static final String ERR_NULL_REASON = "reason must not be null";

    public SettlementItem {
        Objects.requireNonNull(amount, ERR_NULL_AMOUNT);
        Objects.requireNonNull(reason, ERR_NULL_REASON);
    }
}
