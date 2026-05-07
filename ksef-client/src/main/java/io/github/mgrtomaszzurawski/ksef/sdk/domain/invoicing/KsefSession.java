/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
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
import org.jspecify.annotations.Nullable;
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
 * try (KsefSession session = client.openSession(FormCode.FA3)) {
 *     SendInvoiceResult result = session.send(invoiceXmlBytes);
 *     byte[] upo = session.upo(result.referenceNumber());
 * }
 * }</pre>
 *
 * <p>On {@link #close()}: sends close request to KSeF (retries on 415 "session busy"),
 * then polls until processing completes (status 200). UPO is available after close completes.
 *
 * <p><strong>Not thread-safe.</strong> Use one session instance per thread,
 * or coordinate access externally. Concurrent {@code send(...)} + {@code close()}
 * calls produce undefined ordering — the {@code volatile closed} flag is not
 * a memory barrier against in-flight HTTP requests. {@link KsefClient} itself
 * is thread-safe and supports concurrent session opens.
 *
 * @see KsefClient#openSession(FormCode)
 *
 * @since 1.0.0
 */
public final class KsefSession implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KsefSession.class);

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
    private static final String ERR_SESSION_CLOSED = "Session is already closed";
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

    KsefSession(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector) {
        this(sessionClient, referenceNumber, aesKey, initVector, null);
    }

    KsefSession(SessionClient sessionClient, String referenceNumber,
                byte[] aesKey, byte[] initVector,
                @Nullable OffsetDateTime validUntil) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
        this.aesKey = aesKey;
        this.initVector = initVector;
        this.validUntil = validUntil;
    }

    /**
     * Session expiration timestamp captured from the open-session
     * response (Codex 2026-05-05 F8a). May be empty for sessions
     * constructed via legacy paths or test fixtures that don't pass it.
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
        return send(SendInvoiceCommand.normal(invoiceXml));
    }

    /**
     * Send an invoice within this session using the supplied command shape
     * (normal vs technical correction). REQ-OFFLINE-003.
     *
     * <p>Encrypts with the session's AES key, computes hashes, and posts
     * the encrypted payload. For
     * {@link SendInvoiceCommand.TechnicalCorrection} the
     * {@code hashOfCorrectedInvoice} field is forwarded and offline mode
     * is implied at the wire level.
     */
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

    /**
     * Convenience for sending a technical correction (korekta techniczna).
     * Equivalent to
     * {@code send(SendInvoiceCommand.technicalCorrection(invoiceXml, hashOfCorrected))}.
     * REQ-OFFLINE-003.
     */
    public SendInvoiceResult sendTechnicalCorrection(byte[] invoiceXml, byte[] hashOfCorrected) {
        return send(SendInvoiceCommand.technicalCorrection(invoiceXml, hashOfCorrected));
    }

    /**
     * Convenience for sending an invoice issued during an offline window
     * (offline24, awaria, niedostępność). Equivalent to
     * {@code send(SendInvoiceCommand.offline(invoiceXml))}. Codex
     * 2026-05-05 F1.
     */
    public SendInvoiceResult sendOffline(byte[] invoiceXml) {
        return send(SendInvoiceCommand.offline(invoiceXml));
    }

    /**
     * Returns {@code true} when KSeF would auto-classify an invoice as
     * offline based on its issue date vs. the current invoicing date.
     *
     * <p>Per spec ({@code ksef-docs/offline/automatyczne-okreslanie-trybu-offline.md:6-14}):
     * an invoice is automatically considered offline when the calendar
     * day of {@code issueDate} is earlier than the calendar day of
     * {@code invoicingDate}. The comparison is by date only, ignoring
     * time-of-day.
     *
     * <p>Use this helper to decide between {@link SendInvoiceCommand#normal}
     * and an offline-mode send before posting to KSeF, so a server-side
     * mode mismatch does not cause a round-trip failure.
     *
     * <p>Spec citation: REQ-OFFLINE-002.
     */
    public static boolean shouldUseOfflineMode(java.time.LocalDate issueDate,
                                                java.time.LocalDate invoicingDate) {
        Objects.requireNonNull(issueDate, "issueDate must not be null");
        Objects.requireNonNull(invoicingDate, "invoicingDate must not be null");
        return issueDate.isBefore(invoicingDate);
    }

    /**
     * Get the current session status.
     * Works on both open and closed sessions.
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
        // Codex round-9 manual-validation A.2.3 — single-page getInvoices()
        // dropped invoices for sessions with > pageSize entries. Iterate the
        // x-continuation-token cursor internally and return one combined page.
        return new SessionInvoices(null, sessionClient.getAllInvoices(referenceNumber));
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
        return new SessionInvoices(null, sessionClient.getAllFailedInvoices(referenceNumber));
    }

    /**
     * Download UPO (official receipt) for a specific invoice.
     * Works on closed sessions — UPO is only available after close completes.
     *
     * @param invoiceReferenceNumber the invoice reference number
     * @return raw UPO bytes (XML)
     */
    public byte[] upo(String invoiceReferenceNumber) {
        return sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
    }

    /**
     * Download UPO (official receipt) by KSeF invoice number. Works on
     * closed sessions — UPO is only available after close completes.
     * The KSeF number's structure (length, segments, CRC-8) is validated
     * by {@link io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber}.
     *
     * @param ksefNumber the KSeF invoice number
     * @return raw UPO bytes (XML)
     */
    public byte[] upoByKsefNumber(io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    /**
     * Convenience overload that parses the raw KSeF number string before
     * delegating. Throws {@link IllegalArgumentException} on invalid input.
     *
     * @param ksefNumber the KSeF invoice number as a raw string
     * @return raw UPO bytes (XML)
     */
    public byte[] upoByKsefNumber(String ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    /**
     * Download every bulk-session UPO referenced in
     * {@link SessionStatus#upo()}. The KSeF spec
     * ({@code faktury/sesje/sesja-sprawdzenie-stanu-i-pobranie-upo.md}) returns
     * one or more {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoPage}
     * references after a session reaches terminal status; each page can carry
     * up to ~10 000 invoices. This method fetches the current
     * {@link SessionStatus} and downloads every page in order.
     *
     * <p>Codex round-9 manual-validation A.2.1 — previously the
     * {@code SessionClient.getUpoByReference(...)} endpoint was reachable but
     * unreachable through {@code KsefSession}/{@code KsefBatchSession}; the
     * consumer had to plumb {@code upo.pages[]} themselves.
     *
     * @return one byte[] per bulk UPO XML page, in spec order; empty list
     *     if the session has no bulk UPO yet (typical before terminal close).
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
