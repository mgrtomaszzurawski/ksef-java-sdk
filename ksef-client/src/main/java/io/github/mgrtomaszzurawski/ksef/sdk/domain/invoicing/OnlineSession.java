/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A pre-close, write-capable KSeF interactive session.
 *
 * <p>Returned by {@link InvoiceClient#openSession(FormCode)} when the
 * SDK opens a fresh session — the AES session key + IV are held in JVM
 * memory, so invoice content can be encrypted and submitted. After the
 * session is closed, the AES material is zeroised; the only way to keep
 * accessing the session for read operations / UPO retrieval is to call
 * {@link #archive()} which returns a {@link ClosedSession} view.
 *
 * <p>Idiomatic usage with try-with-resources + explicit archive:
 *
 * <pre>{@code
 * try (OnlineSession os = client.invoices().openSession(FormCode.FA3)) {
 *     SendInvoiceResult r1 = os.send(invoiceXml1);
 *     SendInvoiceResult r2 = os.send(invoiceXml2);
 *
 *     ClosedSession cs = os.archive();    // explicit transition + close
 *     byte[] upo = cs.upo(r1.referenceNumber());
 * }   // implicit close() via try-with-resources — idempotent, no-op
 * }</pre>
 *
 * <p>Fire-and-forget (no UPO retrieval needed):
 *
 * <pre>{@code
 * try (OnlineSession os = client.invoices().openSession(FormCode.FA3)) {
 *     for (byte[] xml : invoices) {
 *         os.send(xml);
 *     }
 * }   // implicit close() — terminates the session, no archive view
 * }</pre>
 *
 * <p><strong>Threading.</strong> Implementations are not thread-safe.
 * Use one session instance per thread, or coordinate access externally.
 *
 * <h2>Lifecycle of {@code sendInvoice(Invoice)}</h2>
 *
 * <p>The richer {@code sendInvoice(Invoice)} entry point — accepting the
 * {@code Invoice} open interface and returning a synchronous
 * {@code SubmittedInvoice} — is added in PR12a / PR10. Until then the
 * legacy byte-array sends ({@link #send(byte[])} and friends) are the
 * way to dispatch invoices.
 *
 * @since 1.0.0
 */
public interface OnlineSession extends Session {

    /**
     * Send an invoice within this session.
     *
     * <p>The invoice XML is encrypted with the session's AES key, SHA-256 hashes are
     * computed, and the encrypted payload is sent to KSeF. The consumer never needs
     * to handle encryption or hashing directly.
     *
     * @param invoiceXml raw invoice XML bytes (unencrypted)
     * @return result containing the invoice reference number
     * @throws IllegalStateException if the session is already closed or archived
     */
    SendInvoiceResult send(byte[] invoiceXml);

    /**
     * Send an invoice using an explicit command shape (normal vs offline
     * vs technical correction). REQ-OFFLINE-003.
     */
    SendInvoiceResult send(SendInvoiceCommand command);

    /**
     * Convenience for sending a technical correction (korekta techniczna).
     * Equivalent to {@link #send(SendInvoiceCommand)} with
     * {@link SendInvoiceCommand#technicalCorrection(byte[], byte[])}.
     */
    SendInvoiceResult sendTechnicalCorrection(byte[] invoiceXml, byte[] hashOfCorrected);

    /**
     * Convenience for sending an invoice issued during an offline window.
     * Equivalent to {@link #send(SendInvoiceCommand)} with
     * {@link SendInvoiceCommand#offline(byte[])}.
     */
    SendInvoiceResult sendOffline(byte[] invoiceXml);

    /**
     * Download the UPO (official receipt) for a specific invoice within
     * this session. UPO is only available after the session is closed —
     * calling this on an open session will return whatever the server
     * has at that moment, which may be empty.
     *
     * <p>Available on {@link OnlineSession} for legacy compatibility.
     * Prefer the strongly-typed lifecycle path: close the session via
     * {@link #archive()}, then call {@code upo(...)} on the returned
     * {@link ClosedSession}.
     *
     * @param invoiceReferenceNumber the invoice reference number
     * @return raw UPO bytes (XML)
     */
    byte[] upo(String invoiceReferenceNumber);

    /**
     * Download UPO by KSeF invoice number. See {@link #upo(String)} for
     * the lifecycle caveat — only meaningful post-close.
     *
     * @param ksefNumber the KSeF invoice number (validated structurally)
     * @return raw UPO bytes (XML)
     */
    byte[] upoByKsefNumber(KsefNumber ksefNumber);

    /**
     * Download every bulk-session UPO referenced in
     * {@link Session#status()}. See {@link #upo(String)} for the
     * lifecycle caveat — only meaningful post-close.
     *
     * @return one byte[] per bulk UPO XML page; empty list if no bulk
     *     UPO is yet available.
     */
    List<byte[]> bulkUpos();

    /**
     * Time remaining until {@link Session#validUntil()} relative to the
     * supplied clock. Empty when {@code validUntil} is unknown.
     */
    Optional<Duration> timeToExpiry(Clock clock);

    /**
     * Closes the session AND returns a read-only handle for UPO
     * retrieval. Equivalent to {@link #close()} plus obtaining the
     * archive view in one call.
     *
     * <p>Subsequent {@code archive()} / {@code close()} calls are
     * idempotent — the same {@link ClosedSession} instance is returned,
     * the underlying state machine flips to CLOSED only once.
     *
     * <p>Calling {@link #send(byte[])} (or any other write method) after
     * {@code archive()} throws {@link IllegalStateException} with an
     * informative message.
     *
     * @return the read-only post-close view of this session
     */
    ClosedSession archive();

    /**
     * Returns {@code true} when KSeF would auto-classify an invoice as
     * offline based on its issue date vs. the current invoicing date.
     *
     * <p>Per spec (offline mode auto-determination): an invoice is
     * automatically considered offline when the calendar day of
     * {@code issueDate} is earlier than the calendar day of
     * {@code invoicingDate}. The comparison is by date only, ignoring
     * time-of-day.
     *
     * <p>Spec citation: REQ-OFFLINE-002.
     */
    static boolean shouldUseOfflineMode(LocalDate issueDate, LocalDate invoicingDate) {
        Objects.requireNonNull(issueDate, "issueDate must not be null");
        Objects.requireNonNull(invoicingDate, "invoicingDate must not be null");
        return issueDate.isBefore(invoicingDate);
    }
}
