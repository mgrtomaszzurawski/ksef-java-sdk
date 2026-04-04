/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoiceStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

/**
 * Client for KSeF session operations — online and batch session lifecycle,
 * invoice submission, and UPO retrieval.
 */
public final class SessionClient {

    private static final String PATH_ONLINE = "/api/v2/sessions/online";
    private static final String PATH_BATCH = "/api/v2/sessions/batch";
    private static final String PATH_SESSION_STATUS = "/api/v2/sessions/";

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
    private final SessionContext sessionContext;

    public SessionClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    // --- Online session ---

    /**
     * Open an online (interactive) session for invoice submission.
     *
     * @param request session opening parameters (form code, encryption info)
     * @return response with session reference number and validity period
     */
    public OpenOnlineSessionResponseRaw openOnline(OpenOnlineSessionRequestRaw request) {
        String token = sessionContext.token();
        return http.postJsonAuthenticated(PATH_ONLINE, request, token,
                OpenOnlineSessionResponseRaw.class, OP_OPEN_ONLINE);
    }

    /**
     * Send an invoice within an open online session.
     *
     * @param referenceNumber the session reference number
     * @param request the invoice payload (encrypted content, hashes, sizes)
     * @return response with the invoice reference number
     */
    public SendInvoiceResponseRaw sendInvoice(String referenceNumber, SendInvoiceRequestRaw request) {
        String token = sessionContext.token();
        String path = PATH_ONLINE + "/" + referenceNumber + SEGMENT_INVOICES;
        return http.postJsonAuthenticated(path, request, token,
                SendInvoiceResponseRaw.class, OP_SEND_INVOICE);
    }

    /**
     * Close an online session. No more invoices can be sent after closing.
     *
     * @param referenceNumber the session reference number
     */
    public void closeOnline(String referenceNumber) {
        String token = sessionContext.token();
        String path = PATH_ONLINE + "/" + referenceNumber + SEGMENT_CLOSE;
        http.postNoBodyAuthenticated(path, token, OP_CLOSE_ONLINE);
    }

    // --- Batch session ---

    /**
     * Open a batch session for bulk invoice submission.
     *
     * @param request batch session opening parameters
     * @return response with session reference number
     */
    public OpenBatchSessionResponseRaw openBatch(OpenBatchSessionRequestRaw request) {
        String token = sessionContext.token();
        return http.postJsonAuthenticated(PATH_BATCH, request, token,
                OpenBatchSessionResponseRaw.class, OP_OPEN_BATCH);
    }

    /**
     * Close a batch session.
     *
     * @param referenceNumber the batch session reference number
     */
    public void closeBatch(String referenceNumber) {
        String token = sessionContext.token();
        String path = PATH_BATCH + "/" + referenceNumber + SEGMENT_CLOSE;
        http.postNoBodyAuthenticated(path, token, OP_CLOSE_BATCH);
    }

    // --- Session status and invoices ---

    /**
     * Get the status of a session (online or batch).
     *
     * @param referenceNumber the session reference number
     * @return session status with invoice counts, UPO availability, etc.
     */
    public SessionStatusResponseRaw getStatus(String referenceNumber) {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_SESSION_STATUS + referenceNumber, token,
                SessionStatusResponseRaw.class, OP_GET_STATUS);
    }

    /**
     * Get invoices submitted within a session.
     *
     * @param referenceNumber the session reference number
     * @return list of invoice metadata
     */
    public SessionInvoicesResponseRaw getInvoices(String referenceNumber) {
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_INVOICES;
        return http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_INVOICES);
    }

    /**
     * Get the status of a specific invoice within a session.
     *
     * @param referenceNumber the session reference number
     * @param invoiceReferenceNumber the invoice reference number
     * @return invoice processing status
     */
    public SessionInvoiceStatusResponseRaw getInvoiceStatus(String referenceNumber, String invoiceReferenceNumber) {
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_INVOICES + "/" + invoiceReferenceNumber;
        return http.getAuthenticated(path, token, SessionInvoiceStatusResponseRaw.class, OP_GET_INVOICE_STATUS);
    }

    /**
     * Get failed invoices within a session.
     *
     * @param referenceNumber the session reference number
     * @return list of failed invoice metadata
     */
    public SessionInvoicesResponseRaw getFailedInvoices(String referenceNumber) {
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_FAILED;
        return http.getAuthenticated(path, token, SessionInvoicesResponseRaw.class, OP_GET_FAILED);
    }

    // --- UPO retrieval ---

    /**
     * Download UPO (official receipt) by UPO reference number.
     *
     * @param referenceNumber the session reference number
     * @param upoReferenceNumber the UPO reference number
     * @return raw UPO bytes (XML)
     */
    public byte[] getUpoByReference(String referenceNumber, String upoReferenceNumber) {
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_UPO + upoReferenceNumber;
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
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_INVOICES + "/"
                + invoiceReferenceNumber + SEGMENT_INVOICE_UPO;
        return http.getAuthenticatedBytes(path, token, OP_GET_UPO_BY_INVOICE);
    }

    /**
     * Download UPO by KSeF invoice number.
     *
     * @param referenceNumber the session reference number
     * @param ksefNumber the KSeF invoice number
     * @return raw UPO bytes (XML)
     */
    public byte[] getUpoByKsefNumber(String referenceNumber, String ksefNumber) {
        String token = sessionContext.token();
        String path = PATH_SESSION_STATUS + referenceNumber + SEGMENT_KSEF_UPO + ksefNumber + SEGMENT_INVOICE_UPO;
        return http.getAuthenticatedBytes(path, token, OP_GET_UPO_BY_KSEF);
    }
}
