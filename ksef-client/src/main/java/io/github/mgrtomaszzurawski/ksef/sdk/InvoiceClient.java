/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateRangeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.model.InvoiceMetadataResult;

import java.util.ArrayList;
import java.util.List;

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
    public InvoiceMetadataResult queryMetadata(InvoiceQueryFiltersRaw filters) {
        String token = sessionContext.token();
        QueryInvoicesMetadataResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_METADATA, filters, token,
                QueryInvoicesMetadataResponseRaw.class, OP_QUERY_METADATA);
        return InvoiceMetadataResult.from(raw);
    }

    /**
     * Query all invoice metadata matching the filters, automatically fetching
     * subsequent pages until no more results are available.
     * <p>
     * Uses the permanentStorageHwmDate from each response as a cursor to
     * narrow the date range for the next page.
     * <p>
     * Warning: this can return a large number of results. Consider using
     * {@link #queryMetadata(InvoiceQueryFiltersRaw)} for single-page queries
     * if you only need the first page.
     *
     * @param filters the query filter criteria
     * @return all matching invoice metadata across all pages
     */
    public List<InvoiceMetadata> queryAllMetadata(InvoiceQueryFiltersRaw filters) {
        List<InvoiceMetadata> allInvoices = new ArrayList<>();
        InvoiceQueryFiltersRaw currentFilters = filters;

        while (true) {
            InvoiceMetadataResult page = queryMetadata(currentFilters);
            allInvoices.addAll(page.invoices());

            if (!page.hasMore() || page.permanentStorageHwmDate() == null) {
                break;
            }

            // Use permanentStorageHwmDate as cursor — narrow dateRange.from for next page
            InvoiceQueryDateRangeRaw nextDateRange = new InvoiceQueryDateRangeRaw()
                    .dateType(filters.getDateRange().getDateType())
                    .from(page.permanentStorageHwmDate());
            if (filters.getDateRange().getTo() != null) {
                nextDateRange.to(filters.getDateRange().getTo());
            }
            currentFilters = new InvoiceQueryFiltersRaw()
                    .subjectType(filters.getSubjectType())
                    .dateRange(nextDateRange);
        }

        return List.copyOf(allInvoices);
    }

    /**
     * Start an invoice export job.
     *
     * @param request the export request with date range and optional filters
     * @return response with the export reference number for status polling
     */
    public ExportInvoicesResult exportInvoices(InvoiceExportRequestRaw request) {
        String token = sessionContext.token();
        ExportInvoicesResponseRaw raw = http.postJsonAuthenticated(PATH_EXPORTS, request, token,
                ExportInvoicesResponseRaw.class, OP_EXPORT);
        return ExportInvoicesResult.from(raw);
    }

    /**
     * Get the status of an invoice export job.
     *
     * @param referenceNumber the export reference number from {@link #exportInvoices}
     * @return export status with download URL when complete
     */
    public InvoiceExportStatus getExportStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        InvoiceExportStatusResponseRaw raw = http.getAuthenticated(PATH_EXPORT_STATUS + referenceNumber, token,
                InvoiceExportStatusResponseRaw.class, OP_EXPORT_STATUS);
        return InvoiceExportStatus.from(raw);
    }
}
