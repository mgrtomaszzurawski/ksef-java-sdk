/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.SendInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
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

    private final SessionClient sessionClient;
    private final String referenceNumber;
    private final byte[] aesKey;
    private final byte[] initVector;
    @Nullable private final OffsetDateTime validUntil;
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
        this(sessionClient, referenceNumber, aesKey, initVector, null);
    }

    OnlineSessionImpl(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector,
                @Nullable OffsetDateTime validUntil) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
        this.aesKey = aesKey;
        this.initVector = initVector;
        this.validUntil = validUntil;
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
    public SendInvoiceResult send(byte[] invoiceXml) {
        return send(SendInvoiceCommand.normal(invoiceXml));
    }

    @Override
    public SendInvoiceResult send(SendInvoiceCommand command) {
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

    @Override
    public SendInvoiceResult sendTechnicalCorrection(byte[] invoiceXml, byte[] hashOfCorrected) {
        return send(SendInvoiceCommand.technicalCorrection(invoiceXml, hashOfCorrected));
    }

    @Override
    public SendInvoiceResult sendOffline(byte[] invoiceXml) {
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
    public byte[] upo(String invoiceReferenceNumber) {
        return sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
    }

    @Override
    public byte[] upoByKsefNumber(KsefNumber ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    @Override
    public List<byte[]> bulkUpos() {
        SessionStatus current = sessionClient.getStatus(referenceNumber);
        if (current.upo() == null || current.upo().pages() == null || current.upo().pages().isEmpty()) {
            return List.of();
        }
        List<byte[]> pages = new java.util.ArrayList<>(current.upo().pages().size());
        for (var page : current.upo().pages()) {
            pages.add(sessionClient.getUpoByReference(referenceNumber, page.referenceNumber()));
        }
        return List.copyOf(pages);
    }

    @Override
    public String referenceNumber() {
        return referenceNumber;
    }

    @Override
    public ClosedSession archive() {
        ClosedSessionImpl existing = archiveView.get();
        if (existing != null) {
            return existing;
        }
        // First call wins. Run the close-and-poll lifecycle, then publish
        // a ClosedSession view. Subsequent archive()/close() callers see
        // the same instance via the AtomicReference.
        transitionToClosed();
        ClosedSessionImpl view = new ClosedSessionImpl(sessionClient, referenceNumber);
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
        archiveView.compareAndSet(null, new ClosedSessionImpl(sessionClient, referenceNumber));
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
