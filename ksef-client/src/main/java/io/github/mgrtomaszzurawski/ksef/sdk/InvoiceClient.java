/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

import static io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
 */
public final class InvoiceClient {

    private static final String PATH_INVOICES_KSEF = "/api/v2/invoices/ksef/";
    private static final String PATH_QUERY_METADATA = "/api/v2/invoices/query/metadata";
    private static final String PATH_EXPORTS = "/api/v2/invoices/exports";
    private static final String PATH_EXPORT_STATUS = "/api/v2/invoices/exports/";

    private static final String OP_GET_BY_KSEF = "getInvoiceByKsefNumber";
    private static final String OP_QUERY_METADATA = "queryInvoicesMetadata";
    private static final String OP_EXPORT = "exportInvoices";
    private static final String OP_EXPORT_STATUS = "getExportStatus";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public InvoiceClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Retrieve an invoice by its KSeF number. Returns raw invoice XML bytes.
     *
     * @param ksefNumber the unique KSeF invoice number
     * @return raw invoice XML bytes
     */
    public byte[] getByKsefNumber(String ksefNumber) {
        requireSafePathSegment(ksefNumber);
        String token = sessionContext.token();
        return http.getAuthenticatedBytes(PATH_INVOICES_KSEF + ksefNumber, token, OP_GET_BY_KSEF);
    }

    /**
     * Query invoice metadata with filters (date range, buyer/seller, amounts, etc.).
     *
     * @param filters the query filter criteria
     * @return paginated list of invoice metadata
     */
    public QueryInvoicesMetadataResponseRaw queryMetadata(InvoiceQueryFiltersRaw filters) {
        String token = sessionContext.token();
        return http.postJsonAuthenticated(PATH_QUERY_METADATA, filters, token,
                QueryInvoicesMetadataResponseRaw.class, OP_QUERY_METADATA);
    }

    /**
     * Start an invoice export job.
     *
     * @param request the export request with date range and optional filters
     * @return response with the export reference number for status polling
     */
    public ExportInvoicesResponseRaw exportInvoices(InvoiceExportRequestRaw request) {
        String token = sessionContext.token();
        return http.postJsonAuthenticated(PATH_EXPORTS, request, token,
                ExportInvoicesResponseRaw.class, OP_EXPORT);
    }

    /**
     * Get the status of an invoice export job.
     *
     * @param referenceNumber the export reference number from {@link #exportInvoices}
     * @return export status with download URL when complete
     */
    public InvoiceExportStatusResponseRaw getExportStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_EXPORT_STATUS + referenceNumber, token,
                InvoiceExportStatusResponseRaw.class, OP_EXPORT_STATUS);
    }
}
