/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * SDK request payload for {@code SessionClient.sendInvoice(...)}.
 * Holds the precomputed hashes and AES-encrypted content; the SDK uploads
 * the bytes verbatim to KSeF.
 */
public record SendInvoiceRequest(
        byte[] invoiceHash,
        long invoiceSize,
        byte[] encryptedInvoiceHash,
        long encryptedInvoiceSize,
        byte[] encryptedInvoiceContent,
        boolean offlineMode) {

    public SendInvoiceRequest {
        Objects.requireNonNull(invoiceHash, "invoiceHash");
        Objects.requireNonNull(encryptedInvoiceHash, "encryptedInvoiceHash");
        Objects.requireNonNull(encryptedInvoiceContent, "encryptedInvoiceContent");
        invoiceHash = invoiceHash.clone();
        encryptedInvoiceHash = encryptedInvoiceHash.clone();
        encryptedInvoiceContent = encryptedInvoiceContent.clone();
    }

    @Override
    public byte[] invoiceHash() { return invoiceHash.clone(); }

    @Override
    public byte[] encryptedInvoiceHash() { return encryptedInvoiceHash.clone(); }

    @Override
    public byte[] encryptedInvoiceContent() { return encryptedInvoiceContent.clone(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SendInvoiceRequest that)) {
            return false;
        }
        return invoiceSize == that.invoiceSize
                && encryptedInvoiceSize == that.encryptedInvoiceSize
                && offlineMode == that.offlineMode
                && Arrays.equals(invoiceHash, that.invoiceHash)
                && Arrays.equals(encryptedInvoiceHash, that.encryptedInvoiceHash)
                && Arrays.equals(encryptedInvoiceContent, that.encryptedInvoiceContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceSize, encryptedInvoiceSize, offlineMode,
                Arrays.hashCode(invoiceHash),
                Arrays.hashCode(encryptedInvoiceHash),
                Arrays.hashCode(encryptedInvoiceContent));
    }
}
