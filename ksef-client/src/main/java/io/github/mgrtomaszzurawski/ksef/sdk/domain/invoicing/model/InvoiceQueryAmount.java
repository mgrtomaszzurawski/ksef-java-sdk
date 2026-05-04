/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Amount range filter for {@code POST /invoices/query/metadata} —
 * narrows the result set to invoices whose chosen monetary axis
 * (gross / net / VAT) falls in the inclusive {@code [from, to]} window.
 *
 * <p>Mirrors the OpenAPI {@code InvoiceQueryAmount} schema.
 *
 * @param type which monetary axis to filter on
 * @param from lower bound (inclusive)
 * @param to upper bound (inclusive)
 */
public record InvoiceQueryAmount(InvoiceQueryAmountType type, BigDecimal from, BigDecimal to) {

    public InvoiceQueryAmount {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
