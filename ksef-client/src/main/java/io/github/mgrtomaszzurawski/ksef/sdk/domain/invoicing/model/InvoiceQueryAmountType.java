/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Which monetary axis the {@code amount} filter on
 * {@code POST /invoices/query/metadata} should be applied to.
 *
 * <p>Mirrors the OpenAPI {@code AmountType} enum.
 *
 * @since 0.1.0
 */
public enum InvoiceQueryAmountType {

    /** Gross amount (Polish: <em>brutto</em>) — net + VAT. */
    BRUTTO,
    /** Net amount (Polish: <em>netto</em>) — pre-VAT. */
    NETTO,
    /** VAT amount only. */
    VAT;
}
