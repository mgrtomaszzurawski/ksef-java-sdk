/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;

/**
 * Archive-side implementation of {@link InvoiceArchive}: retrieve by KSeF
 * number, reconstruct cleared invoices, and query/stream invoice metadata.
 *
 * @since 1.0.0
 */
public final class InvoiceArchiveImpl implements InvoiceArchive {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceArchiveImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_INVOICES_KSEF = ApiPaths.INVOICES + "/ksef/";
    private static final String PATH_QUERY_METADATA = ApiPaths.INVOICES + "/query/metadata";

    private static final String OP_GET_BY_KSEF = "getInvoiceByKsefNumber";
    private static final String OP_CLEARED_FROM_ARCHIVE = "clearedFromArchive";
    private static final String OP_QUERY_METADATA = "queryInvoicesMetadata";

    private static final int STATUS_CODE_ACCEPTED = 200;
    private static final String UNKNOWN_FORM_CODE_SYSTEM_CODE = "UNKNOWN";
    private static final String UNKNOWN_FORM_CODE_SCHEMA_VERSION = "0";
    private static final String UNKNOWN_FORM_CODE_TYPE = "UNKNOWN";

    private static final int QUERY_METADATA_MAX_PAGE_SIZE = 250;
    private static final int QUERY_METADATA_FIRST_PAGE_OFFSET = 0;
    private static final String QUERY_PAGE_OFFSET_PARAM = "pageOffset";
    private static final String QUERY_PAGE_SIZE_PARAM = "pageSize";
    private static final String QUERY_SORT_ORDER_PARAM = "sortOrder";

    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NULL_SESSION_REF = "sessionReferenceNumber must not be null";
    private static final String ERR_NULL_INVOICE_REF = "invoiceReferenceNumber must not be null";
    private static final String ERR_CLEARED_FROM_ARCHIVE_REQUIRES_FULL_RUNTIME =
            "clearedFromArchive() requires the full Invoices runtime — instantiate via the multi-arg constructor";
    private static final String ERR_NOT_ACCEPTED =
            "Invoice has not reached accepted state (code 200) — no UPO available. Current status code: ";
    private static final String ERR_NO_KSEF_NUMBER =
            "Server returned status 200 but no ksefNumber — cannot recover invoice document from archive.";
    private static final String ERR_TRUNCATED_NO_CURSOR =
            "streamByMetadata: server returned isTruncated=true but no usable date cursor on the last record "
                    + "for the selected dateType axis — cannot advance pagination safely";

    private final HttpSupport http;
    private final @Nullable SessionClient sessionClient;

    public InvoiceArchiveImpl(HttpRuntime runtime, @Nullable SessionClient sessionClient) {
        this.http = new HttpSupport(runtime);
        this.sessionClient = sessionClient;
    }

    @Override
    public InvoiceDocument getByKsefNumber(KsefNumber ksefNumber) {
        String value = ksefNumber.value();
        LOGGER.debug(LOG_CALL_REF, OP_GET_BY_KSEF, value);
        requireSafePathSegment(value);
        String token = http.requireToken();
        byte[] xml = http.getAuthenticatedBytes(PATH_INVOICES_KSEF + value, token, OP_GET_BY_KSEF);
        Optional<FormCode> detected = FormCodeDetector.detect(xml);
        if (detected.isEmpty()) {
            return InvoiceDocument.fromXml(FormCode.custom(UNKNOWN_FORM_CODE_SYSTEM_CODE,
                    UNKNOWN_FORM_CODE_SCHEMA_VERSION, UNKNOWN_FORM_CODE_TYPE), xml);
        }
        FormCode formCode = detected.get();
        if (formCode.equals(FormCode.FA3)) {
            return Fa3InvoiceDocument.from(xml);
        }
        if (formCode.equals(FormCode.FA2)) {
            return Fa2InvoiceDocument.from(xml);
        }
        if (formCode.equals(FormCode.PEF3)) {
            return PefInvoiceDocument.from(xml);
        }
        if (formCode.equals(FormCode.PEF_KOR3)) {
            return PefKorInvoiceDocument.from(xml);
        }
        return InvoiceDocument.fromXml(formCode, xml);
    }

    @Override
    public ClearedInvoice clearedFromArchive(String sessionReferenceNumber, String invoiceReferenceNumber) {
        Objects.requireNonNull(sessionReferenceNumber, ERR_NULL_SESSION_REF);
        Objects.requireNonNull(invoiceReferenceNumber, ERR_NULL_INVOICE_REF);
        if (sessionClient == null) {
            throw new IllegalStateException(ERR_CLEARED_FROM_ARCHIVE_REQUIRES_FULL_RUNTIME);
        }
        LOGGER.debug(LOG_CALL_REF, OP_CLEARED_FROM_ARCHIVE, invoiceReferenceNumber);

        SessionInvoiceStatus status = sessionClient.getInvoiceStatus(sessionReferenceNumber, invoiceReferenceNumber);
        if (status.status() == null || status.status().code() != STATUS_CODE_ACCEPTED) {
            int code = status.status() == null ? -1 : status.status().code();
            throw new KsefException(ERR_NOT_ACCEPTED + code, null);
        }
        if (status.ksefNumber() == null) {
            throw new KsefException(ERR_NO_KSEF_NUMBER, null);
        }

        KsefNumber ksefNumber = KsefNumber.parse(status.ksefNumber());
        InvoiceDocument document = getByKsefNumber(ksefNumber);
        byte[] upoBytes = sessionClient.getUpoByInvoiceReference(sessionReferenceNumber, invoiceReferenceNumber);
        UpoEntry upo = new UpoEntry(invoiceReferenceNumber, upoBytes);
        Invoice invoice = Invoice.fromXml(document.formCode(), document.xml());

        SubmittedInvoice submitted = new SubmittedInvoice(
                invoice,
                invoiceReferenceNumber,
                status,
                Optional.of(ksefNumber),
                Optional.empty(),
                Optional.empty(),
                List.of());
        return new ClearedInvoice(submitted, document, upo);
    }

    @Override
    public InvoiceMetadataResult queryByMetadata(InvoiceQueryRequest query) {
        LOGGER.debug(LOG_CALL, OP_QUERY_METADATA);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        return doQueryMetadata(
                InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query),
                QUERY_METADATA_FIRST_PAGE_OFFSET, QUERY_METADATA_MAX_PAGE_SIZE,
                query.sortOrder());
    }

    @Override
    public Stream<InvoiceMetadata> streamByMetadata(InvoiceQueryRequest query) {
        LOGGER.debug(LOG_CALL, OP_QUERY_METADATA);
        Objects.requireNonNull(query, ERR_NULL_QUERY);
        InvoiceQueryFiltersRaw filters = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(query);
        Iterator<InvoiceMetadata> iterator = new MetadataPageIterator(filters, query.dateType(), query.sortOrder());
        return java.util.stream.StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(iterator,
                        java.util.Spliterator.ORDERED | java.util.Spliterator.NONNULL),
                false);
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

    private static java.time.@Nullable OffsetDateTime lastRecordCursor(InvoiceMetadataResult page,
            InvoiceQueryDateType dateType) {
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

    private final class MetadataPageIterator implements Iterator<InvoiceMetadata> {

        private final InvoiceQueryFiltersRaw filters;
        private final InvoiceQueryDateType dateType;
        private final @Nullable SortOrder sortOrder;
        private final java.util.Deque<InvoiceMetadata> buffer = new java.util.ArrayDeque<>();
        private int pageOffset = QUERY_METADATA_FIRST_PAGE_OFFSET;
        private java.time.@Nullable OffsetDateTime previousCursor;
        private boolean exhausted;

        MetadataPageIterator(InvoiceQueryFiltersRaw filters,
                              InvoiceQueryDateType dateType,
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
}
