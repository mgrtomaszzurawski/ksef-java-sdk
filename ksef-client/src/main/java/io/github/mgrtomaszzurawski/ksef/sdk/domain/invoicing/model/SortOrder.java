/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Result sort order for paginated invoice metadata queries (per
 * spec {@code pobieranie-faktur/pobieranie-faktur.md}). Maps to the
 * {@code sortOrder} query parameter on
 * {@code POST /invoices/query/metadata}; the spec default is
 * {@link #ASC ascending}.
 *
 * @since 1.0.0
 */
public enum SortOrder {

    /** Ascending — oldest record first. Spec default. */
    ASC("Asc"),

    /** Descending — newest record first. */
    DESC("Desc");

    private final String wireValue;

    SortOrder(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Wire value (PascalCase) expected by the KSeF API. */
    public String wireValue() {
        return wireValue;
    }
}
