/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The period an invoice covers ({@code Fa/OkresFa}) — used instead of a
 * single delivery date for continuous supplies (art. 19a). Both bounds are
 * mandatory within the {@code OkresFa} element.
 *
 * @param from period start ({@code P_6_Od})
 * @param to period end ({@code P_6_Do})
 *
 * @since 0.1.0
 */
public record InvoicePeriod(LocalDate from, LocalDate to) {

    private static final String ERR_NULL_FROM = "from must not be null";
    private static final String ERR_NULL_TO = "to must not be null";

    public InvoicePeriod {
        Objects.requireNonNull(from, ERR_NULL_FROM);
        Objects.requireNonNull(to, ERR_NULL_TO);
    }
}
