/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * SDK request payload for {@code InvoiceClient.exportInvoices(...)}.
 * Carries the per-export AES key (RSA-encrypted with KSeF's public key) and
 * the query filters describing which invoices to export.
 */
public record InvoiceExportRequest(
        byte[] encryptedSymmetricKey,
        byte[] initVector,
        boolean onlyMetadata,
        InvoiceQueryFilters filters) {

    public InvoiceExportRequest {
        Objects.requireNonNull(encryptedSymmetricKey, "encryptedSymmetricKey");
        Objects.requireNonNull(initVector, "initVector");
        Objects.requireNonNull(filters, "filters");
    }
}
