/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link OfflineInvoice}.
 *
 * <p>The {@link OfflineInvoice#fromInvoice} static factory covers the
 * common-case (regular offline issuance, no correction). Use this
 * builder when:
 * <ul>
 *   <li>You need to set {@link #hashOfCorrectedInvoice(byte[])} for a
 *       technical correction issued during an offline window
 *       (REQ-OFFLINE-004).</li>
 *   <li>You want to keep the invoice + certificate + mode + KOD II
 *       envelope assembly in one step at construction time.</li>
 * </ul>
 *
 * <p>Required fields validated at {@link #build()} time
 * ({@link IllegalStateException} on missing): underlyingInvoice,
 * signingCertificate, offlineMode, qrEnvironment, contextType,
 * contextValue, sellerNip, issueDate.
 *
 * @since 1.0.0
 */
public final class OfflineInvoiceBuilder<I extends Invoice> {

    private static final String ERR_NULL_INVOICE = "underlyingInvoice must not be null";
    private static final String ERR_NULL_CERT = "signingCertificate must not be null";
    private static final String ERR_NULL_MODE = "offlineMode must not be null";
    private static final String ERR_NULL_QR_ENV = "qrEnvironment must not be null";
    private static final String ERR_NULL_CONTEXT_TYPE = "contextType must not be null";
    private static final String ERR_NULL_CONTEXT_VALUE = "contextValue must not be null";
    private static final String ERR_NULL_SELLER_NIP = "sellerNip must not be null";
    private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
    private static final String ERR_NULL_HASH = "hashOfCorrectedInvoice must not be null";

    private static final String ERR_REQUIRED_CERT =
            "signingCertificate is required — call signingCertificate(KsefCertificate) before build()";
    private static final String ERR_REQUIRED_MODE =
            "offlineMode is required — call offlineMode(OfflineMode) before build()";
    private static final String ERR_REQUIRED_QR_ENV =
            "qrEnvironment is required — call qrEnvironment(QrEnvironment) before build()";
    private static final String ERR_REQUIRED_CONTEXT_TYPE =
            "contextType is required — call contextType(QrContextType) before build()";
    private static final String ERR_REQUIRED_CONTEXT_VALUE =
            "contextValue is required — call contextValue(String) before build()";
    private static final String ERR_REQUIRED_SELLER_NIP =
            "sellerNip is required — call sellerNip(String) before build()";
    private static final String ERR_REQUIRED_ISSUE_DATE =
            "issueDate is required — call issueDate(LocalDate) before build()";

    /** SHA-256 length in bytes — KSeF requires the corrected-invoice hash to be exactly 32 bytes. */
    private static final int SHA256_LENGTH_BYTES = 32;
    private static final String ERR_HASH_LENGTH =
            "hashOfCorrectedInvoice must be exactly " + SHA256_LENGTH_BYTES
                    + " bytes (SHA-256), got %d";

    private final I underlyingInvoice;
    private @Nullable KsefCertificate signingCertificate;
    private @Nullable OfflineMode offlineMode;
    private @Nullable QrEnvironment qrEnvironment;
    private @Nullable QrContextType contextType;
    private @Nullable String contextValue;
    private @Nullable String sellerNip;
    private @Nullable LocalDate issueDate;
    private byte @Nullable [] hashOfCorrectedInvoice;

    private OfflineInvoiceBuilder(I invoice) {
        this.underlyingInvoice = Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
    }

    /** Entry point: start building an {@link OfflineInvoice} for {@code invoice}. */
    public static <I extends Invoice> OfflineInvoiceBuilder<I> forInvoice(I invoice) {
        return new OfflineInvoiceBuilder<>(invoice);
    }

    /** Required: KSeF Offline certificate that signs the KOD II QR. */
    public OfflineInvoiceBuilder<I> signingCertificate(KsefCertificate value) {
        this.signingCertificate = Objects.requireNonNull(value, ERR_NULL_CERT);
        return this;
    }

    /** Required: offline-mode classification (consumer-chosen / unavailability / emergency). */
    public OfflineInvoiceBuilder<I> offlineMode(OfflineMode value) {
        this.offlineMode = Objects.requireNonNull(value, ERR_NULL_MODE);
        return this;
    }

    /** Required: QR environment whose host is embedded into KOD I + KOD II URLs. */
    public OfflineInvoiceBuilder<I> qrEnvironment(QrEnvironment value) {
        this.qrEnvironment = Objects.requireNonNull(value, ERR_NULL_QR_ENV);
        return this;
    }

    /** Required: authorising context identifier kind for KOD II URL. */
    public OfflineInvoiceBuilder<I> contextType(QrContextType value) {
        this.contextType = Objects.requireNonNull(value, ERR_NULL_CONTEXT_TYPE);
        return this;
    }

    /** Required: authorising context identifier value for KOD II URL. */
    public OfflineInvoiceBuilder<I> contextValue(String value) {
        this.contextValue = Objects.requireNonNull(value, ERR_NULL_CONTEXT_VALUE);
        return this;
    }

    /** Required: 10-digit seller NIP embedded in KOD I + KOD II URLs. */
    public OfflineInvoiceBuilder<I> sellerNip(String value) {
        this.sellerNip = Objects.requireNonNull(value, ERR_NULL_SELLER_NIP);
        return this;
    }

    /** Required: calendar issue date embedded in KOD I URL. */
    public OfflineInvoiceBuilder<I> issueDate(LocalDate value) {
        this.issueDate = Objects.requireNonNull(value, ERR_NULL_ISSUE_DATE);
        return this;
    }

    /**
     * Optional: SHA-256 hash of the corrected (original) invoice when
     * this offline issuance is also a technical correction. Must be
     * exactly 32 bytes (SHA-256 length); rejected otherwise.
     */
    public OfflineInvoiceBuilder<I> hashOfCorrectedInvoice(byte[] value) {
        Objects.requireNonNull(value, ERR_NULL_HASH);
        if (value.length != SHA256_LENGTH_BYTES) {
            throw new IllegalArgumentException(String.format(ERR_HASH_LENGTH, value.length));
        }
        this.hashOfCorrectedInvoice = value.clone();
        return this;
    }

    /**
     * Build the {@link OfflineInvoice}. Validates that all required
     * fields are set; computes the invoice hash, builds KOD I, signs
     * KOD II with {@code signingCertificate}'s private key, and
     * renders both QR codes as PNG bytes.
     *
     * @throws IllegalStateException if a required field has not been set
     */
    public OfflineInvoice<I> build() {
        KsefCertificate certificate = required(signingCertificate, ERR_REQUIRED_CERT);
        OfflineMode mode = required(offlineMode, ERR_REQUIRED_MODE);
        QrEnvironment environment = required(qrEnvironment, ERR_REQUIRED_QR_ENV);
        QrContextType type = required(contextType, ERR_REQUIRED_CONTEXT_TYPE);
        String value = required(contextValue, ERR_REQUIRED_CONTEXT_VALUE);
        String nip = required(sellerNip, ERR_REQUIRED_SELLER_NIP);
        LocalDate date = required(issueDate, ERR_REQUIRED_ISSUE_DATE);

        byte[] invoiceXml = underlyingInvoice.xml();
        byte[] invoiceHash = OfflineInvoice.computeSha256(invoiceXml);

        byte[] kodIPng = OfflineInvoice.renderKodI(environment, nip, date, invoiceHash);
        byte[] kodIIPng = OfflineInvoice.renderKodII(environment, type, value, nip,
                certificate, invoiceHash);

        return new OfflineInvoice<>(underlyingInvoice, invoiceXml, kodIPng, kodIIPng,
                mode, certificate, hashOfCorrectedInvoice);
    }

    private static <T> T required(@Nullable T candidate, String message) {
        if (candidate == null) {
            throw new IllegalStateException(message);
        }
        return candidate;
    }
}
