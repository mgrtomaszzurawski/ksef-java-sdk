/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A table within an invoice-attachment data block — the typed view of a
 * {@code Zalacznik/BlokDanych/Tabela} entry. Columns describe the header;
 * each {@link TableRow} carries cells aligned to those columns; totals
 * carry the optional summary row.
 *
 * @param metadata table-level key/value descriptors ({@code TMetaDane}); empty when none
 * @param description table description ({@code Opis}); null when not supplied
 * @param columns column headers ({@code TNaglowek/Kol})
 * @param rows data rows ({@code Wiersz})
 * @param totals summary cells ({@code Suma/SKom}); empty when no summary row
 *
 * @since 0.1.0
 */
public record AttachmentTable(
        List<AttachmentMetadata> metadata,
        @Nullable String description,
        List<TableColumn> columns,
        List<TableRow> rows,
        List<String> totals) {

    public AttachmentTable {
        metadata = List.copyOf(metadata);
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        totals = List.copyOf(totals);
    }
}
