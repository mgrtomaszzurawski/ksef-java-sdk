/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * One column header of an invoice-attachment table — the typed view of a
 * {@code Zalacznik/BlokDanych/Tabela/TNaglowek/Kol} entry. Both members
 * are mandatory in the XSD.
 *
 * @param label column label ({@code NKom})
 * @param type column value type ({@code @Typ}): one of
 *     {@code date}, {@code datetime}, {@code dec}, {@code int},
 *     {@code time}, {@code txt}
 *
 * @since 0.1.0
 */
public record TableColumn(String label, String type) {

    private static final String ERR_NULL_LABEL = "label must not be null";
    private static final String ERR_NULL_TYPE = "type must not be null";

    public TableColumn {
        Objects.requireNonNull(label, ERR_NULL_LABEL);
        Objects.requireNonNull(type, ERR_NULL_TYPE);
    }
}
