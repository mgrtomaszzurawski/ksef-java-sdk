/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import java.util.List;

/**
 * The post-close, read-only handle to a KSeF interactive session.
 *
 * <p>Returned by {@link OnlineSession#archive()} as the explicit
 * transition verb that closes the underlying session and surfaces a
 * type-state-distinct view for read operations and UPO retrieval. Once
 * a session is closed, no new invoices can be sent — the type system
 * enforces that by omitting all {@code send*} methods on this
 * interface.
 *
 * <p>UPO accessors live here ({@link #upo(String)},
 * {@link #upoByKsefNumber(KsefNumber)}, {@link #bulkUpos()}) — KSeF
 * semantics make UPO retrieval reliably available only after the
 * session is closed, so colocating it here gives the consumer
 * compile-time confidence they aren't blocking on data the server is
 * still computing.
 *
 * <p>{@link #close()} is inherited from {@link Session} and is a
 * no-op idempotent for an already-closed session.
 *
 * <h2>{@code cleared(SubmittedInvoice)} / {@code allCleared()}</h2>
 *
 * <p>The richer cleared-invoice retrieval API — bridging
 * {@code SubmittedInvoice} to {@code ClearedInvoice} (UPO + canonical
 * number + clearance metadata) — is added in PR15. Until then,
 * consumers fetch raw UPO bytes via {@link #upo(String)} or
 * {@link #upoByKsefNumber(KsefNumber)} and parse them externally.
 *
 * @since 1.0.0
 */
public interface ClosedSession extends Session {

    /**
     * Download UPO (official receipt) for a specific invoice.
     *
     * @param invoiceReferenceNumber the invoice reference number
     * @return raw UPO bytes (XML)
     */
    byte[] upo(String invoiceReferenceNumber);

    /**
     * Download UPO by KSeF invoice number. The KSeF number's structure
     * (length, segments, CRC-8) is validated by {@link KsefNumber}.
     *
     * @param ksefNumber the KSeF invoice number
     * @return raw UPO bytes (XML)
     */
    byte[] upoByKsefNumber(KsefNumber ksefNumber);

    /**
     * Download every bulk-session UPO referenced in
     * {@link Session#status()}.
     *
     * @return one byte[] per bulk UPO XML page; empty list if the
     *     session has no bulk UPO yet.
     */
    List<byte[]> bulkUpos();

    /**
     * AutoCloseable contract — for an already-closed session this is a
     * no-op. Documented explicitly to reinforce the lifecycle promise.
     */
    @Override
    void close();
}
