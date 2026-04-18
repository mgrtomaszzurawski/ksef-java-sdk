/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.SendInvoiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An open KSeF interactive session for sending invoices.
 *
 * <p>Manages session crypto state (AES key, IV) internally and provides a clean API
 * for sending invoices without exposing encryption details. Use with try-with-resources
 * to ensure the session is properly closed.
 *
 * <p>Example:
 * <pre>{@code
 * try (KsefSession session = client.openSession(FormCode.FA2)) {
 *     SendInvoiceResult result = session.send(invoiceXmlBytes);
 *     byte[] upo = session.upo(result.referenceNumber());
 * }
 * }</pre>
 *
 * <p>On {@link #close()}: sends close request to KSeF (retries on 415 "session busy"),
 * then polls until processing completes (status 200). UPO is available after close completes.
 *
 * @see KsefClient#openSession(FormCode)
 */
public final class KsefSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KsefSession.class);

    private static final int STATUS_CODE_OK = 200;
    private static final int CLOSE_POLL_INITIAL_DELAY_MS = 1000;
    private static final int CLOSE_POLL_MAX_DELAY_MS = 10000;
    private static final int CLOSE_POLL_BACKOFF_MULTIPLIER = 2;
    private static final long CLOSE_TIMEOUT_MS = 60000;
    private static final int STATUS_POLL_DELAY_MS = 3000;
    private static final int STATUS_POLL_MAX_ATTEMPTS = 20;
    private static final String SESSION_BUSY_INDICATOR = "(415)";
    private static final String ERR_SESSION_CLOSED = "Session is already closed";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for session to become closeable";
    private static final String ERR_POLL_TIMEOUT = "Session status polling timed out after close";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";

    private final SessionClient sessionClient;
    private final String referenceNumber;
    private final byte[] aesKey;
    private final byte[] initVector;
    private volatile boolean closed;

    KsefSession(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
        this.aesKey = aesKey;
        this.initVector = initVector;
    }

    /**
     * Send an invoice within this session.
     *
     * <p>The invoice XML is encrypted with the session's AES key, SHA-256 hashes are
     * computed, and the encrypted payload is sent to KSeF. The consumer never needs
     * to handle encryption or hashing directly.
     *
     * @param invoiceXml raw invoice XML bytes (unencrypted)
     * @return result containing the invoice reference number
     * @throws IllegalStateException if the session is already closed
     */
    public SendInvoiceResult send(byte[] invoiceXml) {
        ensureOpen();
        var request = SendInvoiceBuilder.create(invoiceXml, aesKey, initVector).build();
        return sessionClient.sendInvoice(referenceNumber, request);
    }

    /**
     * Get the current session status.
     *
     * @return session status with invoice counts and processing state
     */
    public SessionStatus status() {
        return sessionClient.getStatus(referenceNumber);
    }

    /**
     * Get all invoices submitted within this session.
     *
     * @return submitted invoice metadata
     */
    public SessionInvoices invoices() {
        return sessionClient.getInvoices(referenceNumber);
    }

    /**
     * Get the status of a specific invoice within this session.
     *
     * @param invoiceReferenceNumber the invoice reference number from {@link #send(byte[])}
     * @return invoice processing status
     */
    public SessionInvoiceStatus invoiceStatus(String invoiceReferenceNumber) {
        return sessionClient.getInvoiceStatus(referenceNumber, invoiceReferenceNumber);
    }

    /**
     * Get failed invoices within this session.
     *
     * @return failed invoice metadata
     */
    public SessionInvoices failedInvoices() {
        return sessionClient.getFailedInvoices(referenceNumber);
    }

    /**
     * Download UPO (official receipt) for a specific invoice.
     *
     * <p>UPO is only available after the session is closed and processing is complete
     * (status code 200).
     *
     * @param invoiceReferenceNumber the invoice reference number
     * @return raw UPO bytes (XML)
     */
    public byte[] upo(String invoiceReferenceNumber) {
        return sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
    }

    /**
     * The session reference number assigned by KSeF.
     *
     * @return session reference number
     */
    public String referenceNumber() {
        return referenceNumber;
    }

    /**
     * Close this session. Sends close request to KSeF, retrying on 415 (session busy),
     * then polls until session processing is complete (status 200).
     *
     * <p>This method is idempotent — calling it on an already-closed session is a no-op.
     * It is called automatically when using try-with-resources.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeWithRetry();
        pollUntilComplete();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_SESSION_CLOSED);
        }
    }

    private void closeWithRetry() {
        long start = System.currentTimeMillis();
        int delay = CLOSE_POLL_INITIAL_DELAY_MS;

        while (elapsed(start) < CLOSE_TIMEOUT_MS) {
            try {
                sessionClient.closeOnline(referenceNumber);
                LOG.info("Closed KSeF session {}", referenceNumber);
                return;
            } catch (KsefException exception) {
                if (isSessionBusy(exception)) {
                    LOG.debug("Session {} still busy (415), retrying in {}ms",
                            referenceNumber, delay);
                    sleep(delay);
                    delay = Math.min(delay * CLOSE_POLL_BACKOFF_MULTIPLIER, CLOSE_POLL_MAX_DELAY_MS);
                } else {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException(ERR_CLOSE_TIMEOUT);
    }

    private void pollUntilComplete() {
        for (int attempt = 0; attempt < STATUS_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(STATUS_POLL_DELAY_MS);
            SessionStatus sessionStatus = sessionClient.getStatus(referenceNumber);
            if (sessionStatus.status() != null
                    && sessionStatus.status().code() == STATUS_CODE_OK) {
                LOG.info("Session {} processing complete", referenceNumber);
                return;
            }
        }
        LOG.warn("Session {} polling timed out — UPO may not be available yet", referenceNumber);
    }

    private static boolean isSessionBusy(KsefException exception) {
        String body = exception.responseBody();
        return body != null && body.contains(SESSION_BUSY_INDICATOR);
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, ex);
        }
    }
}
