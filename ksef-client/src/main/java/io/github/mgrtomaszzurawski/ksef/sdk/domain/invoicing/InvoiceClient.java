/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

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

    byte[] getByKsefNumber(String ksefNumber);

    InvoiceMetadataResult queryMetadata(InvoiceQueryBuilder query);
    List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query);
    List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query, int maxResults);
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
