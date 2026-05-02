/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

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
    }
}
