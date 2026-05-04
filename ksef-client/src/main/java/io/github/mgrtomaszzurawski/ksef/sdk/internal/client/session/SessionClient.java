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

    private final HttpSupport http;

    public SessionClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
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
        LOGGER.debug(LOG_CALL_REF, OP_GET_INVOICES, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_INVOICES;
        SessionInvoicesResponseRaw rawValue = http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_INVOICES);
        return InvoicingMappers.toSessionInvoices(rawValue);
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
        LOGGER.debug(LOG_CALL_REF, OP_GET_FAILED, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        String path = ApiPaths.subPath(ApiPaths.SESSIONS, referenceNumber) + SEGMENT_FAILED;
        SessionInvoicesResponseRaw rawValue = http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_FAILED);
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
