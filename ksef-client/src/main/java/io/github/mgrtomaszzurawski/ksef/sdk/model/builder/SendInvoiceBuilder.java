/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;

import java.security.PublicKey;
import java.util.Objects;

/**
 * Builder for sending an invoice within a session.
 * <p>
 * Handles AES encryption of invoice content and SHA-256 hashing automatically.
 * <p>
 * Usage:
 * <pre>{@code
 * var request = SendInvoiceBuilder.create(invoiceXmlBytes, encryptionPublicKey)
 *     .build();
 * }</pre>
 */
public final class SendInvoiceBuilder {

    private final byte[] invoiceContent;
    private final PublicKey ksefPublicKey;
    private boolean offlineMode;

    private SendInvoiceBuilder(byte[] invoiceContent, PublicKey ksefPublicKey) {
        this.invoiceContent = Objects.requireNonNull(invoiceContent, "invoiceContent is required");
        this.ksefPublicKey = Objects.requireNonNull(ksefPublicKey, "ksefPublicKey is required");
    }

    /**
     * Create a builder for sending an invoice.
     *
     * @param invoiceXml raw invoice XML bytes
     * @param ksefPublicKey the KSeF public key (SymmetricKeyEncryption usage)
     */
    public static SendInvoiceBuilder create(byte[] invoiceXml, PublicKey ksefPublicKey) {
        return new SendInvoiceBuilder(invoiceXml, ksefPublicKey);
    }

    /**
     * Mark this invoice as submitted in offline mode.
     */
    public SendInvoiceBuilder offline() {
        this.offlineMode = true;
        return this;
    }

    /**
     * Build the send invoice request. Encrypts the invoice content with AES,
     * wraps the AES key with RSA, and computes SHA-256 hashes automatically.
     *
     * @return the request ready to pass to {@code SessionClient.sendInvoice()}
     */
    public SendInvoiceRequestRaw build() {
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedContent = CryptoService.encryptAes(invoiceContent, aesKey, initVector);

        byte[] invoiceHash = computeSha256(invoiceContent);
        byte[] encryptedHash = computeSha256(encryptedContent);

        SendInvoiceRequestRaw request = new SendInvoiceRequestRaw();
        request.setInvoiceHash(invoiceHash);
        request.setInvoiceSize((long) invoiceContent.length);
        request.setEncryptedInvoiceHash(encryptedHash);
        request.setEncryptedInvoiceSize((long) encryptedContent.length);
        request.setEncryptedInvoiceContent(encryptedContent);
        request.setOfflineMode(offlineMode);
        return request;
    }

    private static byte[] computeSha256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
