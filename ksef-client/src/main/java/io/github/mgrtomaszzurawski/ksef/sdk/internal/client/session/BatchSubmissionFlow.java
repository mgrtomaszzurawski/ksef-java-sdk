/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BatchFileInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BatchFilePartInfoRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.ValidationIssue;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.BatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FailedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchAssemblyMode;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchFileSpec;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous batch submission flow used by
 * {@code Invoices.submitBatch(...)} / {@code submitBatchFromFiles(...)}.
 *
 * <p>End-to-end pipeline:
 * <ol>
 *   <li>Build the batch package (ZIP + AES-256-CBC encryption + part split)
 *       via {@link BatchPackageBuilder}.</li>
 *   <li>RSA-wrap the AES key with the KSeF SymmetricKeyEncryption certificate
 *       and open the batch session.</li>
 *   <li>Upload every part in parallel (bounded by
 *       {@link BatchOptions#parallelism()}).</li>
 *   <li>Close the batch session, then poll status until terminal or the
 *       supplied {@code BatchOptions.timeout()} elapses.</li>
 *   <li>Fetch UPOs for accepted invoices and the failure breakdown for
 *       rejected ones.</li>
 *   <li>Return a {@link BatchResult} populated from the per-invoice
 *       breakdown.</li>
 * </ol>
 *
 * <p>The whole thing blocks the calling thread — by design. KSeF batch can
 * be up to 5 GB, processing minutes to hours; the SDK never spawns its own
 * executor for the synchronous facade. Callers needing async behaviour wrap
 * the {@code submitBatch} call in a {@link java.util.concurrent.CompletableFuture}.
 *
 * <p>Internal — module-private. Reachable only from
 * {@code InvoicesImpl.submitBatch(...)}.
 *
 * @since 1.0.0
 */
public final class BatchSubmissionFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSubmissionFlow.class);

    /** KSeF terminal-status threshold — codes &gt;= 200 mean processing finished. */
    private static final int TERMINAL_STATUS_THRESHOLD = 200;
    /** Successful terminal-status code. */
    private static final int STATUS_CODE_OK = 200;
    /** HTTP status range for "OK" responses on part-upload PUTs. */
    private static final int HTTP_STATUS_LOWER_BOUND_OK = 200;
    private static final int HTTP_STATUS_UPPER_BOUND_OK = 300;
    /** Polling cadence when waiting for KSeF to reach a terminal session state. */
    private static final long STATUS_POLL_DELAY_MS = 3000L;
    /** Initial back-off when KSeF reports the session is still busy (HTTP 415) on close. */
    private static final long CLOSE_BUSY_INITIAL_DELAY_MS = 1000L;
    /** Upper bound on the close back-off. */
    private static final long CLOSE_BUSY_MAX_DELAY_MS = 10_000L;
    /** Multiplier applied to the close back-off after each retry. */
    private static final int CLOSE_BUSY_BACKOFF_MULTIPLIER = 2;
    /** Worker count below this triggers sequential (in-line) upload — no executor overhead. */
    private static final int SEQUENTIAL_UPLOAD_THRESHOLD = 1;
    /** Deadline-remaining sentinel — values at or below this mean the deadline elapsed. */
    private static final long DEADLINE_EXPIRED_NANOS = 0L;
    /** Total budget for the close-with-busy-retry loop (separate from the overall batch timeout). */
    private static final long CLOSE_TIMEOUT_MS = 60_000L;
    /** Status indicator for HTTP 415 (session-busy) returned by KSeF on close while parts are still in-flight. */
    private static final String SESSION_BUSY_INDICATOR = "(415)";
    private static final String METHOD_PUT = "PUT";
    private static final String SCHEME_HTTPS = "https";
    private static final String LOCALHOST_HOSTNAME = "localhost";

    private static final String LOG_OPENED_SESSION = "Opened KSeF batch session {} for {} invoices";
    private static final String LOG_PROCESSING_COMPLETE = "Batch session {} processing complete";
    private static final String LOG_TERMINAL_FAILURE = "Batch session {} reached terminal failure code={}";
    private static final String LOG_STATUS_TRANSITION = "Batch session {} status code transition: {} -> {} (attempt {})";
    private static final String LOG_PART_UPLOADED = "Uploaded batch part {}";
    private static final String LOG_CLOSED = "Closed KSeF batch session {}";
    private static final String LOG_CLOSE_BUSY_RETRY = "Batch session {} still busy on close, retrying in {}ms";
    private static final String LOG_TOTAL_COUNT_MISMATCH =
            "Batch session {} expected {} invoices, KSeF returned {}; reporting KSeF's count";

    private static final String ERR_FORM_CODE_NULL = "formCode must not be null";
    private static final String ERR_INVOICES_NULL = "invoices must not be null";
    private static final String ERR_FILES_NULL = "files must not be null";
    private static final String ERR_OPTIONS_NULL = "options must not be null";
    private static final String ERR_INVOICES_EMPTY = "invoices must not be empty";
    private static final String ERR_FILES_EMPTY = "files must not be empty";
    private static final String ERR_NULL_INVOICE = "invoice must not be null";
    private static final String ERR_FORM_CODE_MISMATCH =
            "Invoice at index %d declares formCode=%s but the batch is for formCode=%s";
    private static final String ERR_UPLOAD_FAILED = "Failed to upload batch part %d: HTTP %d";
    private static final String ERR_UPLOAD_INTERRUPTED = "Interrupted while uploading batch parts";
    private static final String ERR_UPLOAD_IO = "I/O error uploading batch part %d";
    private static final String ERR_PART_COUNT_MISMATCH = "partUploadRequests count does not match part files count";
    private static final String ERR_INSECURE_UPLOAD_URL = "Refusing to upload batch part over non-HTTPS URL: ";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for batch session to become closeable";
    private static final String ERR_UPLOAD_TIMED_OUT = "Batch upload timed out";
    private static final String ERR_UPLOAD_WORKER_FAILED = "Batch upload worker failed";
    private static final String ERR_DEADLINE_EXCEEDED = "submitBatch deadline exceeded";
    private static final String ERR_UNKNOWN_BATCH_PART_SUBTYPE = "Unknown BatchPart subtype: ";
    private static final String UNKNOWN_FAILURE_DESCRIPTION = "Unknown error";
    private static final String ERR_NULL_SESSION_CLIENT = "sessionClient must not be null";
    private static final String ERR_NULL_HTTP_CLIENT = "httpClient must not be null";
    private static final String ERR_NULL_ENVIRONMENT = "environment must not be null";
    private static final String ERR_NULL_PUBLIC_KEY_RESOLVER = "publicKeyResolver must not be null";
    private static final String ERR_NULL_CLOCK = "clock must not be null";

    /** Worker thread name for batch part uploads — load-bearing for log diagnostics. */
    private static final String UPLOAD_THREAD_NAME = "ksef-batch-upload";
    /** Per-part HTTP request timeout — REQ-SESS-13 (ksef-docs/sesja-wsadowa.md). */
    private static final long UPLOAD_PART_TIMEOUT_MINUTES = 20L;
    /** Plain-HTTP scheme — accepted only for loopback uploads. */
    private static final String SCHEME_HTTP = "http";

    /** Sentinel form-code for the synthetic UPO-only placeholder invoice carried on
     *  rebuilt {@link SubmittedInvoice} entries — see {@link #buildUpoPlaceholderInvoice()}. */
    private static final FormCode UPO_PLACEHOLDER_FORM_CODE = FormCode.custom("UPO", "1", "UPO");
    /** Empty payload for the UPO-only placeholder invoice. The original FA(3)/PEF/PEFKOR
     *  XML is not retained server-side after batch close; consumers needing the canonical
     *  invoice payload must call {@code client.invoices().getByKsefNumber(...)}. */
    private static final byte[] UPO_PLACEHOLDER_XML = new byte[0];

    private static final java.util.regex.Pattern IP_LITERAL_PATTERN = java.util.regex.Pattern.compile(
            "^(?:(?:\\d{1,3}\\.){3}\\d{1,3}|\\[?[0-9a-fA-F]*:[0-9a-fA-F:]*\\]?)$");

    private final SessionClient sessionClient;
    private final HttpClient httpClient;
    private final KsefEnvironment environment;
    private final Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver;
    private final Clock clock;

    public BatchSubmissionFlow(SessionClient sessionClient,
                               HttpClient httpClient,
                               KsefEnvironment environment,
                               Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver) {
        this(sessionClient, httpClient, environment, publicKeyResolver, Clock.systemUTC());
    }

    BatchSubmissionFlow(SessionClient sessionClient,
                        HttpClient httpClient,
                        KsefEnvironment environment,
                        Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver,
                        Clock clock) {
        this.sessionClient = Objects.requireNonNull(sessionClient, ERR_NULL_SESSION_CLIENT);
        this.httpClient = Objects.requireNonNull(httpClient, ERR_NULL_HTTP_CLIENT);
        this.environment = Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
        this.publicKeyResolver = Objects.requireNonNull(publicKeyResolver, ERR_NULL_PUBLIC_KEY_RESOLVER);
        this.clock = Objects.requireNonNull(clock, ERR_NULL_CLOCK);
    }

    /**
     * Submit {@code invoices} as a single KSeF batch. Blocking — by the time
     * the call returns, every accepted invoice already has its UPO downloaded.
     *
     * @param formCode wire form code for the batch
     * @param invoices invoices to send (each must declare a {@link FormCode}
     *     matching {@code formCode})
     * @param options runtime tunables (timeout + parallelism)
     * @return populated {@link BatchResult}
     */
    public BatchResult submit(FormCode formCode, List<Invoice> invoices, BatchOptions options) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(invoices, ERR_INVOICES_NULL);
        Objects.requireNonNull(options, ERR_OPTIONS_NULL);
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException(ERR_INVOICES_EMPTY);
        }
        formCode.assertAllowedOn(environment);
        List<byte[]> invoiceXmls = extractAndValidateXmls(formCode, invoices);
        return runFlow(formCode, options, invoices.size(),
                aesKey -> aesIv -> BatchPackageBuilder.build(invoiceXmls, aesKey, aesIv,
                        BatchAssemblyMode.onDisk()));
    }

    /**
     * Submit invoice files as a single KSeF batch. Streams from disk —
     * suitable for large batches.
     */
    public BatchResult submitFromFiles(FormCode formCode, List<Path> files, BatchOptions options) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(files, ERR_FILES_NULL);
        Objects.requireNonNull(options, ERR_OPTIONS_NULL);
        if (files.isEmpty()) {
            throw new IllegalArgumentException(ERR_FILES_EMPTY);
        }
        formCode.assertAllowedOn(environment);
        return runFlow(formCode, options, files.size(),
                aesKey -> aesIv -> BatchPackageBuilder.buildFromFiles(files, aesKey, aesIv,
                        BatchAssemblyMode.onDisk()));
    }

    private static final java.util.Set<FormCode> XSD_VALIDATED_FORM_CODES = java.util.Set.of(
            FormCode.FA2, FormCode.FA3, FormCode.PEF3, FormCode.PEF_KOR3);

    private static List<byte[]> extractAndValidateXmls(FormCode expected, List<Invoice> invoices) {
        List<byte[]> result = new ArrayList<>(invoices.size());
        for (int index = 0; index < invoices.size(); index++) {
            Invoice invoice = invoices.get(index);
            Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
            FormCode declared = invoice.formCode();
            if (!Objects.equals(declared, expected)) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        ERR_FORM_CODE_MISMATCH, index, declared, expected));
            }
            byte[] xml = invoice.xml();
            preFlightValidate(declared, xml);
            result.add(xml);
        }
        return result;
    }

    private static void preFlightValidate(FormCode formCode, byte[] xml) {
        if (!XSD_VALIDATED_FORM_CODES.contains(formCode)) {
            return;
        }
        List<ValidationIssue> issues = KsefXmlValidator.validate(xml, formCode);
        if (issues.isEmpty()) {
            return;
        }
        boolean hasFailure = false;
        List<String> failureMessages = new ArrayList<>(issues.size());
        for (ValidationIssue issue : issues) {
            failureMessages.add(issue.toString());
            if (issue.severity() == Severity.ERROR || issue.severity() == Severity.FATAL) {
                hasFailure = true;
            }
        }
        if (hasFailure) {
            throw new KsefXmlValidator.KsefXmlValidationException(
                    "Batch invoice XML failed XSD validation: " + String.join("; ", failureMessages),
                    List.copyOf(failureMessages));
        }
    }

    @SuppressWarnings("java:S2629")
    private BatchResult runFlow(FormCode formCode, BatchOptions options, int invoiceCount,
                                Function<byte[], Function<byte[], BatchPackageBuilder.BatchPackage>> packagerFactory) {
        OffsetDateTime startedAt = OffsetDateTime.now(clock);
        long deadlineNanos = nanoNow() + options.timeout().toNanos();

        PublicKey encryptionKey = publicKeyResolver.apply(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        BatchPackageBuilder.BatchPackage pkg = packagerFactory.apply(aesKey).apply(initVector);
        try {
            assertWithinDeadline(deadlineNanos);
            BatchSession session = openBatchSession(formCode, encryptedKey, initVector, pkg.spec());
            String sessionRef = session.referenceNumber();
            LOGGER.debug(LOG_OPENED_SESSION, sessionRef, pkg.parts().size());

            uploadAllParts(session.partUploadRequests(), pkg.parts(), options.parallelism(), deadlineNanos);
            closeWithRetry(sessionRef, deadlineNanos);
            pollUntilTerminal(sessionRef, deadlineNanos);

            return collectResult(sessionRef, invoiceCount, startedAt);
        } finally {
            pkg.cleanup();
        }
    }

    private BatchSession openBatchSession(FormCode formCode, byte[] encryptedKey,
                                           byte[] initVector, BatchFileSpec spec) {
        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(spec))
                .offlineMode(false);
        return sessionClient.openBatch(request);
    }

    private static BatchFileInfoRaw toBatchFileInfoRaw(BatchFileSpec spec) {
        List<BatchFilePartInfoRaw> parts = spec.parts().stream()
                .map(part -> new BatchFilePartInfoRaw()
                        .ordinalNumber(part.ordinalNumber())
                        .fileSize(part.fileSize())
                        .fileHash(part.fileHash()))
                .toList();
        return new BatchFileInfoRaw()
                .fileSize(spec.fileSize())
                .fileHash(spec.fileHash())
                .fileParts(parts);
    }

    private void uploadAllParts(List<PartUploadRequest> uploadRequests, List<BatchPart> parts,
                                int parallelism, long deadlineNanos) {
        if (uploadRequests.size() != parts.size()) {
            throw new IllegalStateException(ERR_PART_COUNT_MISMATCH);
        }
        int workers = Math.min(parallelism, uploadRequests.size());
        if (workers <= SEQUENTIAL_UPLOAD_THRESHOLD) {
            for (int index = 0; index < uploadRequests.size(); index++) {
                assertWithinDeadline(deadlineNanos);
                uploadOnePart(uploadRequests.get(index), parts.get(index));
            }
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable, UPLOAD_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<?>> futures = new ArrayList<>(uploadRequests.size());
            for (int index = 0; index < uploadRequests.size(); index++) {
                final PartUploadRequest upload = uploadRequests.get(index);
                final BatchPart part = parts.get(index);
                futures.add(executor.submit(() -> {
                    assertWithinDeadline(deadlineNanos);
                    uploadOnePart(upload, part);
                    return null;
                }));
            }
            awaitFutures(futures, deadlineNanos);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void awaitFutures(List<Future<?>> futures, long deadlineNanos) {
        for (Future<?> future : futures) {
            try {
                long remainingNanos = deadlineNanos - nanoNow();
                if (remainingNanos <= DEADLINE_EXPIRED_NANOS) {
                    throw new KsefNetworkException(ERR_UPLOAD_TIMED_OUT, null);
                }
                future.get(remainingNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new KsefNetworkException(ERR_UPLOAD_INTERRUPTED, interrupted);
            } catch (ExecutionException executionFailure) {
                Throwable cause = executionFailure.getCause();
                if (cause instanceof RuntimeException runtimeFailure) {
                    throw runtimeFailure;
                }
                throw new KsefNetworkException(ERR_UPLOAD_WORKER_FAILED, cause);
            } catch (java.util.concurrent.TimeoutException timeoutFailure) {
                throw new KsefNetworkException(ERR_UPLOAD_TIMED_OUT, timeoutFailure);
            }
        }
    }

    @SuppressWarnings("java:S2629")
    private void uploadOnePart(PartUploadRequest upload, BatchPart part) {
        if (!isAcceptableUploadScheme(upload.url())) {
            throw new KsefException(
                    ERR_INSECURE_UPLOAD_URL
                            + io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.UriRedaction
                                    .redactQuery(upload.url()),
                    null);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(upload.url())
                .timeout(Duration.ofMinutes(UPLOAD_PART_TIMEOUT_MINUTES));
        String method = upload.method() != null ? upload.method() : METHOD_PUT;
        HttpRequest.BodyPublisher publisher;
        try {
            if (part instanceof BatchPart.OnDiskPart onDisk) {
                publisher = BodyPublishers.ofFile(onDisk.path());
            } else if (part instanceof BatchPart.InMemoryPart inMem) {
                publisher = BodyPublishers.ofInputStream(inMem::openStream);
            } else {
                throw new IllegalStateException(ERR_UNKNOWN_BATCH_PART_SUBTYPE + part.getClass());
            }
        } catch (java.io.FileNotFoundException missingFile) {
            throw new KsefNetworkException(
                    String.format(java.util.Locale.ROOT, ERR_UPLOAD_IO, upload.ordinalNumber()),
                    missingFile);
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
                        String.format(java.util.Locale.ROOT, ERR_UPLOAD_FAILED,
                                upload.ordinalNumber(), response.statusCode()),
                        null);
            }
            LOGGER.debug(LOG_PART_UPLOADED, upload.ordinalNumber());
        } catch (IOException ioFailure) {
            throw new KsefNetworkException(
                    String.format(java.util.Locale.ROOT, ERR_UPLOAD_IO, upload.ordinalNumber()),
                    ioFailure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_UPLOAD_INTERRUPTED, interrupted);
        }
    }

    @SuppressWarnings("java:S2629")
    private void closeWithRetry(String sessionRef, long deadlineNanos) {
        long closeBudgetExpiry = Math.min(
                nanoNow() + Duration.ofMillis(CLOSE_TIMEOUT_MS).toNanos(),
                deadlineNanos);
        long delayMs = CLOSE_BUSY_INITIAL_DELAY_MS;
        while (nanoNow() < closeBudgetExpiry) {
            assertWithinDeadline(deadlineNanos);
            try {
                sessionClient.closeBatch(sessionRef);
                LOGGER.debug(LOG_CLOSED, sessionRef);
                return;
            } catch (KsefException exception) {
                if (isSessionBusy(exception)) {
                    LOGGER.debug(LOG_CLOSE_BUSY_RETRY, sessionRef, delayMs);
                    sleep(delayMs);
                    delayMs = Math.min(delayMs * CLOSE_BUSY_BACKOFF_MULTIPLIER, CLOSE_BUSY_MAX_DELAY_MS);
                } else {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException(ERR_CLOSE_TIMEOUT);
    }

    @SuppressWarnings("java:S2629")
    private void pollUntilTerminal(String sessionRef, long deadlineNanos) {
        Integer lastCode = null;
        int attempt = 0;
        while (true) {
            assertWithinDeadline(deadlineNanos);
            sleep(STATUS_POLL_DELAY_MS);
            attempt++;
            SessionStatus status = sessionClient.getStatus(sessionRef);
            Integer code = status.status() != null ? status.status().code() : null;
            lastCode = logCodeTransition(sessionRef, lastCode, code, attempt);
            if (code != null && code >= TERMINAL_STATUS_THRESHOLD) {
                handleTerminalCode(sessionRef, status, code);
                return;
            }
            if (nanoNow() >= deadlineNanos) {
                throw new KsefSessionPollingTimeoutException(sessionRef, attempt, lastCode);
            }
        }
    }

    private static Integer logCodeTransition(String sessionRef, Integer lastCode, Integer code, int attempt) {
        if (code != null && !code.equals(lastCode)) {
            LOGGER.debug(LOG_STATUS_TRANSITION, sessionRef, lastCode, code, attempt);
            return code;
        }
        return lastCode;
    }

    private static void handleTerminalCode(String sessionRef, SessionStatus status, Integer code) {
        if (code == STATUS_CODE_OK) {
            LOGGER.debug(LOG_PROCESSING_COMPLETE, sessionRef);
            return;
        }
        String description = status.status() != null ? status.status().description() : null;
        List<String> details = status.status() != null ? status.status().details() : List.of();
        LOGGER.warn(LOG_TERMINAL_FAILURE, sessionRef, code);
        throw new KsefSessionTerminalFailureException(sessionRef, code, description, details);
    }

    private static java.util.Optional<io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber>
            parseKsefNumberOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber.parse(value));
        } catch (IllegalArgumentException malformed) {
            LOGGER.warn("Skipping malformed KSeF number {}: {}", value, malformed.getMessage());
            return java.util.Optional.empty();
        }
    }

    private BatchResult collectResult(String sessionRef, int totalCount, OffsetDateTime startedAt) {
        OffsetDateTime completedAt = OffsetDateTime.now(clock);
        List<SessionInvoiceStatus> all = sessionClient.getAllInvoices(sessionRef);
        List<SessionInvoiceStatus> failed = sessionClient.getAllFailedInvoices(sessionRef);

        List<ClearedInvoice> cleared = new ArrayList<>();
        List<FailedInvoice> failures = new ArrayList<>();

        java.util.Set<String> failedRefs = new java.util.HashSet<>();
        for (SessionInvoiceStatus failedInvoice : failed) {
            failedRefs.add(failedInvoice.referenceNumber());
            String error = failedInvoice.status() != null && failedInvoice.status().description() != null
                    ? failedInvoice.status().description()
                    : UNKNOWN_FAILURE_DESCRIPTION;
            List<String> details = failedInvoice.status() != null && failedInvoice.status().details() != null
                    ? failedInvoice.status().details()
                    : List.of();
            failures.add(new FailedInvoice(failedInvoice.referenceNumber(), error, details));
        }
        for (SessionInvoiceStatus invoice : all) {
            if (failedRefs.contains(invoice.referenceNumber())) {
                continue;
            }
            byte[] upo = sessionClient.getUpoByInvoiceReference(sessionRef, invoice.referenceNumber());
            UpoEntry entry = new UpoEntry(invoice.referenceNumber(), upo);
            Invoice placeholder = buildUpoPlaceholderInvoice();
            // Batch flow doesn't re-fetch the archived XML — surface the
            // same UPO-only placeholder content as an InvoiceDocument so
            // ClearedInvoice's typed slot is populated. Consumers needing
            // the real document call client.invoices().getByKsefNumber(...).
            InvoiceDocument documentPlaceholder = InvoiceDocument.fromXml(
                    placeholder.formCode(), placeholder.xml());
            SubmittedInvoice submitted = new SubmittedInvoice(
                    placeholder, invoice.referenceNumber(), invoice,
                    parseKsefNumberOrEmpty(invoice.ksefNumber()),
                    java.util.Optional.empty(), java.util.Optional.empty(), List.of());
            cleared.add(new ClearedInvoice(submitted, documentPlaceholder, entry));
        }
        // BatchResult invariants: successfulCount == cleared.size(), failedCount == failed.size(),
        // totalCount == successful + failed. Caller-supplied totalCount is the *invoice count*
        // submitted to the SDK; if KSeF later disagrees with that count (i.e. all.size() +
        // failures.size() != totalCount), surface it via WARN log + use KSeF's count for the
        // result so the consumer sees what the server actually processed.
        int reconciled = cleared.size() + failures.size();
        if (reconciled != totalCount) {
            LOGGER.warn(LOG_TOTAL_COUNT_MISMATCH, sessionRef, totalCount, reconciled);
        }
        return new BatchResult(sessionRef, cleared, failures,
                reconciled, cleared.size(), failures.size(),
                startedAt, completedAt);
    }

    /**
     * Build the UPO-only placeholder {@link Invoice} carried on rebuilt
     * {@link SubmittedInvoice} entries. The previous implementation built the
     * placeholder from UPO bytes ({@code Invoice.fromXml(formCode, upo)}),
     * which lied about the placeholder content — UPO XAdES bytes are NOT a
     * valid FA/PEF invoice payload. The sentinel below makes the placeholder
     * shape explicit; consumers needing the original invoice XML must call
     * {@code client.invoices().getByKsefNumber(...)}.
     */
    private static Invoice buildUpoPlaceholderInvoice() {
        return Invoice.fromXml(UPO_PLACEHOLDER_FORM_CODE, UPO_PLACEHOLDER_XML);
    }

    private static boolean isSessionBusy(KsefException exception) {
        String body = exception.responseBody();
        return body != null && body.contains(SESSION_BUSY_INDICATOR);
    }

    private static boolean isAcceptableUploadScheme(java.net.URI url) {
        String scheme = url.getScheme();
        if (scheme == null) {
            return false;
        }
        return SCHEME_HTTPS.equalsIgnoreCase(scheme)
                || (SCHEME_HTTP.equalsIgnoreCase(scheme) && isLoopbackHost(url.getHost()));
    }

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
            return java.net.InetAddress.getByName(host).isLoopbackAddress();
        } catch (java.net.UnknownHostException ignored) {
            return false;
        }
    }

    private static long nanoNow() {
        return System.nanoTime();
    }

    private static void assertWithinDeadline(long deadlineNanos) {
        if (nanoNow() >= deadlineNanos) {
            throw new KsefNetworkException(ERR_DEADLINE_EXCEEDED, null);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, interrupted);
        }
    }
}
