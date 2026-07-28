/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * One data row of an invoice-attachment table — the typed view of a
 * {@code Zalacznik/BlokDanych/Tabela/Wiersz} entry. The cell values
 * ({@code WKom}) align positionally with the table's {@link TableColumn}
 * headers.
 *
 * @param cells the row's cell values ({@code WKom}); at least one per row
 *
 * @since 0.1.0
 */
public record TableRow(List<String> cells) {

    public TableRow {
        cells = List.copyOf(cells);
    }
}
