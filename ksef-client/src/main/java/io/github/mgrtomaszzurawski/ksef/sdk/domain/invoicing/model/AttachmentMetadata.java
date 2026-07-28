/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * One key/value descriptor of an invoice attachment — the typed view of a
 * {@code Zalacznik/BlokDanych/MetaDane} entry (or the table-level
 * {@code Tabela/TMetaDane} entry). Both members are mandatory in the XSD.
 *
 * @param key descriptor key ({@code ZKlucz} / {@code TKlucz})
 * @param value descriptor value ({@code ZWartosc} / {@code TWartosc})
 *
 * @since 0.1.0
 */
public record AttachmentMetadata(String key, String value) {

    private static final String ERR_NULL_KEY = "key must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";

    public AttachmentMetadata {
        Objects.requireNonNull(key, ERR_NULL_KEY);
        Objects.requireNonNull(value, ERR_NULL_VALUE);
    }
}
