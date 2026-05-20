/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Buyer-identifier filter for {@code POST /invoices/query/metadata}.
 *
 * <p>Mirrors the OpenAPI {@code InvoiceQueryBuyerIdentifier} schema. The
 * type alone (e.g. {@link BuyerIdentifierType#NONE}) is enough to narrow
 * the result set; the {@code value} is optional for the {@code NONE} case.
 *
 * @param type the buyer identifier kind
 * @param value the identifier value (optional for {@code NONE})
 *
 * @since 0.1.0
 */
public record InvoiceQueryBuyerIdentifier(BuyerIdentifierType type, @Nullable String value) {

    public InvoiceQueryBuyerIdentifier {
        Objects.requireNonNull(type, "type");
    }
}
