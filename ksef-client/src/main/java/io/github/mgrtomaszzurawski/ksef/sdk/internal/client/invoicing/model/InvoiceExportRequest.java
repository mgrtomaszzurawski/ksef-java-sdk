/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import java.util.Arrays;
import java.util.Objects;

/**
 * SDK request payload for {@code InvoiceClient.exportInvoices(...)}.
 * Carries the per-export AES key (RSA-encrypted with KSeF's public key) and
 * the query filters describing which invoices to export.
 *
 * @since 1.0.0
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
        encryptedSymmetricKey = encryptedSymmetricKey.clone();
        initVector = initVector.clone();
    }

    @Override
    public byte[] encryptedSymmetricKey() { return encryptedSymmetricKey.clone(); }

    @Override
    public byte[] initVector() { return initVector.clone(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InvoiceExportRequest that)) {
            return false;
        }
        return onlyMetadata == that.onlyMetadata
                && Arrays.equals(encryptedSymmetricKey, that.encryptedSymmetricKey)
                && Arrays.equals(initVector, that.initVector)
                && Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onlyMetadata, filters,
                Arrays.hashCode(encryptedSymmetricKey),
                Arrays.hashCode(initVector));
    }

    @Override
    public String toString() {
        return "InvoiceExportRequest[encryptedSymmetricKey=" + encryptedSymmetricKey.length + " bytes"
                + ", initVector=" + initVector.length + " bytes"
                + ", onlyMetadata=" + onlyMetadata
                + ", filters=" + filters + "]";
    }
}
