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
import java.util.List;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
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

    InvoiceMetadataResult queryMetadata(InvoiceQueryBuilder query);
    List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query);
    List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query, int maxResults);

    /**
     * Low-level "start export" entry point. Caller must already hold the KSeF
     * symmetric-key public key, generate AES key + IV, and retain them
     * outside the SDK in order to decrypt the returned package later.
     *
     * @deprecated For most consumers, prefer
     *     {@link #prepareExport(InvoiceQueryBuilder, boolean)} which retains
     *     the AES key + IV inside the returned {@link PreparedInvoiceExport}
     *     handle and exposes a polling/download/decrypt workflow.
     */
    @Deprecated(since = "0.1.0")
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
