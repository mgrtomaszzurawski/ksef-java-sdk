/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * An open KSeF batch session for bulk invoice submission via ZIP package.
 *
 * <p>Unlike {@link KsefSession}, batch sessions do not encrypt invoices individually.
 * Instead, the entire ZIP package is encrypted with the session AES key. The consumer
 * uploads ZIP parts to the URLs provided in {@link #partUploadRequests()}.
 *
 * <p>Batch session flow (manual variant):
 * <ol>
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode, BatchFileSpec)}</li>
 *   <li>Upload encrypted ZIP parts to the URLs from {@link #partUploadRequests()}</li>
 *   <li>Call {@link #close()} (or use try-with-resources) to finalize</li>
 * </ol>
 *
 * <p>Batch session flow (automated variant):
 * <ol>
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode, java.util.List)}
 *       — the SDK builds the encrypted ZIP and computes hashes using temp files</li>
 *   <li>Call {@link #uploadParts()} to push every encrypted part file to its URL</li>
 *   <li>Call {@link #close()} to finalize and delete the temp files</li>
 * </ol>
 *
 * @see KsefClient#openBatchSession(FormCode, BatchFileSpec)
 * @see KsefClient#openBatchSession(FormCode, java.util.List)
 */
public final class KsefBatchSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KsefBatchSession.class);

    private static final int STATUS_CODE_OK = 200;
    private static final int HTTP_STATUS_LOWER_BOUND_OK = 200;
    private static final int HTTP_STATUS_UPPER_BOUND_OK = 300;
    private static final int CLOSE_POLL_INITIAL_DELAY_MS = 1000;
    private static final int CLOSE_POLL_MAX_DELAY_MS = 10000;
    private static final int CLOSE_POLL_BACKOFF_MULTIPLIER = 2;
    private static final long CLOSE_TIMEOUT_MS = 60000;
    private static final int STATUS_POLL_DELAY_MS = 3000;
    private static final int STATUS_POLL_MAX_ATTEMPTS = 20;
    private static final String SESSION_BUSY_INDICATOR = "(415)";
    private static final String ERR_NO_PARTS = "No parts to upload — session was opened with raw "
            + "BatchFileSpec, not invoiceXmls";
    private static final String ERR_PART_COUNT_MISMATCH = "partUploadRequests count does not match "
            + "part files count";
    private static final String ERR_UPLOAD_FAILED = "Failed to upload batch part %d: HTTP %d";
    private static final String ERR_UPLOAD_INTERRUPTED = "Interrupted while uploading batch parts";
    private static final String ERR_UPLOAD_IO = "I/O error uploading batch part %d";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for batch session to become closeable";
    private static final String METHOD_PUT = "PUT";

    private final SessionClient sessionClient;
    private final HttpClient httpClient;
    private final String referenceNumber;
    private final List<PartUploadRequest> partUploadRequests;
    private final BatchPackageBuilder.BatchPackage batchPackage;
    private volatile boolean closed;

    /**
     * Package-private constructor used by tests and by {@link KsefClient} when opening a
     * batch session from a pre-built {@link BatchFileSpec} (no part files available —
     * {@link #uploadParts()} will fail).
     */
    KsefBatchSession(SessionClient sessionClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests) {
        this(sessionClient, null, referenceNumber, partUploadRequests, null);
    }

    /**
     * Full constructor — used by {@link KsefClient} when the session was opened from a
     * list of raw invoices and the SDK retains references to the encrypted part files
     * for upload + cleanup.
     */
    KsefBatchSession(SessionClient sessionClient, HttpClient httpClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests,
                     BatchPackageBuilder.BatchPackage batchPackage) {
        this.sessionClient = sessionClient;
        this.httpClient = httpClient;
        this.referenceNumber = referenceNumber;
        this.partUploadRequests = List.copyOf(partUploadRequests);
        if (batchPackage != null
                && batchPackage.partFiles().size() != partUploadRequests.size()) {
            throw new IllegalArgumentException(ERR_PART_COUNT_MISMATCH);
        }
        this.batchPackage = batchPackage;
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
     * Upload all encrypted batch parts to their respective URLs.
     *
     * <p>Only available when the session was opened via
     * {@link KsefClient#openBatchSession(FormCode, java.util.List)} — that flow keeps
     * references to the encrypted part files. When the session was opened from a raw
     * {@link BatchFileSpec}, this method throws {@link IllegalStateException}.
     *
     * <p>Each part is uploaded with the HTTP method and headers returned by KSeF in the
     * open-session response (typically {@code PUT}). Any non-2xx response aborts the
     * upload and is wrapped in {@link KsefNetworkException}.
     *
     * @throws IllegalStateException if the session has no part files attached
     * @throws KsefNetworkException if any upload fails or the network is interrupted
     */
    public void uploadParts() {
        if (batchPackage == null || httpClient == null) {
            throw new IllegalStateException(ERR_NO_PARTS);
        }
        List<Path> partFiles = batchPackage.partFiles();
        for (int index = 0; index < partUploadRequests.size(); index++) {
            uploadSinglePart(partUploadRequests.get(index), partFiles.get(index));
        }
    }

    private void uploadSinglePart(PartUploadRequest upload, Path partFile) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(upload.url());
        String method = upload.method() != null ? upload.method() : METHOD_PUT;
        try {
            builder.method(method, BodyPublishers.ofFile(partFile));
        } catch (java.io.FileNotFoundException ex) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), ex);
        }
        Map<String, String> headers = upload.headers() != null ? upload.headers() : Map.of();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        try {
            HttpResponse<Void> response = httpClient.send(builder.build(), BodyHandlers.discarding());
            if (response.statusCode() < HTTP_STATUS_LOWER_BOUND_OK
                    || response.statusCode() >= HTTP_STATUS_UPPER_BOUND_OK) {
                throw new KsefNetworkException(
                        String.format(ERR_UPLOAD_FAILED, upload.ordinalNumber(), response.statusCode()),
                        null);
            }
            LOG.debug("Uploaded batch part {} to {}", upload.ordinalNumber(), upload.url());
        } catch (IOException ex) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_UPLOAD_INTERRUPTED, ex);
        }
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
     * polls until session processing is complete (status 200), then deletes any
     * temp part files held by this session.
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
        try {
            closeWithRetry();
            pollUntilComplete();
        } finally {
            if (batchPackage != null) {
                batchPackage.cleanup();
            }
        }
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
