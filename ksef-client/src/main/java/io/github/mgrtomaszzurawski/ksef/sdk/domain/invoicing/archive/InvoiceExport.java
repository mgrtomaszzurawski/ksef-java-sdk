/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;

/**
 * Invoice export jobs — start an export, poll its status, then download
 * and decrypt the resulting package via the {@link PreparedInvoiceExport}
 * handle returned by {@link #prepare(InvoiceQueryRequest, ExportScope)}.
 *
 * <p>Reached via {@link Invoices#export()}.
 *
 * @since 1.0.0
 */
public interface InvoiceExport {

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
     * @param scope {@link ExportScope#FULL_CONTENT} for full-content export;
     *     {@link ExportScope#METADATA_ONLY} for metadata-only
     * @return prepared-export handle
     */
    PreparedInvoiceExport prepare(InvoiceQueryRequest query, ExportScope scope);

    /**
     * Get the status of an invoice export job.
     *
     * @param referenceNumber the export reference number from
     *     {@link #prepare(InvoiceQueryRequest, ExportScope)}
     * @return export status with download URL when complete
     */
    InvoiceExportStatus getStatus(String referenceNumber);
}
