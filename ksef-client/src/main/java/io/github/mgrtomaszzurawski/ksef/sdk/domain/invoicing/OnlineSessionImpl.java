/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.builder.SendInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.SendInvoiceCommand;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.InvoiceVerificationConfig;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.OfflineSendHook;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.SessionHandle;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package-private implementation of {@link OnlineSession}. Holds the
 * session AES key + IV, dispatches sends, and runs the close /
 * status-poll state machine.
 *
 * <p>Constructed reflectively by
 * {@code SessionHandleConstructor} — see that class's Javadoc for the
 * cross-package access rationale.
 *
 * <p><strong>Not thread-safe.</strong> Use one session instance per
 * thread, or coordinate access externally. Concurrent {@code send(...)}
 * + {@code close()} calls produce undefined ordering — the
 * {@code volatile closed} flag is not a memory barrier against
 * in-flight HTTP requests. The transition between OPEN and CLOSED is
 * coordinated via a single {@link AtomicReference}, so concurrent
 * {@link #archive()} / {@link #close()} observers see the same
 * {@link ClosedSession} instance.
 *
 * @since 1.0.0
 */
final class OnlineSessionImpl implements OnlineSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineSessionImpl.class);

    private static final int STATUS_CODE_OK = 200;
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
    private static final String ERR_SESSION_CLOSED =
            "Session is already closed (or archived); use the ClosedSession returned by archive()"
                    + " to fetch UPO/clearance — sendInvoice on a closed session is not allowed";
    private static final String ERR_CLOSE_TIMEOUT = "Timeout waiting for session to become closeable";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting";

    private static final String LOG_CLOSED = "Closed KSeF session {}";
    private static final String LOG_CLOSE_BUSY_RETRY = "Session {} still busy (415), retrying in {}ms";
    private static final String LOG_POLL_TIMEOUT =
            "Session {} polling timed out after {} attempts — last status code={} — UPO may not be available yet";
    private static final String LOG_STATUS_TRANSITION =
            "Session {} status code transition: {} -> {} (attempt {})";
    private static final String LOG_PROCESSING_COMPLETE = "Session {} processing complete";
    private static final String LOG_TERMINAL_FAILURE =
            "Session {} reached terminal failure state — code={} description={}";

    /** REQ-SESS-41 — KSeF caps a single session at 10,000 invoices. */
    private static final int MAX_INVOICES_PER_SESSION = 10_000;

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String ERR_SHA256_UNAVAILABLE = "SHA-256 not available";
    private static final String ERR_INVOICE_NULL = "invoice must not be null";
    private static final String ERR_OFFLINE_INVOICE_NULL = "offline must not be null";
    private static final String ERR_OFFLINE_MODE_NULL = "mode must not be null";
    private static final String ERR_SEND_OFFLINE_REQUIRES_PROVIDER =
            "sendOffline(Invoice, OfflineMode) requires an OfflineSigningProvider configured via"
                    + " KsefClient.Builder.offlineSigning(...); fall back to sendOfflineInvoice(OfflineInvoice)"
                    + " if you want to build the OfflineInvoice yourself";
    private static final String ERR_HASH_NULL = "hashOfOriginal must not be null";
    private static final int SHA256_LENGTH_BYTES = 32;
    private static final String ERR_HASH_LENGTH_TEMPLATE =
            "hashOfOriginal must be exactly %d bytes (SHA-256), got %d";
    private static final String ERR_SEND_INVOICE_REQUIRES_FULL_CTOR =
            "sendInvoice(Invoice) requires the verification-aware constructor (KsefEnvironment + timeout);"
                    + " InvoicesImpl wires it automatically — legacy fixtures must use send(byte[]) instead";
    private static final String ERR_FORM_CODE_LEGACY_CTOR =
            "OnlineSession was opened without a FormCode (legacy ctor); "
                    + "formCode() is only meaningful when the session was opened via "
                    + "client.invoices().sessions().online(FormCode).";

    /** Default verification timeout when none is supplied via the builder. */
    static final Duration DEFAULT_VERIFICATION_TIMEOUT = Duration.ofSeconds(60);
    /** Verification poll interval — matches the close-poll cadence (3s). */
    private static final long VERIFICATION_POLL_INTERVAL_MS = STATUS_POLL_DELAY_MS;
    /** A status with code &lt; 200 means KSeF is still processing the invoice. */
    private static final int VERIFICATION_TERMINAL_THRESHOLD = STATUS_CODE_OK;

    private static final String ERR_FORM_CODE_MISMATCH =
            "Invoice formCode %s does not match session formCode %s";

    private final SessionClient sessionClient;
    private final String referenceNumber;
    private final byte[] aesKey;
    private final byte[] initVector;
    @Nullable private final FormCode formCode;
    @Nullable private final OffsetDateTime validUntil;
    @Nullable private final KsefEnvironment environment;
    private final Duration invoiceVerificationTimeout;
    private final QrCodeService qrCodeService;
    @Nullable private final OfflineSigningProvider offlineSigningProvider;
    @Nullable private final String sellerNip;
    private final io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes invoiceTypes;
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicInteger sentInvoiceCount =
            new java.util.concurrent.atomic.AtomicInteger();
    /**
     * Non-null once the session has transitioned to closed (via either
     * {@link #close()} or {@link #archive()}). Both transition methods
     * publish the same {@link ClosedSession} reference into this slot
     * via compare-and-set, so subsequent observers see one canonical
     * archive view.
     */
    private final AtomicReference<ClosedSessionImpl> archiveView = new AtomicReference<>();

    OnlineSessionImpl(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector) {
        this(new SessionHandle(sessionClient, referenceNumber, aesKey, initVector, null),
                new InvoiceVerificationConfig(null, null, DEFAULT_VERIFICATION_TIMEOUT),
                OfflineSendHook.empty());
    }

    OnlineSessionImpl(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector,
                @Nullable OffsetDateTime validUntil) {
        this(new SessionHandle(sessionClient, referenceNumber, aesKey, initVector, null),
                new InvoiceVerificationConfig(validUntil, null, DEFAULT_VERIFICATION_TIMEOUT),
                OfflineSendHook.empty());
    }

    OnlineSessionImpl(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector,
                @Nullable OffsetDateTime validUntil,
                @Nullable KsefEnvironment environment,
                Duration invoiceVerificationTimeout) {
        this(new SessionHandle(sessionClient, referenceNumber, aesKey, initVector, null),
                new InvoiceVerificationConfig(validUntil, environment, invoiceVerificationTimeout),
                OfflineSendHook.empty());
    }

    OnlineSessionImpl(SessionHandle handle, InvoiceVerificationConfig verification, OfflineSendHook offline) {
        this(handle, verification, offline,
                io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes.builtinsOnly());
    }

    OnlineSessionImpl(SessionHandle handle, InvoiceVerificationConfig verification, OfflineSendHook offline,
                io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes invoiceTypes) {
        this.sessionClient = handle.client();
        this.referenceNumber = handle.referenceNumber();
        this.aesKey = handle.aesKey();
        this.initVector = handle.initVector();
        this.formCode = handle.formCode();
        this.validUntil = verification.validUntil();
        this.environment = verification.environment();
        this.invoiceVerificationTimeout = Objects.requireNonNull(verification.invoiceVerificationTimeout(),
                "invoiceVerificationTimeout must not be null");
        this.qrCodeService = new QrCodeService();
        this.offlineSigningProvider = offline.provider();
        this.sellerNip = offline.sellerNip();
        this.invoiceTypes = Objects.requireNonNull(invoiceTypes, "invoiceTypes must not be null");
    }

    @Override
    public FormCode formCode() {
        if (formCode == null) {
            throw new IllegalStateException(ERR_FORM_CODE_LEGACY_CTOR);
        }
        return formCode;
    }

    private void ensureFormCodeMatches(Invoice invoice) {
        if (formCode == null) {
            return;
        }
        FormCode invoiceFormCode = invoice.formCode();
        if (!Objects.equals(invoiceFormCode, formCode)) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_FORM_CODE_MISMATCH, invoiceFormCode, formCode));
        }
    }

    @Override
    public Optional<OffsetDateTime> validUntil() {
        return Optional.ofNullable(validUntil);
    }

    @Override
    public Optional<Duration> timeToExpiry(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return validUntil().map(deadline -> Duration.between(clock.instant(), deadline.toInstant()));
    }

    @Override
    public <I extends Invoice> SubmittedInvoice<I> sendInvoice(I invoice) {
        Objects.requireNonNull(invoice, ERR_INVOICE_NULL);
        ensureFormCodeMatches(invoice);
        if (environment == null) {
            // Legacy 4/5-arg constructors do not carry the environment +
            // timeout pair needed for KOD I QR rendering. Surface the
            // misconfiguration loudly rather than silently producing a
            // half-populated SubmittedInvoice.
            throw new IllegalStateException(ERR_SEND_INVOICE_REQUIRES_FULL_CTOR);
        }
        byte[] invoiceXml = invoice.xml();
        InvoiceValidationGate.validate(invoice.formCode(), invoiceXml);
        // Submit through the existing wire path. SendInvoiceBuilder
        // computes the SHA-256 internally, but we re-compute it here so
        // we own a copy of the digest for the KOD I QR step (the
        // builder's hash is wrapped inside the request and not exposed
        // by the wire response).
        byte[] invoiceSha256 = computeSha256(invoiceXml);
        SendInvoiceResult sendResult = send(invoiceXml);
        SessionInvoiceStatus terminalStatus = pollUntilVerified(sendResult.referenceNumber());
        return buildSubmittedInvoice(invoice, sendResult.referenceNumber(),
                terminalStatus, invoiceSha256);
    }

    @Override
    public <I extends Invoice> SubmittedInvoice<I> sendOfflineInvoice(OfflineInvoice<I> offline) {
        Objects.requireNonNull(offline, ERR_OFFLINE_INVOICE_NULL);
        I underlying = offline.underlyingInvoice();
        ensureFormCodeMatches(underlying);
        byte[] invoiceXml = offline.xml();
        InvoiceValidationGate.validate(offline.formCode(), invoiceXml);
        // When the offline invoice carries hashOfCorrectedInvoice (built
        // via OfflineInvoices.issueTechnicalCorrection or
        // OfflineInvoiceBuilder.hashOfCorrectedInvoice), route through
        // the technical-correction wire shape so the hash reaches the
        // server (REQ-OFFLINE-004, ksef-docs/offline/korekta-techniczna.md).
        SendInvoiceCommand command = offline.hashOfCorrectedInvoice()
                .map(hash -> SendInvoiceCommand.technicalCorrection(invoiceXml, hash))
                .orElseGet(() -> SendInvoiceCommand.offline(invoiceXml));
        SendInvoiceResult sendResult = send(command);
        SessionInvoiceStatus terminalStatus = pollUntilVerified(sendResult.referenceNumber());
        return buildOfflineSubmittedInvoice(underlying, sendResult.referenceNumber(),
                terminalStatus, offline.kodIQrPng(), offline.kodIIQrPng());
    }

    @Override
    public <I extends Invoice> SubmittedInvoice<I> sendOffline(I invoice, OfflineMode mode) {
        Objects.requireNonNull(invoice, ERR_INVOICE_NULL);
        Objects.requireNonNull(mode, ERR_OFFLINE_MODE_NULL);
        ensureFormCodeMatches(invoice);
        if (offlineSigningProvider == null || environment == null || sellerNip == null) {
            throw new IllegalStateException(ERR_SEND_OFFLINE_REQUIRES_PROVIDER);
        }
        QrEnvironment qrEnvironment = QrEnvironment.fromKsefEnvironment(environment);
        io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext context =
                new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext(
                        qrEnvironment, QrContextType.NIP, sellerNip, sellerNip,
                        java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        OfflineInvoice<I> offline = offlineSigningProvider.signAndPackage(invoice, mode, context);
        return sendOfflineInvoice(offline);
    }

    @Override
    public <I extends Invoice> SubmittedInvoice<I> sendTechnicalCorrection(I invoice, byte[] hashOfOriginal) {
        Objects.requireNonNull(invoice, ERR_INVOICE_NULL);
        Objects.requireNonNull(hashOfOriginal, ERR_HASH_NULL);
        ensureFormCodeMatches(invoice);
        if (hashOfOriginal.length != SHA256_LENGTH_BYTES) {
            throw new IllegalArgumentException(String.format(
                    ERR_HASH_LENGTH_TEMPLATE, SHA256_LENGTH_BYTES, hashOfOriginal.length));
        }
        byte[] invoiceXml = invoice.xml();
        InvoiceValidationGate.validate(invoice.formCode(), invoiceXml);
        SendInvoiceResult sendResult = send(SendInvoiceCommand.technicalCorrection(invoiceXml, hashOfOriginal));
        SessionInvoiceStatus terminalStatus = pollUntilVerified(sendResult.referenceNumber());
        // Technical correction is offline at the wire level but we do
        // not synthesise a KOD II here — the caller is responsible for
        // pairing this submission with a KOD II if visualisation is
        // required (the certificate isn't available at this layer).
        return new SubmittedInvoice<>(invoice, sendResult.referenceNumber(),
                terminalStatus, Optional.empty(), Optional.empty(),
                Optional.empty(), List.of());
    }

    private static <I extends Invoice> SubmittedInvoice<I> buildOfflineSubmittedInvoice(
            I invoice, String invoiceRef,
            SessionInvoiceStatus terminalStatus,
            byte[] kodIPng, byte[] kodIIPng) {
        Integer code = terminalStatus.status() != null ? terminalStatus.status().code() : null;
        boolean accepted = code != null && code == STATUS_CODE_OK;
        Optional<KsefNumber> ksefNumber = Optional.empty();
        List<String> errorDetails = List.of();
        if (accepted && terminalStatus.ksefNumber() != null) {
            ksefNumber = Optional.of(terminalStatus.ksefNumber());
        } else if (terminalStatus.status() != null && terminalStatus.status().details() != null) {
            errorDetails = new ArrayList<>(terminalStatus.status().details());
            if (terminalStatus.status().description() != null && errorDetails.isEmpty()) {
                errorDetails.add(terminalStatus.status().description());
            }
        }
        // KOD I + KOD II were rendered at OfflineInvoice construction
        // time — propagate them verbatim. The visualisation must show
        // both QRs on the offline path even on rejection.
        return new SubmittedInvoice<>(invoice, invoiceRef, terminalStatus,
                ksefNumber, Optional.of(kodIPng), Optional.of(kodIIPng), errorDetails);
    }

    private SessionInvoiceStatus pollUntilVerified(String invoiceRef) {
        long deadlineMillis = System.currentTimeMillis() + invoiceVerificationTimeout.toMillis();
        Integer lastCode = null;
        int attempts = 0;
        while (System.currentTimeMillis() < deadlineMillis) {
            attempts++;
            SessionInvoiceStatus snapshot = sessionClient.getInvoiceStatus(referenceNumber, invoiceRef);
            Integer code = snapshot.status() != null ? snapshot.status().code() : null;
            lastCode = code;
            if (code != null && code >= VERIFICATION_TERMINAL_THRESHOLD) {
                return snapshot;
            }
            sleep((int) VERIFICATION_POLL_INTERVAL_MS);
        }
        throw new KsefSessionPollingTimeoutException(invoiceRef, attempts, lastCode);
    }

    private <I extends Invoice> SubmittedInvoice<I> buildSubmittedInvoice(I invoice, String invoiceRef,
                                                   SessionInvoiceStatus terminalStatus,
                                                   byte[] invoiceSha256) {
        Integer code = terminalStatus.status() != null ? terminalStatus.status().code() : null;
        boolean accepted = code != null && code == STATUS_CODE_OK;
        Optional<KsefNumber> ksefNumber = Optional.empty();
        Optional<byte[]> kodIQr = Optional.empty();
        List<String> errorDetails = List.of();
        if (accepted && terminalStatus.ksefNumber() != null) {
            KsefNumber number = terminalStatus.ksefNumber();
            ksefNumber = Optional.of(number);
            kodIQr = Optional.of(renderKodIQr(number, invoiceSha256));
        } else if (terminalStatus.status() != null && terminalStatus.status().details() != null) {
            errorDetails = new ArrayList<>(terminalStatus.status().details());
            if (terminalStatus.status().description() != null && errorDetails.isEmpty()) {
                errorDetails.add(terminalStatus.status().description());
            }
        }
        return new SubmittedInvoice<>(invoice, invoiceRef, terminalStatus,
                ksefNumber, kodIQr, Optional.empty(), errorDetails);
    }

    private byte[] renderKodIQr(KsefNumber ksefNumber, byte[] invoiceSha256) {
        Objects.requireNonNull(environment, "environment must not be null at this point");
        QrEnvironment qrEnvironment = QrEnvironment.fromKsefEnvironment(environment);
        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                qrEnvironment,
                ksefNumber.sellerNip(),
                ksefNumber.acceptanceDate(),
                invoiceSha256);
        return qrCodeService.generateQrCode(url);
    }

    private static byte[] computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new IllegalStateException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        }
    }

    SendInvoiceResult send(byte[] invoiceXml) {
        return send(SendInvoiceCommand.normal(invoiceXml));
    }

    SendInvoiceResult send(SendInvoiceCommand command) {
        ensureOpen();
        Objects.requireNonNull(command, "command must not be null");
        // REQ-SESS-41 — KSeF caps a single session at 10,000 invoices. Reject
        // the (10001)st send before crafting the request so the caller gets a
        // clean SDK error instead of a server rejection.
        int attempted = sentInvoiceCount.incrementAndGet();
        if (attempted > MAX_INVOICES_PER_SESSION) {
            sentInvoiceCount.decrementAndGet();
            throw new IllegalStateException(
                    "KSeF caps a single session at " + MAX_INVOICES_PER_SESSION
                            + " invoices (REQ-SESS-41); already sent "
                            + (attempted - 1) + " in this session — open a new session to send more");
        }
        SendInvoiceBuilder builder = SendInvoiceBuilder.create(command.invoiceXml(), aesKey, initVector);
        if (command instanceof SendInvoiceCommand.TechnicalCorrection correction) {
            builder = builder.technicalCorrection(correction.hashOfCorrectedInvoice());
        } else if (command instanceof SendInvoiceCommand.Offline) {
            builder = builder.offline();
        }
        return sessionClient.sendInvoice(referenceNumber, builder.build());
    }

    SendInvoiceResult sendTechnicalCorrection(byte[] invoiceXml, byte[] hashOfCorrected) {
        return send(SendInvoiceCommand.technicalCorrection(invoiceXml, hashOfCorrected));
    }

    SendInvoiceResult sendOffline(byte[] invoiceXml) {
        return send(SendInvoiceCommand.offline(invoiceXml));
    }

    @Override
    public SessionStatus status() {
        return sessionClient.getStatus(referenceNumber);
    }

    @Override
    public OffsetDateTime dateCreated() {
        return status().dateCreated();
    }

    @Override
    public Optional<Integer> totalInvoiceCount() {
        return Optional.ofNullable(status().invoiceCount());
    }

    @Override
    public Optional<Integer> successfulInvoiceCount() {
        return Optional.ofNullable(status().successfulInvoiceCount());
    }

    @Override
    public Optional<Integer> failedInvoiceCount() {
        return Optional.ofNullable(status().failedInvoiceCount());
    }

    @Override
    public SessionInvoices invoices() {
        // Codex round-9 manual-validation A.2.3 — single-page getInvoices()
        // dropped invoices for sessions with > pageSize entries. Iterate the
        // x-continuation-token cursor internally and return one combined page.
        return new SessionInvoices(null, sessionClient.getAllInvoices(referenceNumber));
    }

    @Override
    public SessionInvoiceStatus invoiceStatus(String invoiceReferenceNumber) {
        return sessionClient.getInvoiceStatus(referenceNumber, invoiceReferenceNumber);
    }

    @Override
    public SessionInvoices failedInvoices() {
        return new SessionInvoices(null, sessionClient.getAllFailedInvoices(referenceNumber));
    }

    @Override
    public String referenceNumber() {
        return referenceNumber;
    }

    @Override
    public ClosedSession complete() {
        ClosedSessionImpl existing = archiveView.get();
        if (existing != null) {
            return existing;
        }
        // First call wins. Run the close-and-poll lifecycle, then publish
        // a ClosedSession view. Subsequent archive()/close() callers see
        // the same instance via the AtomicReference.
        transitionToClosed();
        ClosedSessionImpl view = new ClosedSessionImpl(sessionClient, referenceNumber, invoiceTypes);
        if (archiveView.compareAndSet(null, view)) {
            return view;
        }
        // Lost the race — return the canonical view that won.
        return Objects.requireNonNull(archiveView.get());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // Run the close lifecycle exactly once, identically to archive().
        // We don't expose the ClosedSession view here — close() is the
        // void-returning AutoCloseable contract — but we still set
        // archiveView so that a subsequent archive() returns a coherent
        // (already-closed) handle without re-running the lifecycle.
        transitionToClosed();
        archiveView.compareAndSet(null, new ClosedSessionImpl(sessionClient, referenceNumber, invoiceTypes));
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_SESSION_CLOSED);
        }
    }

    /**
     * Drives the close-and-poll lifecycle. Idempotent — once
     * {@code closed} is set, subsequent calls return immediately. Wipes
     * AES material from the heap on completion regardless of how the
     * close + poll resolved (CWE-316).
     */
    private synchronized void transitionToClosed() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            closeWithRetry();
            pollUntilComplete();
        } finally {
            // CWE-316 hardening — wipe AES key + IV from heap regardless of how
            // close + poll resolved. Symmetric with PreparedInvoiceExport.close().
            // Subsequent send()/upo() calls already fail via the closed flag, so
            // the arrays are unreachable past this point.
            java.util.Arrays.fill(aesKey, (byte) 0);
            java.util.Arrays.fill(initVector, (byte) 0);
        }
    }

    private void closeWithRetry() {
        long start = System.currentTimeMillis();
        int delay = CLOSE_POLL_INITIAL_DELAY_MS;

        while (elapsed(start) < CLOSE_TIMEOUT_MS) {
            try {
                sessionClient.closeOnline(referenceNumber);
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
     * Polls session status until terminal. Any code &gt;= 200 is terminal:
     * 200 = success; 415/440/445/etc. = various failures. Codes &lt; 200
     * (100=open, 170=closing) are intermediate.
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
