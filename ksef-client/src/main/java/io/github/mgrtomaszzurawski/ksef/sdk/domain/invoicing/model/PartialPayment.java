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
 * One partial payment under {@code Fa/Platnosc/ZaplataCzesciowa} —
 * an installment already received before the invoice was issued.
 *
 * <p>The payment form is an XSD choice: either a coded
 * {@code FormaPlatnosci} or an "other" marker
 * ({@code PlatnoscInna}) with a free-text {@code OpisPlatnosci}.
 *
 * @param amount installment amount ({@code KwotaZaplatyCzesciowej}) — mandatory
 * @param date date the installment was received
 *     ({@code DataZaplatyCzesciowej}) — null when not supplied
 * @param methodCode coded payment form ({@code FormaPlatnosci}, e.g. "1"
 *     cash, "2" card, "6" transfer) — null when the "other" form is used
 * @param otherForm "other payment form" marker ({@code PlatnoscInna = 1})
 *     — null when a coded form is used
 * @param otherDescription free-text detail of the other form
 *     ({@code OpisPlatnosci}) — null when a coded form is used
 *
 * @since 0.1.0
 */
public record PartialPayment(
        BigDecimal amount,
        @Nullable LocalDate date,
        @Nullable String methodCode,
        @Nullable Boolean otherForm,
        @Nullable String otherDescription) {

    private static final String ERR_NULL_AMOUNT = "amount must not be null";

    public PartialPayment {
        Objects.requireNonNull(amount, ERR_NULL_AMOUNT);
    }
}
