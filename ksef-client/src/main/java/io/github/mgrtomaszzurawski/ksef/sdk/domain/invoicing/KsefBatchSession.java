/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.PreparedBatchPackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An open KSeF batch session for bulk invoice submission via ZIP package.
 *
 * <p>Unlike {@link KsefSession}, batch sessions do not encrypt invoices individually.
 * Instead, the entire ZIP package is encrypted with the session AES key. The consumer
 * uploads ZIP parts to the URLs provided in {@link #partUploadRequests()}.
 *
 * <p>Batch session flow (manual variant):
 * <ol>
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode, PreparedBatchPackage)}</li>
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
 * @see KsefClient#openBatchSession(FormCode, PreparedBatchPackage)
 * @see KsefClient#openBatchSession(FormCode, java.util.List)
 */
public final class KsefBatchSession implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KsefBatchSession.class);

    private static final int STATUS_CODE_OK = 200;
    private static final int HTTP_STATUS_LOWER_BOUND_OK = 200;
    private static final int HTTP_STATUS_UPPER_BOUND_OK = 300;
    private static final int CLOSE_POLL_INITIAL_DELAY_MS = 1000;
    private static final int CLOSE_POLL_MAX_DELAY_MS = 10000;
    private static final int CLOSE_POLL_BACKOFF_MULTIPLIER = 2;
    private static final long CLOSE_TIMEOUT_MS = 60000;
    private static final int STATUS_POLL_DELAY_MS = 3000;
    /**
     * 5-minute safety budget (100 × 3000ms). Polling exits immediately on any
     * terminal state (code &gt;= 200), so this cap only triggers when KSeF is
     * genuinely stalled.
     */
    private static final int STATUS_POLL_MAX_ATTEMPTS = 100;
    private static final String SESSION_BUSY_INDICATOR = "(415)";
    private static final String ERR_NO_PARTS = "No parts to upload — session was opened with a "
            + "PreparedBatchPackage, not a list of invoice XMLs";
    private static final String ERR_PART_COUNT_MISMATCH = "partUploadRequests count does not match "
            + "part files count";
    private static final String ERR_UPLOAD_FAILED = "Failed to upload batch part %d: HTTP %d";
    private static final String ERR_UPLOAD_INTERRUPTED = "Interrupted while uploading batch parts";
    private static final String ERR_UPLOAD_IO = "I/O error uploading batch part %d";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for batch session to become closeable";
    private static final String METHOD_PUT = "PUT";

    private static final String LOG_PART_UPLOADED = "Uploaded batch part {} to {}";
    private static final String LOG_CLOSED = "Closed KSeF batch session {}";
    private static final String LOG_CLOSE_BUSY_RETRY = "Batch session {} still busy (415), retrying in {}ms";
    private static final String LOG_POLL_TIMEOUT =
            "Batch session {} polling timed out after {} attempts — last status code={} — processing may not be complete yet";
    private static final String LOG_STATUS_TRANSITION =
            "Batch session {} status code transition: {} -> {} (attempt {})";
    private static final String LOG_PROCESSING_COMPLETE = "Batch session {} processing complete";
    private static final String LOG_TERMINAL_FAILURE =
            "Batch session {} reached terminal failure state — code={} description={}";

    private final SessionClient sessionClient;
    private final HttpClient httpClient;
    private final String referenceNumber;
    private final List<PartUploadRequest> partUploadRequests;
    private final BatchPackageBuilder.BatchPackage batchPackage;
    private volatile boolean closed;

    /**
     * Constructor used by tests and by {@link KsefClient} when opening a batch session
     * from a pre-built {@link PreparedBatchPackage} (no SDK-managed part files —
     * the consumer is responsible for uploading via {@link #partUploadRequests()};
     * {@link #uploadParts()} will fail).
     *
     * @apiNote Internal — constructed by {@code KsefClient.openBatchSession(...)}.
     * The {@link SessionClient} parameter type lives in a non-exported package, so
     * this constructor is not callable from consumer code despite being public.
     */
    public KsefBatchSession(SessionClient sessionClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests) {
        this(sessionClient, null, referenceNumber, partUploadRequests, null);
    }

    /**
     * Full constructor — used by {@link KsefClient} when the session was opened from a
     * list of raw invoices and the SDK retains references to the encrypted part files
     * for upload + cleanup.
     *
     * @apiNote Internal — see the alternative-overload note above.
     */
    public KsefBatchSession(SessionClient sessionClient, HttpClient httpClient, String referenceNumber,
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
     * references to the encrypted part files. When the session was opened from a
     * {@link PreparedBatchPackage}, this method throws {@link IllegalStateException}.
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
        } catch (java.io.FileNotFoundException missingFile) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), missingFile);
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
            LOGGER.debug(LOG_PART_UPLOADED, upload.ordinalNumber(), upload.url());
        } catch (IOException ioFailure) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), ioFailure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_UPLOAD_INTERRUPTED, interrupted);
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
                LOGGER.debug(LOG_CLOSED, referenceNumber);
                return;
            } catch (KsefException exception) {
                if (isSessionBusy(exception)) {
                    LOGGER.debug(LOG_CLOSE_BUSY_RETRY, referenceNumber, delay);
                    sleep(delay);
                    delay = Math.min(delay * CLOSE_POLL_BACKOFF_MULTIPLIER, CLOSE_POLL_MAX_DELAY_MS);
                } else {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException(ERR_CLOSE_TIMEOUT);
    }

    /**
     * Polls batch session status until terminal. Any code &gt;= 200 is terminal:
     * 200 = success; others = various failures. Codes &lt; 200 (100=open,
     * 170=closing) are intermediate.
     */
    private void pollUntilComplete() {
        Integer lastCode = null;
        for (int attempt = 0; attempt < STATUS_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(STATUS_POLL_DELAY_MS);
            SessionStatus sessionStatus = sessionClient.getStatus(referenceNumber);
            Integer code = sessionStatus.status() != null ? sessionStatus.status().code() : null;
            lastCode = logStatusTransition(lastCode, code, attempt);
            if (code != null && code >= STATUS_CODE_OK) {
                logTerminalState(code, sessionStatus);
                return;
            }
        }
        LOGGER.warn(LOG_POLL_TIMEOUT, referenceNumber, STATUS_POLL_MAX_ATTEMPTS, lastCode);
    }

    private Integer logStatusTransition(Integer lastCode, Integer code, int attempt) {
        if (code != null && !code.equals(lastCode)) {
            LOGGER.debug(LOG_STATUS_TRANSITION, referenceNumber, lastCode, code, attempt + 1);
            return code;
        }
        return lastCode;
    }

    private void logTerminalState(int code, SessionStatus sessionStatus) {
        if (code == STATUS_CODE_OK) {
            LOGGER.debug(LOG_PROCESSING_COMPLETE, referenceNumber);
            return;
        }
        String description = sessionStatus.status() != null ? sessionStatus.status().description() : null;
        List<String> details = sessionStatus.status() != null
                ? sessionStatus.status().details() : List.of();
        LOGGER.warn(LOG_TERMINAL_FAILURE, referenceNumber, code, description);
        throw new KsefSessionTerminalFailureException(referenceNumber, code, description, details);
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
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, interrupted);
        }
    }
}
