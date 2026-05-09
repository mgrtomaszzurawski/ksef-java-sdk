/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSink;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.net.http.HttpClient;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;
import org.jspecify.annotations.Nullable;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
 *
 * @since 1.0.0
 */
public final class InvoiceClientImpl implements InvoiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceClientImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_INVOICES_KSEF = ApiPaths.INVOICES + "/ksef/";
    private static final String PATH_QUERY_METADATA = ApiPaths.INVOICES + "/query/metadata";
    private static final String PATH_EXPORTS = ApiPaths.INVOICES + "/exports";
    private static final String PATH_EXPORT_STATUS = ApiPaths.INVOICES + "/exports/";

    private static final String OP_GET_BY_KSEF = "getInvoiceByKsefNumber";
    private static final String OP_QUERY_METADATA = "queryInvoicesMetadata";
    private static final String OP_EXPORT_STATUS = "getExportStatus";
    private static final String OP_PREPARE_EXPORT = "prepareInvoiceExport";
    private static final String OP_OPEN_SESSION = "openSession";
    private static final String OP_STREAM_SESSIONS = "streamSessions";
    private static final String OP_SYNC = "sync";
    private static final String LOG_OPENED_ONLINE_SESSION = "Opened KSeF session {}, formCode={}";
    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NULL_FILTER = "filter must not be null";
    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_OPEN_SESSION_REQUIRES_FULL_RUNTIME =
            "openSession() requires the full InvoiceClient runtime — instantiate via the multi-arg constructor";
    private static final String ERR_STREAM_SESSIONS_REQUIRES_FULL_RUNTIME =
            "streamSessions() requires the full InvoiceClient runtime — instantiate via the multi-arg constructor";
    private static final String ERR_NO_SYMMETRIC_KEY_CERT = "No KSeF public key found for SYMMETRIC_KEY_ENCRYPTION usage";
    private static final String ERR_TRUNCATED_NO_CURSOR =
            "streamInvoicesByMetadata: server returned isTruncated=true but no usable date cursor on the last record "
                    + "for the selected dateType axis — cannot advance pagination safely";
    /** Spec-defined maximum page size for {@code POST /invoices/query/metadata}. */
    private static final int QUERY_METADATA_MAX_PAGE_SIZE = 250;
    /** First page is offset 0 per OpenAPI. */
    private static final int QUERY_METADATA_FIRST_PAGE_OFFSET = 0;
    private static final String QUERY_PAGE_OFFSET_PARAM = "pageOffset";
    private static final String QUERY_PAGE_SIZE_PARAM = "pageSize";
    private static final String QUERY_SORT_ORDER_PARAM = "sortOrder";

    private final HttpRuntime runtime;
    private final HttpSupport http;
    private final SecurityClient securityClient;
    private final HttpClient httpClient;
    private final @Nullable SessionClient sessionClient;
    private final @Nullable KsefEnvironment environment;
    private final @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver;

    public InvoiceClientImpl(HttpRuntime runtime) {
        this(runtime, null, null, null);
    }

    public InvoiceClientImpl(HttpRuntime runtime,
                              @Nullable SessionClient sessionClient,
                              @Nullable KsefEnvironment environment,
                              @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver) {
        this.runtime = runtime;
        this.http = new HttpSupport(runtime);
        this.securityClient = new SecurityClient(runtime);
        this.httpClient = runtime.httpClient();
        this.sessionClient = sessionClient;
        this.environment = environment;
        this.publicKeyResolver = publicKeyResolver;
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
     * @param query the filter criteria
     * @return paginated list of invoice metadata
     */
    @Override
    public InvoiceMetadataResult queryInvoicesByMetadata(InvoiceQueryFilters query) {
        LOGGER.debug(LOG_CALL, OP_QUERY_METADATA);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        return doQueryMetadata(
                InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query),
                QUERY_METADATA_FIRST_PAGE_OFFSET, QUERY_METADATA_MAX_PAGE_SIZE,
                query.sortOrder());
    }

    /**
     * Stream every invoice metadata record matching {@code query},
     * walking the spec's date-cursor + page-offset model lazily.
     *
     * <p>Per {@code pobieranie-faktur/pobieranie-faktur.md}:
     * <ul>
     *   <li>{@code hasMore=false} → stream completes.</li>
     *   <li>{@code hasMore=true && isTruncated=false} → {@code pageOffset++}
     *       on the same date range.</li>
     *   <li>{@code hasMore=true && isTruncated=true} → narrow
     *       {@code dateRange.from} to the last record's date for the
     *       configured axis, reset {@code pageOffset = 0}.</li>
     * </ul>
     *
     * <p>If 251+ records share the same axis value, the cursor stalls;
     * we fall back to {@code pageOffset++} on the same range so the
     * cluster can still drain server-side.
     *
     * <p>Throws {@link KsefException} when a truncated page lacks a
     * usable date cursor — partial data here would silently lie about
     * result completeness.
     */
    @Override
    public Stream<InvoiceMetadata> streamInvoicesByMetadata(InvoiceQueryFilters query) {
        LOGGER.debug(LOG_CALL, OP_QUERY_METADATA);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        InvoiceQueryFiltersRaw filters = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query);
        Iterator<InvoiceMetadata> iterator = new MetadataPageIterator(filters, query.dateType(), query.sortOrder());
        return java.util.stream.StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(iterator,
                        java.util.Spliterator.ORDERED | java.util.Spliterator.NONNULL),
                false);
    }

    private final class MetadataPageIterator implements Iterator<InvoiceMetadata> {

        private final InvoiceQueryFiltersRaw filters;
        private final io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType dateType;
        private final @Nullable SortOrder sortOrder;
        private final java.util.Deque<InvoiceMetadata> buffer = new java.util.ArrayDeque<>();
        private int pageOffset = QUERY_METADATA_FIRST_PAGE_OFFSET;
        private java.time.@Nullable OffsetDateTime previousCursor;
        private boolean exhausted;

        MetadataPageIterator(InvoiceQueryFiltersRaw filters,
                              io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType dateType,
                              @Nullable SortOrder sortOrder) {
            this.filters = filters;
            this.dateType = dateType;
            this.sortOrder = sortOrder;
        }

        @Override
        public boolean hasNext() {
            if (!buffer.isEmpty()) {
                return true;
            }
            while (!exhausted && buffer.isEmpty()) {
                fetchOnePage();
            }
            return !buffer.isEmpty();
        }

        @Override
        public InvoiceMetadata next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }
            return buffer.poll();
        }

        private void fetchOnePage() {
            InvoiceMetadataResult page = doQueryMetadata(filters, pageOffset, QUERY_METADATA_MAX_PAGE_SIZE, sortOrder);
            buffer.addAll(page.invoices());
            if (!page.hasMore()) {
                exhausted = true;
                return;
            }
            if (page.isTruncated()) {
                java.time.OffsetDateTime cursor = lastRecordCursor(page, dateType);
                if (cursor == null) {
                    throw new KsefException(ERR_TRUNCATED_NO_CURSOR, null);
                }
                if (cursor.equals(previousCursor)) {
                    pageOffset++;
                } else {
                    filters.getDateRange().from(cursor);
                    pageOffset = QUERY_METADATA_FIRST_PAGE_OFFSET;
                    previousCursor = cursor;
                }
            } else {
                pageOffset++;
            }
        }
    }

    /**
     * Get the status of an invoice export job.
     *
     * @param referenceNumber the export reference number from {@link #prepareExport(InvoiceQueryFilters, boolean)}
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
    public PreparedInvoiceExport prepareExport(InvoiceQueryFilters query, boolean fullContent) {
        LOGGER.debug(LOG_CALL, OP_PREPARE_EXPORT);
        Objects.requireNonNull(query, ERR_NULL_QUERY);

        PublicKey symmetricKey = securityClient.getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().contains(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION))
                .findFirst()
                .map(PublicKeyCertificate::publicKey)
                .orElseThrow(() -> new IllegalStateException(ERR_NO_SYMMETRIC_KEY_CERT));

        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, symmetricKey);

        InvoiceExportRequest request = new InvoiceExportRequest(
                encryptedKey, initVector, !fullContent, query);
        InvoiceExportRequestRaw rawRequest = InvoicingRequestMappers.toInvoiceExportRequestRaw(request);
        String token = http.requireToken();
        ExportInvoicesResponseRaw rawValue = http.postJsonAuthenticated(PATH_EXPORTS, rawRequest, token,
                ExportInvoicesResponseRaw.class, OP_PREPARE_EXPORT);
        ExportInvoicesResult result = InvoicingMappers.toExportInvoicesResult(rawValue);
        return SessionHandleConstructor.newPreparedExport(
                this, httpClient, result.referenceNumber(), aesKey, initVector);
    }

    /**
     * Pull the cursor value for the truncation-restart case from the last
     * record on the page, picking the date axis matching the filter's
     * {@code dateType}. Returns {@code null} when the page contained no
     * records (defensive — should not happen on a truncated page in
     * practice).
     */
    private static java.time.@Nullable OffsetDateTime lastRecordCursor(InvoiceMetadataResult page,
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

    private InvoiceMetadataResult doQueryMetadata(InvoiceQueryFiltersRaw filters,
                                                    int pageOffset,
                                                    int pageSize,
                                                    @Nullable SortOrder sortOrder) {
        String token = http.requireToken();
        StringBuilder path = new StringBuilder(PATH_QUERY_METADATA)
                .append('?').append(QUERY_PAGE_OFFSET_PARAM).append('=').append(pageOffset)
                .append('&').append(QUERY_PAGE_SIZE_PARAM).append('=').append(pageSize);
        if (sortOrder != null) {
            path.append('&').append(QUERY_SORT_ORDER_PARAM).append('=').append(sortOrder.wireValue());
        }
        QueryInvoicesMetadataResponseRaw rawValue = http.postJsonAuthenticated(path.toString(), filters, token,
                QueryInvoicesMetadataResponseRaw.class, OP_QUERY_METADATA);
        return InvoicingMappers.toInvoiceMetadataResult(rawValue);
    }

    @Override
    @SuppressWarnings("java:S2629")
    public KsefSession openSession(FormCode formCode) {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        if (sessionClient == null || environment == null || publicKeyResolver == null) {
            throw new IllegalStateException(ERR_OPEN_SESSION_REQUIRES_FULL_RUNTIME);
        }
        LOGGER.debug(LOG_CALL, OP_OPEN_SESSION);
        formCode.assertAllowedOn(environment);

        PublicKey encryptionKey = publicKeyResolver.apply(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector));

        OnlineSession session = sessionClient.openOnline(request);
        LOGGER.debug(LOG_OPENED_ONLINE_SESSION, session.referenceNumber(), formCode);
        guardAgainstCooldown(session.referenceNumber());

        return SessionHandleConstructor.newOnlineSession(
                sessionClient, session.referenceNumber(), aesKey, initVector, session.validUntil());
    }

    /**
     * Codex 2026-05-05 #8b — proactively detect the post-termination
     * cooldown window. KSeF returns a fresh session reference on
     * {@code openOnline}, but the session can immediately enter status 415
     * when reopened too soon after a previous termination for the same
     * NIP. Catch that here and surface it as a typed exception instead of
     * letting the caller hit it on first {@code send(...)}.
     */
    private void guardAgainstCooldown(String referenceNumber) {
        var status = Objects.requireNonNull(sessionClient, "sessionClient").getStatus(referenceNumber);
        if (status.status() != null
                && KsefSessionCooldownException.isCooldownStatus(status.status().code())) {
            throw new KsefSessionCooldownException(
                    "openSession returned reference " + referenceNumber
                            + " but immediately reports status 415 — server is in the post-termination"
                            + " cooldown window for this NIP. Wait at least "
                            + KsefSessionCooldownException.TYPICAL_COOLDOWN
                            + " before retrying.");
        }
    }

    @Override
    public Stream<SessionListItem> streamSessions(SessionsQueryFilter filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        if (sessionClient == null) {
            throw new IllegalStateException(ERR_STREAM_SESSIONS_REQUIRES_FULL_RUNTIME);
        }
        LOGGER.debug(LOG_CALL, OP_STREAM_SESSIONS);
        return sessionClient.streamSessions(filter);
    }

    @Override
    public SyncResult sync(IncrementalSyncPlan plan, CheckpointStore checkpointStore, InvoiceSink sink) {
        LOGGER.debug(LOG_CALL, OP_SYNC);
        return new InvoiceSyncClient(this, runtime.objectMapper()).sync(plan, checkpointStore, sink);
    }
}
