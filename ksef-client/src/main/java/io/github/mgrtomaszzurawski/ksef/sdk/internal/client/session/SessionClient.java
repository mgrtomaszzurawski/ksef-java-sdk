/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session;

import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoiceStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;

/**
 * Client for KSeF session operations — online and batch session lifecycle,
 * invoice submission, and UPO retrieval.
 *
 * @since 1.0.0
 */
public final class SessionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionClient.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_ONLINE = ApiPaths.SESSIONS + "/online";
    private static final String PATH_BATCH = ApiPaths.SESSIONS + "/batch";

    private static final String SEGMENT_INVOICES = "/invoices";
    private static final String SEGMENT_FAILED = "/invoices/failed";
    private static final String SEGMENT_CLOSE = "/close";
    private static final String SEGMENT_UPO = "/upo/";
    private static final String SEGMENT_INVOICE_UPO = "/upo";
    private static final String SEGMENT_KSEF_UPO = "/invoices/ksef/";
    /** Spec-defined max page size for {@code GET /sessions/{ref}/invoices}. */
    private static final int SESSION_INVOICES_PAGE_SIZE = 250;
    /** Custom request header for paginated session-invoices endpoint. */
    public static final String HEADER_CONTINUATION_TOKEN = "x-continuation-token";
    private static final String QUERY_PAGE_SIZE = "?pageSize=";

    // Codex review CRITICAL — magic strings → constants for the
    // GET /sessions listing query-string assembly.
    private static final String QUERY_PARAM_SEPARATOR = "&";
    private static final String QUERY_PARAM_EQUALS = "=";
    private static final String PARAM_SESSION_TYPE = "sessionType";
    private static final String PARAM_REFERENCE_NUMBER = "referenceNumber";
    private static final String PARAM_DATE_CREATED_FROM = "dateCreatedFrom";
    private static final String PARAM_DATE_CREATED_TO = "dateCreatedTo";
    private static final String PARAM_DATE_CLOSED_FROM = "dateClosedFrom";
    private static final String PARAM_DATE_CLOSED_TO = "dateClosedTo";
    private static final String PARAM_DATE_MODIFIED_FROM = "dateModifiedFrom";
    private static final String PARAM_DATE_MODIFIED_TO = "dateModifiedTo";
    private static final String PARAM_STATUSES = "statuses";
    /** Spec wire value for {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType#ONLINE}. */
    private static final String SESSION_TYPE_ONLINE = "Online";
    /** Spec wire value for {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType#BATCH}. */
    private static final String SESSION_TYPE_BATCH = "Batch";

    private static final String OP_OPEN_ONLINE = "openOnlineSession";
    private static final String OP_SEND_INVOICE = "sendInvoice";
    private static final String OP_CLOSE_ONLINE = "closeOnlineSession";
    private static final String OP_OPEN_BATCH = "openBatchSession";
    private static final String OP_CLOSE_BATCH = "closeBatchSession";
    private static final String OP_GET_STATUS = "getSessionStatus";
    private static final String OP_GET_INVOICES = "getSessionInvoices";
    private static final String OP_GET_INVOICE_STATUS = "getInvoiceStatus";
    private static final String OP_GET_FAILED = "getFailedInvoices";
    private static final String OP_GET_UPO_BY_REF = "getUpoByReference";
    private static final String OP_GET_UPO_BY_INVOICE = "getUpoByInvoiceReference";
    private static final String OP_GET_UPO_BY_KSEF = "getUpoByKsefNumber";
    private static final String OP_QUERY_SESSIONS = "querySessions";

    private final HttpSupport http;

    public SessionClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * List sessions matching the given filter, with internal cursor iteration.
     * Codex round-9 manual-validation A.2.4 — previously the
     * {@code GET /sessions} listing was reachable in OpenAPI but not in code.
     */
    public java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem>
            queryAllSessions(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter filter) {
        java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem> all =
                new java.util.ArrayList<>();
        String continuationToken = null;
        while (true) {
            io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseRaw raw = querySessionsPage(filter, continuationToken);
            if (raw.getSessions() != null) {
                for (io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseItemRaw item : raw.getSessions()) {
                    all.add(toSessionListItem(item));
                }
            }
            if (all.size() >= io.github.mgrtomaszzurawski.ksef.sdk.common.KsefLimits.DEFAULT_QUERY_RESULT_LIMIT) {
                return java.util.List.copyOf(all.subList(0,
                        io.github.mgrtomaszzurawski.ksef.sdk.common.KsefLimits.DEFAULT_QUERY_RESULT_LIMIT));
            }
            String next = raw.getContinuationToken();
            if (next == null || next.isEmpty()) {
                return java.util.List.copyOf(all);
            }
            continuationToken = next;
        }
    }

    @SuppressWarnings("PMD.ConsecutiveAppendsShouldReuse")
    private io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseRaw querySessionsPage(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter filter,
            String continuationToken) {
        LOGGER.debug(LOG_CALL, OP_QUERY_SESSIONS);
        String token = http.requireToken();
        StringBuilder path = new StringBuilder(ApiPaths.SESSIONS).append(QUERY_PAGE_SIZE).append(SESSION_INVOICES_PAGE_SIZE);
        // sessionType is required per OpenAPI; SessionsQueryFilter compact
        // constructor enforces non-null so we never need a presence check here.
        path.append(QUERY_PARAM_SEPARATOR).append(PARAM_SESSION_TYPE).append(QUERY_PARAM_EQUALS)
                .append(filter.sessionType() == io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType.ONLINE
                        ? SESSION_TYPE_ONLINE : SESSION_TYPE_BATCH);
        if (filter.referenceNumber() != null) {
            path.append(QUERY_PARAM_SEPARATOR).append(PARAM_REFERENCE_NUMBER).append(QUERY_PARAM_EQUALS)
                    .append(java.net.URLEncoder.encode(filter.referenceNumber(), java.nio.charset.StandardCharsets.UTF_8));
        }
        appendDateParam(path, PARAM_DATE_CREATED_FROM, filter.dateCreatedFrom());
        appendDateParam(path, PARAM_DATE_CREATED_TO, filter.dateCreatedTo());
        appendDateParam(path, PARAM_DATE_CLOSED_FROM, filter.dateClosedFrom());
        appendDateParam(path, PARAM_DATE_CLOSED_TO, filter.dateClosedTo());
        appendDateParam(path, PARAM_DATE_MODIFIED_FROM, filter.dateModifiedFrom());
        appendDateParam(path, PARAM_DATE_MODIFIED_TO, filter.dateModifiedTo());
        if (filter.statuses() != null) {
            for (Integer code : filter.statuses()) {
                path.append(QUERY_PARAM_SEPARATOR).append(PARAM_STATUSES).append(QUERY_PARAM_EQUALS).append(code);
            }
        }
        return continuationToken == null
                ? http.getAuthenticated(path.toString(), token,
                        io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseRaw.class, OP_QUERY_SESSIONS)
                : http.getAuthenticated(path.toString(), token,
                        io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseRaw.class, OP_QUERY_SESSIONS,
                        HEADER_CONTINUATION_TOKEN, continuationToken);
    }

    private static void appendDateParam(StringBuilder path, String name, java.time.OffsetDateTime value) {
        if (value != null) {
            path.append(QUERY_PARAM_SEPARATOR).append(name).append(QUERY_PARAM_EQUALS)
                    .append(java.net.URLEncoder.encode(value.toString(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem toSessionListItem(
            io.github.mgrtomaszzurawski.ksef.client.model.SessionsQueryResponseItemRaw raw) {
        return new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem(
                raw.getReferenceNumber(),
                io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers.toStatusInfo(raw.getStatus()),
                raw.getDateCreated(),
                raw.getDateUpdated(),
                raw.getValidUntil(),
                raw.getTotalInvoiceCount(),
                raw.getSuccessfulInvoiceCount(),
                raw.getFailedInvoiceCount());
    }

    /**
     * Open an online (interactive) session for invoice submission.
     *
     * @param request session opening parameters (form code, encryption info)
     * @return response with session reference number and validity period
     */
    public OnlineSession openOnline(OpenOnlineSessionRequestRaw request) {
        LOGGER.debug(LOG_CALL, OP_OPEN_ONLINE);
        String token = http.requireToken();
        OpenOnlineSessionResponseRaw rawValue = http.postJsonAuthenticated(PATH_ONLINE, request, token,
                OpenOnlineSessionResponseRaw.class, OP_OPEN_ONLINE);
        return InvoicingMappers.toOnlineSession(rawValue);
    }

    /**
     * Send an invoice within an open online session.
     *
     * @param referenceNumber the session reference number
     * @param request the invoice payload (encrypted content, hashes, sizes)
     * @return response with the invoice reference number
     */
    public SendInvoiceResult sendInvoice(String referenceNumber, SendInvoiceRequest request) {
        LOGGER.debug(LOG_CALL_REF, OP_SEND_INVOICE, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_ONLINE, referenceNumber) + SEGMENT_INVOICES;
        SendInvoiceRequestRaw rawValue = InvoicingRequestMappers.toSendInvoiceRequestRaw(request);
        SendInvoiceResponseRaw responseRaw = http.postJsonAuthenticated(path, rawValue, token,
                SendInvoiceResponseRaw.class, OP_SEND_INVOICE);
        return InvoicingMappers.toSendInvoiceResult(responseRaw);
    }

    /**
     * Close an online session. No more invoices can be sent after closing.
     *
     * @param referenceNumber the session reference number
     */
    public void closeOnline(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_CLOSE_ONLINE, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_ONLINE, referenceNumber) + SEGMENT_CLOSE;
        http.postNoBodyAuthenticated(path, token, OP_CLOSE_ONLINE);
    }

    /**
     * Open a batch session for bulk invoice submission.
     *
     * @param request batch session opening parameters
     * @return response with session reference number
     */
    public BatchSession openBatch(OpenBatchSessionRequestRaw request) {
        LOGGER.debug(LOG_CALL, OP_OPEN_BATCH);
        String token = http.requireToken();
        OpenBatchSessionResponseRaw rawValue = http.postJsonAuthenticated(PATH_BATCH, request, token,
                OpenBatchSessionResponseRaw.class, OP_OPEN_BATCH);
        return InvoicingMappers.toBatchSession(rawValue);
    }

    /**
     * Close a batch session.
     *
     * @param referenceNumber the batch session reference number
     */
    public void closeBatch(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_CLOSE_BATCH, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_BATCH, referenceNumber) + SEGMENT_CLOSE;
        http.postNoBodyAuthenticated(path, token, OP_CLOSE_BATCH);
    }

    /**
     * Get the status of a session (online or batch).
     *
     * @param referenceNumber the session reference number
     * @return session status with invoice counts, UPO availability, etc.
     */
    public SessionStatus getStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        SessionStatusResponseRaw rawValue = http.getAuthenticated(ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber), token,
                SessionStatusResponseRaw.class, OP_GET_STATUS);
        return InvoicingMappers.toSessionStatus(rawValue);
    }

    /**
     * Get invoices submitted within a session.
     *
     * @param referenceNumber the session reference number
     * @return list of invoice metadata
     */
    public SessionInvoices getInvoices(String referenceNumber) {
        return getInvoicesPage(referenceNumber, null);
    }

    /**
     * Get a single page of invoices, with optional continuation token from the
     * previous page (Codex round-9 manual-validation A.2.3). The token is
     * forwarded in the {@code x-continuation-token} request header per spec.
     */
    public SessionInvoices getInvoicesPage(String referenceNumber, String continuationToken) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_INVOICES, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_INVOICES
                + QUERY_PAGE_SIZE + SESSION_INVOICES_PAGE_SIZE;
        SessionInvoicesResponseRaw rawValue = continuationToken == null
                ? http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_INVOICES)
                : http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_INVOICES,
                        HEADER_CONTINUATION_TOKEN, continuationToken);
        return InvoicingMappers.toSessionInvoices(rawValue);
    }

    /**
     * Iterate every page of invoices for the session, following
     * {@code x-continuation-token} internally. Use this from
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession#invoices()}
     * — sessions with more than 10 (default) or {@value #SESSION_INVOICES_PAGE_SIZE}
     * (max) invoices were silently truncated by {@link #getInvoices(String)}.
     */
    public java.util.List<SessionInvoiceStatus> getAllInvoices(String referenceNumber) {
        return collectAllPages(referenceNumber, false);
    }

    public java.util.List<SessionInvoiceStatus> getAllFailedInvoices(String referenceNumber) {
        return collectAllPages(referenceNumber, true);
    }

    private java.util.List<SessionInvoiceStatus> collectAllPages(String referenceNumber, boolean failedOnly) {
        java.util.List<SessionInvoiceStatus> all = new java.util.ArrayList<>();
        String continuationToken = null;
        while (true) {
            SessionInvoices page = failedOnly
                    ? getFailedInvoicesPage(referenceNumber, continuationToken)
                    : getInvoicesPage(referenceNumber, continuationToken);
            all.addAll(page.invoices());
            if (all.size() >= io.github.mgrtomaszzurawski.ksef.sdk.common.KsefLimits.DEFAULT_QUERY_RESULT_LIMIT) {
                return java.util.List.copyOf(all.subList(0,
                        io.github.mgrtomaszzurawski.ksef.sdk.common.KsefLimits.DEFAULT_QUERY_RESULT_LIMIT));
            }
            String next = page.continuationToken();
            if (next == null || next.isEmpty()) {
                return java.util.List.copyOf(all);
            }
            continuationToken = next;
        }
    }

    /**
     * Get the status of a specific invoice within a session.
     *
     * @param referenceNumber the session reference number
     * @param invoiceReferenceNumber the invoice reference number
     * @return invoice processing status
     */
    public SessionInvoiceStatus getInvoiceStatus(String referenceNumber, String invoiceReferenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_INVOICE_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        requireSafePathSegment(invoiceReferenceNumber);
        String token = http.requireToken();
        String sessionPath = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_INVOICES;
        String path = ApiPaths.subPath(sessionPath, invoiceReferenceNumber);
        SessionInvoiceStatusResponseRaw rawValue = http.getAuthenticated(path, token, SessionInvoiceStatusResponseRaw.class, OP_GET_INVOICE_STATUS);
        return InvoicingMappers.toSessionInvoiceStatus(rawValue);
    }

    /**
     * Get failed invoices within a session.
     *
     * @param referenceNumber the session reference number
     * @return list of failed invoice metadata
     */
    public SessionInvoices getFailedInvoices(String referenceNumber) {
        return getFailedInvoicesPage(referenceNumber, null);
    }

    public SessionInvoices getFailedInvoicesPage(String referenceNumber, String continuationToken) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_FAILED, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_FAILED
                + QUERY_PAGE_SIZE + SESSION_INVOICES_PAGE_SIZE;
        SessionInvoicesResponseRaw rawValue = continuationToken == null
                ? http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_FAILED)
                : http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_FAILED,
                        HEADER_CONTINUATION_TOKEN, continuationToken);
        return InvoicingMappers.toSessionInvoices(rawValue);
    }

    /**
     * Download UPO (official receipt) by UPO reference number.
     *
     * @param referenceNumber the session reference number
     * @param upoReferenceNumber the UPO reference number
     * @return raw UPO bytes (XML)
     */
    public byte[] getUpoByReference(String referenceNumber, String upoReferenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_UPO_BY_REF, referenceNumber);
        requireSafePathSegment(referenceNumber);
        requireSafePathSegment(upoReferenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_UPO + upoReferenceNumber;
        return http.getAuthenticatedBytes(path, token, OP_GET_UPO_BY_REF);
    }

    /**
     * Download UPO by invoice reference number.
     *
     * @param referenceNumber the session reference number
     * @param invoiceReferenceNumber the invoice reference number
     * @return raw UPO bytes (XML)
     */
    public byte[] getUpoByInvoiceReference(String referenceNumber, String invoiceReferenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_UPO_BY_INVOICE, referenceNumber);
        requireSafePathSegment(referenceNumber);
        requireSafePathSegment(invoiceReferenceNumber);
        String token = http.requireToken();
        String sessionPath = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_INVOICES;
        String path = ApiPaths.subPath(sessionPath, invoiceReferenceNumber) + SEGMENT_INVOICE_UPO;
        return http.getAuthenticatedBytes(path, token, OP_GET_UPO_BY_INVOICE);
    }

    /**
     * Download UPO by KSeF invoice number. Validates length, format, and
     * CRC-8 checksum of {@code ksefNumber} before the network call
     * (REQ-SESS-18/19/20).
     *
     * @param referenceNumber the session reference number
     * @param ksefNumber the KSeF invoice number — pre-validated by
     *     {@link KsefNumber}
     * @return raw UPO bytes (XML)
     */
    public byte[] getUpoByKsefNumber(String referenceNumber, KsefNumber ksefNumber) {
        String value = ksefNumber.value();
        LOGGER.debug(LOG_CALL_REF, OP_GET_UPO_BY_KSEF, referenceNumber);
        requireSafePathSegment(referenceNumber);
        requireSafePathSegment(value);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber)
                + SEGMENT_KSEF_UPO + value + SEGMENT_INVOICE_UPO;
        return http.getAuthenticatedBytes(path, token, OP_GET_UPO_BY_KSEF);
    }

    /**
     * Convenience overload that parses the raw KSeF number string into a
     * {@link KsefNumber} before delegating. Throws
     * {@link IllegalArgumentException} on invalid input.
     */
    public byte[] getUpoByKsefNumber(String referenceNumber, String ksefNumber) {
        return getUpoByKsefNumber(referenceNumber, KsefNumber.parse(ksefNumber));
    }
}
