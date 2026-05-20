/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Read-only base contract for a KSeF interactive (online) session.
 *
 * <p>Defines the accessor surface available in every state of the session
 * lifecycle — open, closing, closed. Two type-state specialisations
 * extend this base: {@link OnlineSession} (write-capable, pre-close) and
 * {@link ClosedSession} (read-only, post-close, supports UPO retrieval).
 *
 * <p>The split lets the SDK enforce two compile-time guarantees that a
 * single fat interface could not:
 *
 * <ol>
 *   <li>Sessions enumerated via {@code sessions().stream} (recovered from
 *       the server with no AES key in JVM memory) cannot send invoices —
 *       {@code sendInvoice} lives only on {@link OnlineSession}.</li>
 *   <li>UPO retrieval — only meaningful after the session is closed — is
 *       gated on {@link ClosedSession}, so consumers cannot accidentally
 *       block forever fetching UPOs from a still-open session.</li>
 * </ol>
 *
 * <p>Because {@link AutoCloseable} is the supertype, every {@code Session}
 * is usable with try-with-resources.
 *
 * @since 1.0.0
 */
public sealed interface Session extends AutoCloseable permits OnlineSession, ClosedSession {

    /**
     * The session reference number assigned by KSeF.
     *
     * @return non-null session reference number
     */
    String referenceNumber();

    /**
     * The current session status. Always returns the freshest value the
     * SDK can fetch from KSeF — does not cache.
     */
    SessionStatus status();

    /**
     * When the session was created (server-side).
     */
    OffsetDateTime dateCreated();

    /**
     * Session expiration timestamp captured from the open-session
     * response. May be empty for sessions reconstructed without that
     * information (test fixtures, legacy paths).
     */
    Optional<OffsetDateTime> validUntil();

    /**
     * Total number of invoices submitted within this session, when
     * known. Empty until the server populates the counter — typically
     * available after at least one send completes.
     */
    Optional<Integer> totalInvoiceCount();

    /**
     * Successful invoice count, when known. See
     * {@link #totalInvoiceCount()} for the empty-Optional semantics.
     */
    Optional<Integer> successfulInvoiceCount();

    /**
     * Failed invoice count, when known. See
     * {@link #totalInvoiceCount()} for the empty-Optional semantics.
     */
    Optional<Integer> failedInvoiceCount();

    /**
     * All invoices submitted within this session.
     */
    SessionInvoices invoices();

    /**
     * Status of a specific invoice within this session.
     *
     * @param invoiceRef the invoice reference returned by a previous send
     * @return invoice status snapshot
     */
    SessionInvoiceStatus invoiceStatus(String invoiceRef);

    /**
     * The subset of invoices in this session that the server reports as
     * failed. Convenience for consumers building error reports.
     */
    SessionInvoices failedInvoices();

    /**
     * Close the session. AutoCloseable contract — pure cleanup, returns
     * void, idempotent. Subsequent calls are no-ops.
     *
     * <p>For {@link OnlineSession} this transitions the underlying state
     * to closed (sending the {@code /close} request to KSeF, polling
     * until the session reaches a terminal status, and zeroising the
     * session's AES key + IV from the heap). For {@link ClosedSession}
     * (already-closed view) this is a pure no-op.
     *
     * <p>Use {@link OnlineSession#complete()} when you also need the
     * read-only handle for UPO retrieval after close.
     */
    @Override
    void close();
}
