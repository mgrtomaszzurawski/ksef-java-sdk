/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateRangeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
 */
public final class InvoiceClientImpl implements InvoiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceClientImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";
    private static final String LOG_CALL_MAX = "→ {} maxResults={}";

    private static final String PATH_INVOICES_KSEF = ApiPaths.INVOICES + "/ksef/";
    private static final String PATH_QUERY_METADATA = ApiPaths.INVOICES + "/query/metadata";
    private static final String PATH_EXPORTS = ApiPaths.INVOICES + "/exports";
    private static final String PATH_EXPORT_STATUS = ApiPaths.INVOICES + "/exports/";

    private static final String OP_GET_BY_KSEF = "getInvoiceByKsefNumber";
    private static final String OP_QUERY_METADATA = "queryInvoicesMetadata";
    private static final String OP_EXPORT = "exportInvoices";
    private static final String OP_EXPORT_STATUS = "getExportStatus";
    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NULL_EXPORT = "exportBuilder must not be null";
    private static final int DEFAULT_MAX_RESULTS = 10000;

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public InvoiceClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef.runtime());
        this.sessionContext = ksef.runtime().sessionContext();
    }

    /**
     * Retrieve an invoice by its KSeF number. Returns raw invoice XML bytes.
     *
     * @param ksefNumber the unique KSeF invoice number
     * @return raw invoice XML bytes
     */
    @Override
    public byte[] getByKsefNumber(String ksefNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_BY_KSEF, ksefNumber);
        requireSafePathSegment(ksefNumber);
        String token = sessionContext.token();
        return http.getAuthenticatedBytes(PATH_INVOICES_KSEF + ksefNumber, token, OP_GET_BY_KSEF);
    }

    /**
     * Query invoice metadata with filters (date range, buyer/seller, amounts, etc.).
     *
     * @param query the query builder with filter criteria
     * @return paginated list of invoice metadata
     */
    @Override
    public InvoiceMetadataResult queryMetadata(InvoiceQueryBuilder query) {
        LOGGER.debug(LOG_CALL, OP_QUERY_METADATA);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        return doQueryMetadata(InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query.build()));
    }

    /**
     * Query all invoice metadata matching the filters, automatically fetching
     * subsequent pages until no more results are available or maxResults is reached.
     * <p>
     * Uses the permanentStorageHwmDate from each response as a cursor to
     * narrow the date range for the next page. Default limit: 10,000 results.
     *
     * @param query the query builder with filter criteria
     * @return all matching invoice metadata across all pages (up to default limit)
     */
    @Override
    public List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query) {
        return queryAllMetadata(query, DEFAULT_MAX_RESULTS);
    }

    /**
     * Query invoice metadata with automatic pagination and explicit result limit.
     *
     * @param query the query builder with filter criteria
     * @param maxResults maximum number of results to return (safety limit)
     * @return matching invoice metadata across pages (up to maxResults)
     */
    @Override
    public List<InvoiceMetadata> queryAllMetadata(InvoiceQueryBuilder query, int maxResults) {
        LOGGER.debug(LOG_CALL_MAX, OP_QUERY_METADATA, maxResults);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        InvoiceQueryFiltersRaw filters = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query.build());
        List<InvoiceMetadata> allInvoices = new ArrayList<>();

        while (true) {
            InvoiceMetadataResult page = doQueryMetadata(filters);
            allInvoices.addAll(page.invoices());

            if (allInvoices.size() >= maxResults) {
                return List.copyOf(allInvoices.subList(0, maxResults));
            }

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
            filters = new InvoiceQueryFiltersRaw()
                    .subjectType(filters.getSubjectType())
                    .dateRange(nextDateRange);
        }

        return List.copyOf(allInvoices);
    }

    /**
     * Start an invoice export job.
     *
     * @param exportBuilder the export builder with date range and filters
     * @return response with the export reference number for status polling
     */
    @Override
    public ExportInvoicesResult exportInvoices(InvoiceExportBuilder exportBuilder) {
        LOGGER.debug(LOG_CALL, OP_EXPORT);
        Objects.requireNonNull(exportBuilder, ERR_NULL_EXPORT);
        InvoiceExportRequestRaw request = InvoicingRequestMappers.toInvoiceExportRequestRaw(exportBuilder.build());
        String token = sessionContext.token();
        ExportInvoicesResponseRaw raw = http.postJsonAuthenticated(PATH_EXPORTS, request, token,
                ExportInvoicesResponseRaw.class, OP_EXPORT);
        return InvoicingMappers.toExportInvoicesResult(raw);
    }

    /**
     * Get the status of an invoice export job.
     *
     * @param referenceNumber the export reference number from {@link #exportInvoices}
     * @return export status with download URL when complete
     */
    @Override
    public InvoiceExportStatus getExportStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_EXPORT_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        InvoiceExportStatusResponseRaw raw = http.getAuthenticated(PATH_EXPORT_STATUS + referenceNumber, token,
                InvoiceExportStatusResponseRaw.class, OP_EXPORT_STATUS);
        return InvoicingMappers.toInvoiceExportStatus(raw);
    }

    private InvoiceMetadataResult doQueryMetadata(InvoiceQueryFiltersRaw filters) {
        String token = sessionContext.token();
        QueryInvoicesMetadataResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_METADATA, filters, token,
                QueryInvoicesMetadataResponseRaw.class, OP_QUERY_METADATA);
        return InvoicingMappers.toInvoiceMetadataResult(raw);
    }
}
