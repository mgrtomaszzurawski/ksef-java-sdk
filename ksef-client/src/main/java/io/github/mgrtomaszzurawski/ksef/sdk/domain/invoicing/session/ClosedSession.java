/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import java.util.List;

/**
 * The post-close, read-only handle to a KSeF interactive session.
 *
 * <p>Returned by {@link OnlineSession#complete()} as the explicit
 * transition verb that closes the underlying session and surfaces a
 * type-state-distinct view for read operations and UPO retrieval. Once
 * a session is closed, no new invoices can be sent — the type system
 * enforces that by omitting all {@code send*} methods on this
 * interface.
 *
 * <p>UPO retrieval lives here via the typed
 * {@link #cleared(SubmittedInvoice)} and {@link #allCleared()} accessors
 * — KSeF semantics make UPO retrieval reliably available only after the
 * session is closed, so colocating it here gives the consumer
 * compile-time confidence they aren't blocking on data the server is
 * still computing.
 *
 * <p>{@link #close()} is inherited from {@link Session} and is a
 * no-op idempotent for an already-closed session.
 *
 * @since 0.1.0
 */
public sealed interface ClosedSession extends Session permits ClosedSessionImpl {

    /**
     * Bridge a {@link SubmittedInvoice} to its {@link ClearedInvoice} —
     * embeds the original {@code SubmittedInvoice} chain plus the UPO
     * (raw XAdES bytes + parsed {@code UpoSummary}). Synchronous: blocks
     * polling internally until the UPO is ready or the configured
     * {@code upoRetrievalTimeout} elapses.
     *
     * @param submitted the submission record returned by an earlier
     *     {@code OnlineSession.sendInvoice(...)} on the same session
     * @return the cleared-invoice record
     * @throws NullPointerException if {@code submitted} is null
     * @throws io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException
     *     if the UPO is not available within the configured retrieval timeout
     */
    <I extends Invoice> ClearedInvoice<I> cleared(SubmittedInvoice<I> submitted);

    /**
     * Convenience overload for callers that hold only a reference number
     * (e.g. a {@code SubmittedInvoice.referenceNumber()} persisted across
     * a restart). Builds
     * a synthetic {@link SubmittedInvoice} from the latest server query
     * for the given reference and attaches the UPO entry.
     *
     * @param invoiceReferenceNumber the SDK-assigned reference number
     * @return the cleared-invoice record
     */
    ClearedInvoice<Invoice> cleared(String invoiceReferenceNumber);

    /**
     * Bulk-fetch every {@link ClearedInvoice} for this session — covers
     * each UPO page referenced in {@link Session#status()}. Each
     * embedded {@link SubmittedInvoice} is rebuilt from server query
     * data (the original {@code Invoice} sent on this session is not
     * preserved; the embedded {@code Invoice} is the minimal
     * {@code Invoice.fromXml(...)} wrapper for the fetched bytes).
     *
     * @return one {@link ClearedInvoice} per accepted invoice; empty
     *     list when the session has no UPO yet
     */
    List<ClearedInvoice<Invoice>> allCleared();

    /**
     * AutoCloseable contract — for an already-closed session this is a
     * no-op. Documented explicitly to reinforce the lifecycle promise.
     */
    @Override
    void close();
}
