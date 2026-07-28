/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One data block of an invoice attachment — the typed view of a
 * {@code Zalacznik/BlokDanych} entry. Carries an optional header, its
 * key/value descriptors, free-text paragraphs and tables.
 *
 * @param header block header ({@code ZNaglowek}); null when not supplied
 * @param metadata key/value descriptors ({@code MetaDane})
 * @param paragraphs free-text paragraphs ({@code Tekst/Akapit}); empty when none
 * @param tables tables in the block ({@code Tabela}); empty when none
 *
 * @since 0.1.0
 */
public record AttachmentBlock(
        @Nullable String header,
        List<AttachmentMetadata> metadata,
        List<String> paragraphs,
        List<AttachmentTable> tables) {

    public AttachmentBlock {
        metadata = List.copyOf(metadata);
        paragraphs = List.copyOf(paragraphs);
        tables = List.copyOf(tables);
    }
}
