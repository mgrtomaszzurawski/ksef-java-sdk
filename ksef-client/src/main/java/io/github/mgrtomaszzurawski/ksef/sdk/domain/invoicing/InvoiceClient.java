/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
 *
 * @since 1.0.0
 */
public interface InvoiceClient {

    /**
     * Retrieve invoice XML by KSeF number. Validates length, format, and
     * CRC-8 checksum (REQ-SESS-18/19/20) before the network call.
     */
    byte[] getByKsefNumber(KsefNumber ksefNumber);

    /**
     * Convenience overload that parses the raw string into a
     * {@link KsefNumber} before delegating. Throws
     * {@link IllegalArgumentException} on invalid input.
     */
    default byte[] getByKsefNumber(String ksefNumber) {
        return getByKsefNumber(KsefNumber.parse(ksefNumber));
    }

    InvoiceMetadataResult queryInvoicesByMetadata(InvoiceQueryBuilder query);

    /**
     * Stream every invoice metadata record matching the filter, walking
     * the date-cursor + page-offset model used by KSeF's
     * {@code POST /invoices/query/metadata}. Pages are fetched lazily;
     * caller controls memory pressure by limiting / collecting
     * downstream.
     */
    java.util.stream.Stream<InvoiceMetadata> streamInvoicesByMetadata(InvoiceQueryBuilder query);

    /**
     * Low-level "start export" entry point — Tier-3 advanced API per
     * ADR-021. Caller manages the KSeF symmetric-key public-key fetch,
     * AES key + IV generation, and key retention out-of-band so the
     * returned export package can be decrypted later.
     *
     * <p>Most consumers should use
     * {@link #prepareExport(InvoiceQueryBuilder, boolean)} instead — it
     * retains the AES key + IV inside a {@link PreparedInvoiceExport}
     * handle and exposes the full poll/download/decrypt workflow with
     * automatic crypto material zeroisation on close.
     *
     * @apiNote Advanced. Use {@code prepareExport(...)} unless you have
     *     a specific reason to manage the symmetric key material yourself
     *     (e.g. integration with an external HSM).
     */
    ExportInvoicesResult exportInvoices(InvoiceExportBuilder exportBuilder);

    InvoiceExportStatus getExportStatus(String referenceNumber);

    /**
     * Start an invoice export and return a {@link PreparedInvoiceExport} handle
     * that retains the AES key + IV needed to decrypt the returned package.
     *
     * <p>The SDK fetches the KSeF symmetric-key public key, generates the AES
     * key + IV, encrypts the AES key with the KSeF public key, sends the export
     * request, and retains the plaintext AES/IV inside the returned handle so
     * the resulting package can be downloaded and decrypted via
     * {@link PreparedInvoiceExport#downloadAndDecrypt(InvoiceExportStatus)}.
     *
     * @param query filters identifying invoices to export
     * @param fullContent {@code true} for full-content export; {@code false} for
     *     metadata-only
     * @return prepared-export handle
     */
    PreparedInvoiceExport prepareExport(InvoiceQueryBuilder query, boolean fullContent);
}
