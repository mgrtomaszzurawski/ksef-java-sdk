/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * Early-payment discount (skonto) under {@code Fa/Platnosc/Skonto}.
 * Both fields are mandatory within the {@code Skonto} element.
 *
 * <p>{@code amount} is a free-text string in the XSD ({@code TZnakowy}),
 * not a numeric type — the spec lets the issuer state the discount as
 * e.g. a percentage or an amount with wording.
 *
 * @param conditions conditions the buyer must meet to take the discount
 *     ({@code WarunkiSkonta})
 * @param amount the discount, as stated on the invoice
 *     ({@code WysokoscSkonta}) — free text, not parsed
 *
 * @since 0.1.0
 */
public record EarlyPaymentDiscount(String conditions, String amount) {

    private static final String ERR_NULL_CONDITIONS = "conditions must not be null";
    private static final String ERR_NULL_AMOUNT = "amount must not be null";

    public EarlyPaymentDiscount {
        Objects.requireNonNull(conditions, ERR_NULL_CONDITIONS);
        Objects.requireNonNull(amount, ERR_NULL_AMOUNT);
    }
}
