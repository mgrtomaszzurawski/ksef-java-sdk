/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Scope of a KSeF invoice export. Replaces the prior
 * {@code boolean fullContent} parameter on
 * {@code Invoices.export().prepare(...)} so call sites read as
 * {@code export().prepare(query, ExportScope.FULL_CONTENT)} rather than
 * the boolean-trap {@code export().prepare(query, true)}.
 *
 * @since 1.0.0
 */
public enum ExportScope {

    /**
     * Full content export — the resulting package includes the canonical
     * invoice XML bytes per matching invoice, plus the metadata records.
     */
    FULL_CONTENT,

    /**
     * Metadata-only export — the resulting package includes the per-invoice
     * metadata records but not the invoice XML payload. Faster to download
     * and smaller; use when only KSeF numbers / acceptance dates / amounts
     * are needed without the underlying invoice content.
     */
    METADATA_ONLY
}
