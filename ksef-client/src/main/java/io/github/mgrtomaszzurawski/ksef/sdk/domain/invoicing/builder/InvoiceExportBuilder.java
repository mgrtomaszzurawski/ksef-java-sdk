/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.security.PublicKey;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for invoice export requests. Generates a fresh AES key and IV per
 * {@link #build()} call and encrypts the AES key with the supplied KSeF
 * public key. Required: filters (use {@link InvoiceQueryBuilder}).
 *
 * @since 1.0.0
 */
public final class InvoiceExportBuilder {

    private static final String ERR_QUERY_BUILDER_REQUIRED = "queryBuilder is required";
    private static final String ERR_NULL_KSEF_PUBLIC_KEY = "ksefPublicKey is required";
    private static final String ERR_NULL_FILTERS = "filters are required — use .filters() before .build()";

    private final PublicKey ksefPublicKey;
    private @Nullable InvoiceQueryFilters filters;
    private boolean onlyMetadata;

    private InvoiceExportBuilder(PublicKey ksefPublicKey) {
        this.ksefPublicKey = Objects.requireNonNull(ksefPublicKey, ERR_NULL_KSEF_PUBLIC_KEY);
    }

    public static InvoiceExportBuilder create(PublicKey ksefPublicKey) {
        return new InvoiceExportBuilder(ksefPublicKey);
    }

    public InvoiceExportBuilder filters(InvoiceQueryBuilder queryBuilder) {
        Objects.requireNonNull(queryBuilder, ERR_QUERY_BUILDER_REQUIRED);
        this.filters = queryBuilder.build();
        return this;
    }

    public InvoiceExportBuilder metadataOnly() {
        this.onlyMetadata = true;
        return this;
    }

    public InvoiceExportBuilder fullContent() {
        this.onlyMetadata = false;
        return this;
    }

    public InvoiceExportBuilder toBuilder() {
        InvoiceExportBuilder copy = new InvoiceExportBuilder(this.ksefPublicKey);
        copy.filters = this.filters;
        copy.onlyMetadata = this.onlyMetadata;
        return copy;
    }

    public InvoiceExportRequest build() {
        Objects.requireNonNull(filters, ERR_NULL_FILTERS);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, ksefPublicKey);
        return new InvoiceExportRequest(encryptedKey, initVector, onlyMetadata, filters);
    }
}
