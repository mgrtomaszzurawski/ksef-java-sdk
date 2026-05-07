/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.PreparedBatchPackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode,
 *       PreparedBatchPackage, BatchSessionOptions)}</li>
 *   <li>Upload encrypted ZIP parts to the URLs from {@link #partUploadRequests()}</li>
 *   <li>Call {@link #close()} (or use try-with-resources) to finalize</li>
 * </ol>
 *
 * <p>Batch session flow (automated variant):
 * <ol>
 *   <li>Open batch session via {@link KsefClient#openBatchSession(FormCode,
 *       java.util.List, BatchSessionOptions)} — the SDK builds the
 *       encrypted ZIP and computes hashes using temp files</li>
 *   <li>Call {@link #uploadParts()} to push every encrypted part file to its URL</li>
 *   <li>Call {@link #close()} to finalize and delete the temp files</li>
 * </ol>
 *
 * <p><strong>Not thread-safe.</strong> Use one session instance per thread,
 * or coordinate access externally. Concurrent {@code uploadParts()} +
 * {@code close()} calls produce undefined ordering. {@link KsefClient}
 * itself is thread-safe and supports concurrent batch session opens.
 *
 * @see KsefClient#openBatchSession(FormCode, PreparedBatchPackage, BatchSessionOptions)
 * @see KsefClient#openBatchSession(FormCode, java.util.List, BatchSessionOptions)
 *
 * @since 1.0.0
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
    /**
     * REQ-SESS-13 — KSeF allows 20 minutes per part for the cumulative
     * upload budget. The SDK fails fast if this budget is exceeded
     * mid-upload rather than letting the server reject the request.
     * Spec: {@code ksef-docs/sesja-wsadowa.md:288-293}.
     */
    /** REQ-SESS-13 — minutes per part the server allows for the cumulative upload. */
    public static final long UPLOAD_BUDGET_MINUTES_PER_PART = 20L;
    private static final long UPLOAD_BUDGET_NANOS_PER_PART =
            UPLOAD_BUDGET_MINUTES_PER_PART * 60L * 1_000_000_000L;
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
    private static final String SCHEME_HTTPS = "https";
    private static final String SCHEME_HTTP = "http";
    private static final String LOCALHOST_HOSTNAME = "localhost";
    private static final String ERR_INSECURE_UPLOAD_URL =
            "Refusing to upload batch part over non-HTTPS URL: ";
    /**
     * Matches IPv4 dotted-quad and IPv6 hex-colon literals. Used to gate
     * DNS resolution — only resolve when the host string is already an
     * IP literal, never resolve arbitrary hostnames just to check if
     * they happen to be loopback.
     *
     * <ul>
     *   <li>IPv4 branch: exactly 4 dot-separated 1-3 digit octets. Stricter
     *       than the trivial {@code [0-9.]+} which matched {@code "1"}
     *       and {@code ".."}.</li>
     *   <li>IPv6 branch: requires at least one colon. Stricter than the
     *       trivial {@code [0-9a-fA-F:]+} which matched {@code "aabb"}
     *       (a 4-char hex string the JDK would still feed to DNS,
     *       allowing a hostile resolver to return {@code 127.0.0.1} and
     *       smuggle a non-loopback URL past the loopback gate).</li>
     * </ul>
     */
    private static final Pattern IP_LITERAL_PATTERN = Pattern.compile(
            "^(?:(?:\\d{1,3}\\.){3}\\d{1,3}|\\[?[0-9a-fA-F]*:[0-9a-fA-F:]*\\]?)$");

    /**
     * URL is logged via {@code UriRedaction.redactQuery(URI)} (internal helper)
     * — KSeF returns presigned upload URLs whose query parameters are
     * effectively short-lived bearer credentials (Codex H1).
     */
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
    private final @Nullable HttpClient httpClient;
    private final String referenceNumber;
    private final List<PartUploadRequest> partUploadRequests;
    private final BatchPackageBuilder.@Nullable BatchPackage batchPackage;
    private final java.util.function.LongSupplier nanoTimeSource;
    @Nullable private final OffsetDateTime validUntil;
    private volatile boolean closed;

    /**
     * Package-private — see {@code SessionHandleConstructor} (internal bridge) class-level Javadoc
     * (Codex round-9 fresh review H3).
     */
    KsefBatchSession(SessionClient sessionClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests) {
        this(sessionClient, null, referenceNumber, partUploadRequests, null, System::nanoTime);
    }

    /**
     * Package-private — see {@code SessionHandleConstructor} (internal bridge) class-level Javadoc
     * (Codex round-9 fresh review H3).
     */
    KsefBatchSession(SessionClient sessionClient, @Nullable HttpClient httpClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests,
                     BatchPackageBuilder.@Nullable BatchPackage batchPackage) {
        this(sessionClient, httpClient, referenceNumber, partUploadRequests, batchPackage, System::nanoTime);
    }

    /**
     * Package-private — internal constructor with injectable nano-time source
     * for upload-budget tests; see {@code SessionHandleConstructor} (internal bridge) class-level Javadoc
     * for the construction policy (Codex round-9 fresh review H3).
     */
    KsefBatchSession(SessionClient sessionClient, @Nullable HttpClient httpClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests,
                     BatchPackageBuilder.@Nullable BatchPackage batchPackage,
                     java.util.function.LongSupplier nanoTimeSource) {
        this(sessionClient, httpClient, referenceNumber, partUploadRequests, batchPackage,
                nanoTimeSource, null);
    }

    /**
     * Package-private — canonical constructor with validUntil
     * (Codex 2026-05-05 F8a). The other ctors delegate here with
     * {@code validUntil=null}.
     */
    KsefBatchSession(SessionClient sessionClient, @Nullable HttpClient httpClient, String referenceNumber,
                     List<PartUploadRequest> partUploadRequests,
                     BatchPackageBuilder.@Nullable BatchPackage batchPackage,
                     java.util.function.LongSupplier nanoTimeSource,
                     @Nullable OffsetDateTime validUntil) {
        this.sessionClient = sessionClient;
        this.httpClient = httpClient;
        this.referenceNumber = referenceNumber;
        this.partUploadRequests = List.copyOf(partUploadRequests);
        if (batchPackage != null
                && batchPackage.parts().size() != partUploadRequests.size()) {
            throw new IllegalArgumentException(ERR_PART_COUNT_MISMATCH);
        }
        this.batchPackage = batchPackage;
        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
        this.validUntil = validUntil;
    }

    /**
     * Session expiration timestamp captured from the open-batch
     * response (Codex 2026-05-05 F8a). May be empty for sessions
     * constructed via legacy paths or test fixtures.
     *
     * <p>Use {@link #status()} to fetch the current value from the
     * server when freshness matters.
     */
    public Optional<OffsetDateTime> validUntil() {
        return Optional.ofNullable(validUntil);
    }

    /**
     * Time remaining until {@link #validUntil()} relative to the supplied
     * clock. Empty when {@code validUntil} is unknown. Negative durations
     * indicate an already-expired session.
     */
    public Optional<Duration> timeToExpiry(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return validUntil().map(deadline -> Duration.between(clock.instant(), deadline.toInstant()));
    }

    /**
     * Spec-defined upload budget for this session: {@link #UPLOAD_BUDGET_MINUTES_PER_PART}
     * minutes per declared part, applied cumulatively to the entire
     * {@link #uploadParts()} call (REQ-SESS-13). Consumers planning
     * external retry/circuit-breaker logic can use this to compute
     * their own deadline before invoking the SDK.
     *
     * @return total minutes allowed = parts × {@link #UPLOAD_BUDGET_MINUTES_PER_PART}
     * @since 1.0.0
     */
    public Duration uploadBudget() {
        return Duration.ofMinutes(partUploadRequests.size() * UPLOAD_BUDGET_MINUTES_PER_PART);
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
     * {@link KsefClient#openBatchSession(FormCode, java.util.List,
     *     io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions)}
     * — that flow keeps
     * references to the encrypted part files. When the session was opened from a
     * {@link PreparedBatchPackage}, this method throws {@link IllegalStateException}.
     *
     * <p>Each part is uploaded with the HTTP method and headers returned by KSeF in the
     * open-session response (typically {@code PUT}). Any non-2xx response aborts the
     * upload and is wrapped in {@link KsefNetworkException}.
     *
     * <p><strong>Cumulative upload budget (REQ-SESS-13):</strong> the total
     * upload duration is capped at 20 minutes per part. Slow uplinks or
     * very large multi-part batches that exceed the budget receive a
     * {@link KsefNetworkException} mid-upload — pre-size the batch
     * accordingly.
     *
     * @throws IllegalStateException if the session has no part files attached
     * @throws KsefNetworkException if any upload fails, the network is
     *     interrupted, or the cumulative budget is exhausted
     */
    public void uploadParts() {
        if (batchPackage == null || httpClient == null) {
            throw new IllegalStateException(ERR_NO_PARTS);
        }
        List<io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart> partFiles = batchPackage.parts();
        // REQ-SESS-13: KSeF gives 20 minutes per part (cumulative across parts)
        // for the entire upload. Track elapsed time and fail-fast if the
        // budget is about to be exceeded; this surfaces a clean SDK error
        // instead of a server timeout/rejection mid-upload.
        long startNanos = nanoTimeSource.getAsLong();
        long budgetNanos = partUploadRequests.size() * UPLOAD_BUDGET_NANOS_PER_PART;
        for (int index = 0; index < partUploadRequests.size(); index++) {
            long elapsedNanos = nanoTimeSource.getAsLong() - startNanos;
            if (elapsedNanos > budgetNanos) {
                throw new KsefNetworkException(
                        String.format("Batch upload deadline exceeded after %d parts; "
                                        + "spec budget is %d minutes per part (REQ-SESS-13)",
                                index, UPLOAD_BUDGET_MINUTES_PER_PART),
                        null);
            }
            uploadSinglePart(partUploadRequests.get(index), partFiles.get(index));
        }
    }

    private void uploadSinglePart(PartUploadRequest upload,
                                  io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart part) {
        // Defense in depth: KSeF only ever returns HTTPS pre-signed URLs,
        // but assert it explicitly so a misbehaving/spoofed presigner
        // cannot trick the SDK into uploading ciphertext over plaintext.
        // Loopback (localhost / 127.x / [::1]) is accepted because the
        // MITM threat does not apply on the local interface and WireMock-
        // backed tests cannot serve TLS without per-test cert plumbing.
        if (!isAcceptableUploadScheme(upload.url())) {
            throw new KsefException(
                    ERR_INSECURE_UPLOAD_URL
                            + io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.UriRedaction
                                    .redactQuery(upload.url()),
                    null);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(upload.url())
                // Performance review CRITICAL: cap the per-request socket
                // timeout at the per-part upload budget so a hung TCP
                // connection mid-upload aborts cleanly rather than
                // deadlocking the call. The outer per-part budget check
                // only fires between parts.
                .timeout(Duration.ofMinutes(UPLOAD_BUDGET_MINUTES_PER_PART));
        String method = upload.method() != null ? upload.method() : METHOD_PUT;
        HttpRequest.BodyPublisher publisher;
        try {
            if (part instanceof io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart.OnDiskPart onDisk) {
                publisher = BodyPublishers.ofFile(onDisk.path());
            } else if (part instanceof io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart.InMemoryPart inMem) {
                // Use openStream() instead of bytes() to avoid the defensive
                // accessor clone — ByteArrayInputStream wraps the internal
                // byte[] without copying and provides read-only access.
                publisher = BodyPublishers.ofInputStream(inMem::openStream);
            } else {
                throw new IllegalStateException("Unknown BatchPart subtype: " + part.getClass());
            }
        } catch (java.io.FileNotFoundException missingFile) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), missingFile);
        }
        builder.method(method, publisher);
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
            LOGGER.debug(LOG_PART_UPLOADED, upload.ordinalNumber(),
                    io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.UriRedaction.redactQuery(upload.url()));
        } catch (IOException ioFailure) {
            throw new KsefNetworkException(
                    String.format(ERR_UPLOAD_IO, upload.ordinalNumber()), ioFailure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_UPLOAD_INTERRUPTED, interrupted);
        }
    }

    /**
     * Whether the supplied upload URL scheme is safe to PUT batch
     * ciphertext to. Accepts HTTPS unconditionally; accepts HTTP only
     * when the host is a loopback address ({@code localhost},
     * {@code 127.x.x.x}, {@code [::1]}) — MITM threat model does not
     * apply on the local interface and WireMock-based tests cannot
     * easily serve TLS.
     */
    private static boolean isAcceptableUploadScheme(URI url) {
        String scheme = url.getScheme();
        if (scheme == null) {
            return false;
        }
        return SCHEME_HTTPS.equalsIgnoreCase(scheme)
                || (SCHEME_HTTP.equalsIgnoreCase(scheme) && isLoopbackHost(url.getHost()));
    }

    /**
     * Loopback check that never blocks on DNS:
     * <ul>
     *   <li>{@code "localhost"} (literal) — accepted via direct comparison.</li>
     *   <li>IP literal (IPv4 dotted-quad or IPv6) — parsed via
     *       {@link InetAddress#getByName(String)} which does NOT perform
     *       DNS for IP literals (per the JDK Javadoc), then checked via
     *       {@link InetAddress#isLoopbackAddress()}.</li>
     *   <li>Any other hostname — rejected without resolving. Defends
     *       against a malicious upload URL like {@code http://evil.example.com}
     *       triggering an outbound DNS lookup on the upload thread.</li>
     * </ul>
     */
    private static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        if (LOCALHOST_HOSTNAME.equalsIgnoreCase(host)) {
            return true;
        }
        if (!IP_LITERAL_PATTERN.matcher(host).matches()) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException ignored) {
            return false;
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
     * All invoices submitted within this batch session, with the
     * {@code x-continuation-token} cursor followed internally
     * (Codex round-9 manual-validation A.2.2 — mirrors {@code KsefSession.invoices()}).
     */
    public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices invoices() {
        return new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices(
                null, sessionClient.getAllInvoices(referenceNumber));
    }

    /**
     * Status of a specific invoice within this batch session.
     */
    public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus invoiceStatus(
            String invoiceReferenceNumber) {
        return sessionClient.getInvoiceStatus(referenceNumber, invoiceReferenceNumber);
    }

    /**
     * Failed invoices within this batch session, cursor followed internally.
     */
    public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices failedInvoices() {
        return new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices(
                null, sessionClient.getAllFailedInvoices(referenceNumber));
    }

    /**
     * Download UPO XML for a specific invoice in this batch session
     * (post-terminal-close).
     */
    public byte[] upo(String invoiceReferenceNumber) {
        return sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
    }

    /**
     * Download UPO XML for a specific invoice by KSeF number.
     */
    public byte[] upoByKsefNumber(io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    /**
     * Convenience overload that parses the raw KSeF number string.
     */
    public byte[] upoByKsefNumber(String ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    /**
     * Download every bulk-session UPO XML page referenced in
     * {@link SessionStatus#upo()}. Same shape as
     * {@code KsefSession.bulkUpos()} — see Javadoc there for spec context
     * (Codex round-9 manual-validation A.2.1).
     */
    public java.util.List<byte[]> bulkUpos() {
        SessionStatus current = sessionClient.getStatus(referenceNumber);
        if (current.upo() == null || current.upo().pages() == null || current.upo().pages().isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<byte[]> pages = new java.util.ArrayList<>(current.upo().pages().size());
        for (var page : current.upo().pages()) {
            pages.add(sessionClient.getUpoByReference(referenceNumber, page.referenceNumber()));
        }
        return java.util.List.copyOf(pages);
    }

    /**
     * Close this batch session. Sends close request to KSeF, retrying on 415 (session busy),
     * polls until session processing is complete (status 200), then deletes any
     * temp part files held by this session.
     *
     * <p>This method is idempotent — calling it on an already-closed session is a no-op.
     * It is called automatically when using try-with-resources.
     *
     * <p><strong>Polling timeout:</strong> {@code close()} polls the
     * server every 3 seconds for up to 5 minutes (100 attempts) waiting
     * for terminal status. If the server does not reach a terminal state
     * in that window, this method throws
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException}.
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
        throw new KsefSessionPollingTimeoutException(referenceNumber, STATUS_POLL_MAX_ATTEMPTS, lastCode);
    }

    private @Nullable Integer logStatusTransition(@Nullable Integer lastCode, @Nullable Integer code, int attempt) {
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
