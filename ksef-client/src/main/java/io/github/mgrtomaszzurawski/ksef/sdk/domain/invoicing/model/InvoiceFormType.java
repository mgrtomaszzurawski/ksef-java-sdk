/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Form-type filter for {@code POST /invoices/query/metadata}.
 *
 * <p>Mirrors the OpenAPI {@code InvoiceQueryFormType} enum:
 * <ul>
 *   <li>{@link #FA} — Polish KSeF FA (faktura) schema</li>
 *   <li>{@link #PEF} — PEF (Pan-European e-invoice)</li>
 *   <li>{@link #RR} — RR (faktury rolnicze)</li>
 *   <li>{@link #FA_RR} — combined FA + RR scope</li>
 * </ul>
 *
 * @since 0.1.0
 */
public enum InvoiceFormType {
    FA,
    PEF,
    RR,
    FA_RR;
}
