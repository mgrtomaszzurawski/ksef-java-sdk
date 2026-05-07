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
 *
 * <p>{@code hashOfCorrectedInvoice} is non-null only for technical
 * corrections (korekta techniczna) per
 * {@code ksef-docs/offline/korekta-techniczna.md}. For normal sends it
 * stays {@code null} and is omitted from the wire request.
 *
 * @since 1.0.0
 */
public record SendInvoiceRequest(
        byte[] invoiceHash,
        long invoiceSize,
        byte[] encryptedInvoiceHash,
        long encryptedInvoiceSize,
        byte[] encryptedInvoiceContent,
        boolean offlineMode,
        byte[] hashOfCorrectedInvoice) {

    public SendInvoiceRequest {
        Objects.requireNonNull(invoiceHash, "invoiceHash");
        Objects.requireNonNull(encryptedInvoiceHash, "encryptedInvoiceHash");
        Objects.requireNonNull(encryptedInvoiceContent, "encryptedInvoiceContent");
        invoiceHash = invoiceHash.clone();
        encryptedInvoiceHash = encryptedInvoiceHash.clone();
        encryptedInvoiceContent = encryptedInvoiceContent.clone();
        hashOfCorrectedInvoice = hashOfCorrectedInvoice == null ? null : hashOfCorrectedInvoice.clone();
    }

    /** Convenience 6-arg constructor — defaults {@code hashOfCorrectedInvoice} to {@code null}. */
    public SendInvoiceRequest(byte[] invoiceHash, long invoiceSize,
                              byte[] encryptedInvoiceHash, long encryptedInvoiceSize,
                              byte[] encryptedInvoiceContent, boolean offlineMode) {
        this(invoiceHash, invoiceSize, encryptedInvoiceHash, encryptedInvoiceSize,
                encryptedInvoiceContent, offlineMode, null);
    }

    @Override
    public byte[] invoiceHash() { return invoiceHash.clone(); }

    @Override
    public byte[] encryptedInvoiceHash() { return encryptedInvoiceHash.clone(); }

    @Override
    public byte[] encryptedInvoiceContent() { return encryptedInvoiceContent.clone(); }

    @Override
    public byte[] hashOfCorrectedInvoice() {
        return hashOfCorrectedInvoice == null ? null : hashOfCorrectedInvoice.clone();
    }

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
                && Arrays.equals(encryptedInvoiceContent, that.encryptedInvoiceContent)
                && Arrays.equals(hashOfCorrectedInvoice, that.hashOfCorrectedInvoice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceSize, encryptedInvoiceSize, offlineMode,
                Arrays.hashCode(invoiceHash),
                Arrays.hashCode(encryptedInvoiceHash),
                Arrays.hashCode(encryptedInvoiceContent),
                Arrays.hashCode(hashOfCorrectedInvoice));
    }

    @Override
    public String toString() {
        return "SendInvoiceRequest[invoiceSize=" + invoiceSize
                + ", encryptedInvoiceSize=" + encryptedInvoiceSize
                + ", offlineMode=" + offlineMode
                + ", invoiceHash=" + bytesLabel(invoiceHash)
                + ", encryptedInvoiceHash=" + bytesLabel(encryptedInvoiceHash)
                + ", encryptedInvoiceContent=" + bytesLabel(encryptedInvoiceContent)
                + ", hashOfCorrectedInvoice="
                + (hashOfCorrectedInvoice == null ? "null" : bytesLabel(hashOfCorrectedInvoice))
                + "]";
    }

    private static String bytesLabel(byte[] bytes) {
        return bytes.length + " bytes";
    }
}
