/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The order or contract underpinning an advance-payment invoice — the
 * typed view of the {@code Fa/Zamowienie} node (art. 106f ust. 1 pkt 4).
 * Null on {@code InvoiceDocument.advanceOrder()} when the invoice carries
 * no order block.
 *
 * <p>Distinct from the {@code Fa/WarunkiTransakcji/Zamowienia} purchase-order
 * references ({@link PurchaseOrder}): this node itemises the ordered goods
 * and services priced for the advance invoice.
 *
 * @param orderValue total order value including tax ({@code WartoscZamowienia})
 * @param lines itemised order lines ({@code ZamowienieWiersz}); an XSD-valid
 *     order carries at least one, so empty only when the source omitted all lines
 *
 * @since 0.1.0
 */
public record AdvanceOrder(BigDecimal orderValue, List<OrderLine> lines) {

    private static final String ERR_NULL_VALUE = "orderValue must not be null";

    public AdvanceOrder {
        Objects.requireNonNull(orderValue, ERR_NULL_VALUE);
        lines = List.copyOf(lines);
    }
}
