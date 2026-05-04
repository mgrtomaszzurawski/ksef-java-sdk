/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Builder for sending an invoice within an open session.
 * <p>
 * The invoice must be encrypted with the SAME AES key that was used to open the session.
 * This builder handles AES encryption and SHA-256 hashing automatically.
 * <p>
 * Usage:
 * <pre>{@code
 * var request = SendInvoiceBuilder.create(invoiceXmlBytes, sessionAesKey, sessionIv)
 *     .build();
 * }</pre>
 */
public final class SendInvoiceBuilder {

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String ERR_NULL_INVOICE_CONTENT = "invoiceContent is required";
    private static final String ERR_NULL_AES_KEY = "aesKey is required";
    private static final String ERR_NULL_INIT_VECTOR = "initVector is required";
    private static final String ERR_SHA256_UNAVAILABLE = "SHA-256 not available";

    private final byte[] invoiceContent;
    private final byte[] aesKey;
    private final byte[] initVector;
    private boolean offlineMode;
    private byte[] hashOfCorrectedInvoice;

    private SendInvoiceBuilder(byte[] invoiceContent, byte[] aesKey, byte[] initVector) {
        this.invoiceContent = Objects.requireNonNull(invoiceContent, ERR_NULL_INVOICE_CONTENT);
        this.aesKey = Objects.requireNonNull(aesKey, ERR_NULL_AES_KEY);
        this.initVector = Objects.requireNonNull(initVector, ERR_NULL_INIT_VECTOR);
    }

    /**
     * Create a builder for sending an invoice.
     *
     * @param invoiceXml raw invoice XML bytes
     * @param aesKey the AES key used when opening the session
     * @param initVector the initialization vector used when opening the session
     */
    public static SendInvoiceBuilder create(byte[] invoiceXml, byte[] aesKey, byte[] initVector) {
        return new SendInvoiceBuilder(invoiceXml, aesKey, initVector);
    }

    /**
     * Mark this invoice as submitted in offline mode.
     */
    public SendInvoiceBuilder offline() {
        this.offlineMode = true;
        return this;
    }

    /**
     * Mark this invoice as a technical correction (korekta techniczna) of
     * an earlier invoice. {@code hashOfCorrected} is the SHA-256 of the
     * original invoice's XML content. Implies {@link #offline()} per spec
     * ({@code ksef-docs/offline/korekta-techniczna.md}).
     *
     * <p>Spec citation: REQ-OFFLINE-003.
     */
    public SendInvoiceBuilder technicalCorrection(byte[] hashOfCorrected) {
        Objects.requireNonNull(hashOfCorrected, "hashOfCorrected is required");
        this.hashOfCorrectedInvoice = hashOfCorrected.clone();
        this.offlineMode = true;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public SendInvoiceBuilder toBuilder() {
        SendInvoiceBuilder copy = new SendInvoiceBuilder(this.invoiceContent, this.aesKey, this.initVector);
        copy.offlineMode = this.offlineMode;
        copy.hashOfCorrectedInvoice = this.hashOfCorrectedInvoice == null
                ? null : this.hashOfCorrectedInvoice.clone();
        return copy;
    }

    /**
     * Build the send invoice request. Encrypts the invoice content with the
     * session's AES key and computes SHA-256 hashes automatically.
     *
     * @return the request ready to pass to {@code SessionClient.sendInvoice()}
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public SendInvoiceRequest build() {
        byte[] encryptedContent = CryptoService.encryptAes(invoiceContent, aesKey, initVector);
        byte[] invoiceHash = computeSha256(invoiceContent);
        byte[] encryptedHash = computeSha256(encryptedContent);
        return new SendInvoiceRequest(
                invoiceHash,
                invoiceContent.length,
                encryptedHash,
                encryptedContent.length,
                encryptedContent,
                offlineMode,
                hashOfCorrectedInvoice);
    }

    private static byte[] computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new IllegalStateException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        }
    }
}
