/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A pre-close, write-capable KSeF interactive session.
 *
 * <p>Returned by {@link InvoiceSessions#online(FormCode)} when the
 * SDK opens a fresh session — the AES session key + IV are held in JVM
 * memory, so invoice content can be encrypted and submitted. After the
 * session is closed, the AES material is zeroised; the only way to keep
 * accessing the session for read operations / UPO retrieval is to call
 * {@link #complete()} which closes the session AND returns a
 * {@link ClosedSession} view in one call.
 *
 * <p>Idiomatic usage with try-with-resources + explicit archive:
 *
 * <pre>{@code
 * try (OnlineSession os = client.invoices().sessions().online(FormCode.FA3)) {
 *     SubmittedInvoice r1 = os.sendInvoice(invoice1);
 *     SubmittedInvoice r2 = os.sendInvoice(invoice2);
 *
 *     ClosedSession cs = os.complete();    // explicit transition + close
 *     byte[] upo = cs.upo(r1.referenceNumber());
 * }   // implicit close() via try-with-resources — idempotent, no-op
 * }</pre>
 *
 * <p>Fire-and-forget (no UPO retrieval needed):
 *
 * <pre>{@code
 * try (OnlineSession os = client.invoices().sessions().online(FormCode.FA3)) {
 *     for (Invoice invoice : invoices) {
 *         os.sendInvoice(invoice);
 *     }
 * }   // implicit close() — terminates the session, no archive view
 * }</pre>
 *
 * <p><strong>Threading.</strong> Implementations are not thread-safe.
 * Use one session instance per thread, or coordinate access externally.
 *
 * @since 1.0.0
 */
public sealed interface OnlineSession extends Session permits OnlineSessionImpl {

    /**
     * The {@link FormCode} this session was opened with. Each subsequent
     * {@code sendInvoice*} call must supply an {@link Invoice} whose
     * {@link Invoice#formCode()} matches this value — the SDK fails
     * fast with {@link IllegalArgumentException} on mismatch before
     * any wire traffic (R1-19).
     *
     * @return the session's declared form code
     */
    FormCode formCode();

    /**
     * Send an invoice (the open {@link Invoice} interface) within this
     * session and synchronously wait for KSeF terminal verification.
     *
     * <p>The SDK extracts {@link Invoice#xml()} and {@link Invoice#formCode()},
     * encrypts the XML with the session's AES key, SHA-256-hashes it,
     * and submits it to KSeF. It then polls
     * {@link Session#invoiceStatus(String)} on the assigned reference
     * number until the server reports a terminal status (Accepted /
     * Rejected / etc.). The verification timeout is configured via
     * {@code KsefClient.Builder.invoiceVerificationTimeout(Duration)}
     * (default 60 seconds).
     *
     * <p>On Accepted: the returned {@link SubmittedInvoice} carries the
     * canonical {@link KsefNumber} and a freshly-rendered KOD I QR-code
     * PNG so callers can produce buyer-facing visualisations without an
     * extra round-trip.
     *
     * <p>On Rejected: the returned {@link SubmittedInvoice} carries the
     * server's error details and an empty KSeF number / KOD I QR.
     *
     * <p>On verification timeout: throws
     * {@code KsefSessionPollingTimeoutException}. The submission itself
     * has already happened by then — the caller can re-query
     * {@link Session#invoiceStatus(String)} later if they want to wait
     * longer.
     *
     * @param invoice the invoice to send — must not be null
     * @return synchronous terminal-state result with embedded invoice
     * @throws IllegalStateException if the session is already closed or archived
     * @throws NullPointerException if {@code invoice} is null
     */
    <I extends Invoice> SubmittedInvoice<I> sendInvoice(I invoice);

    /**
     * Send a pre-built {@link OfflineInvoice} within this session.
     *
     * <p>Sets the wire-level {@code offlineMode=true} marker on the
     * request and otherwise mirrors the {@link #sendInvoice(Invoice)}
     * lifecycle: XML validation gate, AES-encryption with the session
     * key, post, terminal-state polling.
     *
     * <p>The {@link OfflineInvoice} carries the KOD I + KOD II PNG
     * bytes pre-rendered at construction time; both are propagated
     * verbatim to the returned {@link SubmittedInvoice} via
     * {@link SubmittedInvoice#kodIQr()} and
     * {@link SubmittedInvoice#kodIIQr()} regardless of the terminal
     * status code (because the offline visualisation must show the
     * KOD II QR even before KSeF acceptance per
     * {@code ksef-docs/kody-qr.md}).
     *
     * <p>Spec citation: REQ-OFFLINE-001..007.
     *
     * @param offline the offline invoice to send — must not be null
     * @return synchronous terminal-state result with embedded KOD I + KOD II QRs
     * @throws IllegalStateException if the session is already closed or archived
     * @throws NullPointerException if {@code offline} is null
     */
    <I extends Invoice> SubmittedInvoice<I> sendOfflineInvoice(OfflineInvoice<I> offline);

    /**
     * Convenience overload that signs and packages the invoice on the
     * caller's behalf using the
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider}
     * registered on {@code KsefClient.Builder.offlineSigning(...)}.
     * Equivalent to building an {@link OfflineInvoice} via the provider
     * and passing it to {@link #sendOfflineInvoice(OfflineInvoice)}.
     *
     * <p>Context defaults used by the SDK:
     * <ul>
     *   <li>{@code environment} — derived from the active
     *       {@code KsefEnvironment} configured on the client;</li>
     *   <li>{@code contextType} = {@code NIP}, {@code contextValue} =
     *       seller NIP from the authenticated credentials;</li>
     *   <li>{@code issueDate} = today (UTC).</li>
     * </ul>
     *
     * <p>If a different context kind or non-NIP context value is needed
     * (e.g. EU-entity offline issuance), use
     * {@link #sendOfflineInvoice(OfflineInvoice)} with an explicitly-built
     * {@link OfflineInvoice} via
     * {@code OfflineInvoice.fromInvoice(...)} or a custom
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider}.
     *
     * @throws IllegalStateException if no {@code OfflineSigningProvider}
     *     was configured on the client builder
     */
    <I extends Invoice> SubmittedInvoice<I> sendOffline(I invoice, OfflineMode mode);

    /**
     * Send a technical correction (korekta techniczna) within this
     * session. The {@code hashOfOriginal} is the SHA-256 (32 bytes) of
     * the corrected invoice's XML; KSeF requires it on every technical
     * correction (REQ-OFFLINE-004) and rejects a missing or
     * malformed value.
     *
     * <p>Wire shape: same as a normal send except
     * {@code offlineMode=true} (technical corrections are implicitly
     * offline at the wire level) and the {@code hashOfCorrectedInvoice}
     * field carries {@code hashOfOriginal}.
     *
     * <p>Spec citation: REQ-OFFLINE-003..005;
     * {@code ksef-docs/offline/korekta-techniczna.md}.
     *
     * @param invoice the corrected (replacement) invoice
     * @param hashOfOriginal SHA-256 of the original invoice XML; must be exactly 32 bytes
     * @return synchronous terminal-state result
     * @throws IllegalArgumentException if {@code hashOfOriginal} is not 32 bytes
     * @throws IllegalStateException if the session is already closed or archived
     * @throws NullPointerException if any argument is null
     */
    <I extends Invoice> SubmittedInvoice<I> sendTechnicalCorrection(I invoice, byte[] hashOfOriginal);

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
     * <p>Subsequent {@code complete()} / {@code close()} calls are
     * idempotent — the same {@link ClosedSession} instance is returned,
     * the underlying state machine flips to CLOSED only once.
     *
     * <p>Calling {@link #sendInvoice(Invoice)} (or any other {@code send*}
     * method) after {@code complete()} throws {@link IllegalStateException}
     * with an informative message.
     *
     * @return the read-only post-close view of this session
     */
    ClosedSession complete();

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
