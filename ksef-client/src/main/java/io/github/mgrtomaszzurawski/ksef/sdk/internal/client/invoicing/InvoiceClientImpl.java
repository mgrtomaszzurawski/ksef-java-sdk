/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.net.http.HttpClient;
import java.security.PublicKey;
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
    private static final String OP_PREPARE_EXPORT = "prepareInvoiceExport";
    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NULL_EXPORT = "exportBuilder must not be null";
    private static final String ERR_NO_SYMMETRIC_KEY_CERT = "No KSeF public key found for SYMMETRIC_KEY_ENCRYPTION usage";
    private static final String ERR_PARSE_SYMMETRIC_CERT = "Failed to parse SYMMETRIC_KEY_ENCRYPTION certificate";
    private static final String ERR_TRUNCATED_NO_CURSOR =
            "queryAllMetadata: server returned isTruncated=true but no usable date cursor on the last record "
                    + "for the selected dateType axis — cannot advance pagination safely";
    private static final String CERT_TYPE_X509 = "X.509";
    private static final int DEFAULT_MAX_RESULTS = 10000;
    /** Spec-defined maximum page size for {@code POST /invoices/query/metadata}. */
    private static final int QUERY_METADATA_MAX_PAGE_SIZE = 250;
    /** First page is offset 0 per OpenAPI. */
    private static final int QUERY_METADATA_FIRST_PAGE_OFFSET = 0;
    private static final String QUERY_PAGE_OFFSET_PARAM = "pageOffset";
    private static final String QUERY_PAGE_SIZE_PARAM = "pageSize";

    private final HttpSupport http;
    private final SecurityClient securityClient;
    private final HttpClient httpClient;

    public InvoiceClientImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.securityClient = new SecurityClient(runtime);
        this.httpClient = runtime.httpClient();
    }

    /**
     * Retrieve an invoice by its KSeF number. Returns raw invoice XML bytes.
     *
     * @param ksefNumber the unique KSeF invoice number — pre-validated by
     *     {@link KsefNumber} (length, format, CRC-8) so this method never
     *     issues a request with a malformed identifier
     * @return raw invoice XML bytes
     */
    @Override
    public byte[] getByKsefNumber(KsefNumber ksefNumber) {
        String value = ksefNumber.value();
        LOGGER.debug(LOG_CALL_REF, OP_GET_BY_KSEF, value);
        requireSafePathSegment(value);
        String token = http.requireToken();
        return http.getAuthenticatedBytes(PATH_INVOICES_KSEF + value, token, OP_GET_BY_KSEF);
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

        // Codex round-9 manual-validation A.1.2 — spec algorithm
        // (pobieranie-faktur/pobieranie-faktur.md):
        //   hasMore=false                                 → stop
        //   hasMore=true  && isTruncated=false            → pageOffset++ (same dateRange)
        //   hasMore=true  && isTruncated=true             → narrow dateRange.from
        //                                                   + reset pageOffset to 0
        // The previous helper always advanced dateRange.from by HWM and never
        // touched pageOffset, which dropped invoices that arrived between the
        // first page and HWM advancement.
        int pageOffset = QUERY_METADATA_FIRST_PAGE_OFFSET;
        while (true) {
            InvoiceMetadataResult page = doQueryMetadata(filters,
                    pageOffset, QUERY_METADATA_MAX_PAGE_SIZE);
            allInvoices.addAll(page.invoices());

            if (allInvoices.size() >= maxResults) {
                return List.copyOf(allInvoices.subList(0, maxResults));
            }

            if (!page.hasMore()) {
                break;
            }

            if (page.isTruncated()) {
                // Truncation case: per spec
                // (POST /invoices/query/metadata description in open-api.json),
                // narrow dateRange.from to the LAST RETURNED RECORD's date for
                // the chosen dateType axis, then reset pageOffset to 0.
                // permanentStorageHwmDate is a per-query constant, not a
                // per-page cursor — using it would produce stale data or
                // infinite loops on busy taxpayers.
                java.time.OffsetDateTime cursor = lastRecordCursor(page, query.build().dateType());
                if (cursor == null) {
                    // Codex 2026-05-05 F3 — fail-fast on internally
                    // inconsistent server response: a truncated page that
                    // doesn't supply the date axis we need cannot be
                    // advanced. Returning partial data here would lie to
                    // the caller about result completeness.
                    throw new KsefException(ERR_TRUNCATED_NO_CURSOR, null);
                }
                filters.getDateRange().from(cursor);
                pageOffset = QUERY_METADATA_FIRST_PAGE_OFFSET;
            } else {
                // hasMore && !isTruncated: stay on the same dateRange, advance
                // pageOffset to fetch the next page.
                pageOffset++;
            }
        }

        return List.copyOf(allInvoices);
    }

    /**
     * Start an invoice export job.
     *
     * @param exportBuilder the export builder with date range and filters
     * @return response with the export reference number for status polling
     * @deprecated prefer {@link #prepareExport(InvoiceQueryBuilder, boolean)}
     */
    @Deprecated(since = "0.1.0")
    @Override
    public ExportInvoicesResult exportInvoices(InvoiceExportBuilder exportBuilder) {
        LOGGER.debug(LOG_CALL, OP_EXPORT);
        Objects.requireNonNull(exportBuilder, ERR_NULL_EXPORT);
        InvoiceExportRequestRaw request = InvoicingRequestMappers.toInvoiceExportRequestRaw(exportBuilder.build());
        String token = http.requireToken();
        ExportInvoicesResponseRaw rawValue = http.postJsonAuthenticated(PATH_EXPORTS, request, token,
                ExportInvoicesResponseRaw.class, OP_EXPORT);
        return InvoicingMappers.toExportInvoicesResult(rawValue);
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
        String token = http.requireToken();
        InvoiceExportStatusResponseRaw rawValue = http.getAuthenticated(PATH_EXPORT_STATUS + referenceNumber, token,
                InvoiceExportStatusResponseRaw.class, OP_EXPORT_STATUS);
        return InvoicingMappers.toInvoiceExportStatus(rawValue);
    }

    @Override
    public PreparedInvoiceExport prepareExport(InvoiceQueryBuilder query, boolean fullContent) {
        LOGGER.debug(LOG_CALL, OP_PREPARE_EXPORT);
        Objects.requireNonNull(query, ERR_NULL_QUERY);

        PublicKey symmetricKey = securityClient.getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().contains(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION))
                .findFirst()
                .map(InvoiceClientImpl::parsePublicKey)
                .orElseThrow(() -> new IllegalStateException(ERR_NO_SYMMETRIC_KEY_CERT));

        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, symmetricKey);

        InvoiceExportRequest request = new InvoiceExportRequest(
                encryptedKey, initVector, !fullContent, query.build());
        InvoiceExportRequestRaw rawRequest = InvoicingRequestMappers.toInvoiceExportRequestRaw(request);
        String token = http.requireToken();
        ExportInvoicesResponseRaw rawValue = http.postJsonAuthenticated(PATH_EXPORTS, rawRequest, token,
                ExportInvoicesResponseRaw.class, OP_PREPARE_EXPORT);
        ExportInvoicesResult result = InvoicingMappers.toExportInvoicesResult(rawValue);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newPreparedExport(
                this, httpClient, result.referenceNumber(), aesKey, initVector);
    }

    private static PublicKey parsePublicKey(PublicKeyCertificate certificate) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            X509Certificate x509 = (X509Certificate)
                    factory.generateCertificate(new ByteArrayInputStream(certificate.certificate()));
            return x509.getPublicKey();
        } catch (CertificateException certificateFailure) {
            throw new KsefException(ERR_PARSE_SYMMETRIC_CERT, certificateFailure);
        }
    }

    /**
     * Pull the cursor value for the truncation-restart case from the last
     * record on the page, picking the date axis matching the filter's
     * {@code dateType}. Returns {@code null} when the page contained no
     * records (defensive — should not happen on a truncated page in
     * practice).
     */
    private static java.time.OffsetDateTime lastRecordCursor(InvoiceMetadataResult page,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType dateType) {
        if (page.invoices() == null || page.invoices().isEmpty()) {
            return null;
        }
        InvoiceMetadata last = page.invoices().get(page.invoices().size() - 1);
        return switch (dateType) {
            case PERMANENT_STORAGE -> last.permanentStorageDate();
            case INVOICING -> last.invoicingDate();
            case ISSUE -> last.issueDate() == null ? null
                    : last.issueDate().atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
        };
    }

    private InvoiceMetadataResult doQueryMetadata(InvoiceQueryFiltersRaw filters) {
        return doQueryMetadata(filters, QUERY_METADATA_FIRST_PAGE_OFFSET, QUERY_METADATA_MAX_PAGE_SIZE);
    }

    private InvoiceMetadataResult doQueryMetadata(InvoiceQueryFiltersRaw filters,
                                                    int pageOffset,
                                                    int pageSize) {
        String token = http.requireToken();
        String path = PATH_QUERY_METADATA
                + "?" + QUERY_PAGE_OFFSET_PARAM + "=" + pageOffset
                + "&" + QUERY_PAGE_SIZE_PARAM + "=" + pageSize;
        QueryInvoicesMetadataResponseRaw rawValue = http.postJsonAuthenticated(path, filters, token,
                QueryInvoicesMetadataResponseRaw.class, OP_QUERY_METADATA);
        return InvoicingMappers.toInvoiceMetadataResult(rawValue);
    }
}
