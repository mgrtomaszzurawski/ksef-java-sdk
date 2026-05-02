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
}
