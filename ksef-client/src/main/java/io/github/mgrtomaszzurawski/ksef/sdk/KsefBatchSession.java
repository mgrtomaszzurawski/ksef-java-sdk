/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * An open KSeF batch session for bulk invoice submission via ZIP package.
 *
 * <p>Unlike {@link KsefSession}, batch sessions do not encrypt invoices individually.
 * Instead, the entire ZIP package is encrypted with the session AES key. The consumer
 * uploads ZIP parts to the URLs provided in {@link #partUploadRequests()}.
 *
 * <p>Batch session flow:
 * <ol>
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode, BatchFileSpec)}</li>
 *   <li>Upload encrypted ZIP parts to the URLs from {@link #partUploadRequests()}</li>
 *   <li>Call {@link #close()} (or use try-with-resources) to finalize</li>
 * </ol>
 *
 * <p>Example:
 * <pre>{@code
 * try (KsefBatchSession batch = client.openBatchSession(FormCode.FA2, batchFileSpec)) {
 *     // Upload ZIP parts to batch.partUploadRequests() URLs
 *     batch.close();
 * }
 * }</pre>
 *
 * @see KsefClient#openBatchSession(FormCode, BatchFileSpec)
 */
public final class KsefBatchSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KsefBatchSession.class);

    private static final int STATUS_CODE_OK = 200;
    private static final int CLOSE_POLL_INITIAL_DELAY_MS = 1000;
    private static final int CLOSE_POLL_MAX_DELAY_MS = 10000;
    private static final int CLOSE_POLL_BACKOFF_MULTIPLIER = 2;
    private static final long CLOSE_TIMEOUT_MS = 60000;
    private static final int STATUS_POLL_DELAY_MS = 3000;
    private static final int STATUS_POLL_MAX_ATTEMPTS = 20;
    private static final String SESSION_BUSY_INDICATOR = "(415)";
    private static final String ERR_SESSION_CLOSED = "Batch session is already closed";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for batch session to become closeable";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";

    private final SessionClient sessionClient;
    private final String referenceNumber;
    private final List<PartUploadRequest> partUploadRequests;
    private volatile boolean closed;

    KsefBatchSession(SessionClient sessionClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
        this.partUploadRequests = List.copyOf(partUploadRequests);
    }

    /**
     * The batch session reference number assigned by KSeF.
     *
     * @return session reference number
     */
    public String referenceNumber() {
        return referenceNumber;
    }

    /**
     * Upload instructions for each batch file part.
     *
     * <p>Each {@link PartUploadRequest} contains the URL, HTTP method, and headers
     * needed to upload one part of the encrypted ZIP package.
     *
     * @return immutable list of part upload requests
     */
    public List<PartUploadRequest> partUploadRequests() {
        return partUploadRequests;
    }

    /**
     * Get the current batch session status.
     * Works on both open and closed sessions.
     *
     * @return session status with invoice counts and processing state
     */
    public SessionStatus status() {
        return sessionClient.getStatus(referenceNumber);
    }

    /**
     * Close this batch session. Sends close request to KSeF, retrying on 415 (session busy),
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

    private void closeWithRetry() {
        long start = System.currentTimeMillis();
        int delay = CLOSE_POLL_INITIAL_DELAY_MS;

        while (elapsed(start) < CLOSE_TIMEOUT_MS) {
            try {
                sessionClient.closeBatch(referenceNumber);
                LOG.info("Closed KSeF batch session {}", referenceNumber);
                return;
            } catch (KsefException exception) {
                if (isSessionBusy(exception)) {
                    LOG.debug("Batch session {} still busy (415), retrying in {}ms",
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
                LOG.info("Batch session {} processing complete", referenceNumber);
                return;
            }
        }
        LOG.warn("Batch session {} polling timed out — processing may not be complete yet",
                referenceNumber);
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
