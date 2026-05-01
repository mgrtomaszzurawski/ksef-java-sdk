/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.security.SecurityClient;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.CryptoService;

import java.security.PublicKey;
import java.util.Objects;

/**
 * Builder for invoice export requests. Handles AES key generation and
 * RSA encryption automatically.
 * <p>
 * Required: filters (use {@link InvoiceQueryBuilder}), KSeF public key for encryption.
 * <p>
 * Usage:
 * <pre>{@code
 * InvoiceExportRequestRaw request = InvoiceExportBuilder.create(publicKey)
 *     .filters(InvoiceQueryBuilder.seller()
 *         .invoicingDateFrom(date)
 *         .build())
 *     .metadataOnly()
 *     .build();
 * }</pre>
 */
public final class InvoiceExportBuilder {

    private static final String ERR_QUERY_BUILDER_REQUIRED = "queryBuilder is required";
    private static final String ERR_NULL_KSEF_PUBLIC_KEY = "ksefPublicKey is required";
    private static final String ERR_NULL_FILTERS = "filters are required — use .filters() before .build()";

    private final PublicKey ksefPublicKey;
    private InvoiceQueryFiltersRaw filters;
    private boolean onlyMetadata;

    private InvoiceExportBuilder(PublicKey ksefPublicKey) {
        this.ksefPublicKey = Objects.requireNonNull(ksefPublicKey, ERR_NULL_KSEF_PUBLIC_KEY);
    }

    /**
     * Create an export builder with the KSeF public key for encryption.
     *
     * @param ksefPublicKey the public key from SecurityClient (SymmetricKeyEncryption usage)
     */
    public static InvoiceExportBuilder create(PublicKey ksefPublicKey) {
        return new InvoiceExportBuilder(ksefPublicKey);
    }

    /**
     * Set the query filters for the export.
     *
     * @param queryBuilder query builder with filter criteria
     */
    public InvoiceExportBuilder filters(InvoiceQueryBuilder queryBuilder) {
        Objects.requireNonNull(queryBuilder, ERR_QUERY_BUILDER_REQUIRED);
        this.filters = queryBuilder.build();
        return this;
    }

    /**
     * Export metadata only (no invoice content).
     */
    public InvoiceExportBuilder metadataOnly() {
        this.onlyMetadata = true;
        return this;
    }

    /**
     * Export full invoice content (default).
     */
    public InvoiceExportBuilder fullContent() {
        this.onlyMetadata = false;
        return this;
    }

    /**
     * Build the export request. Generates AES key, IV, and encrypts the symmetric
     * key with the KSeF public key automatically.
     *
     * @return the request ready to pass to {@code InvoiceClient.exportInvoices()}
     * @throws IllegalStateException if filters are not set
     */
    public InvoiceExportRequestRaw build() {
        Objects.requireNonNull(filters, ERR_NULL_FILTERS);

        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, ksefPublicKey);

        return new InvoiceExportRequestRaw()
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .onlyMetadata(onlyMetadata)
                .filters(filters);
    }
}
