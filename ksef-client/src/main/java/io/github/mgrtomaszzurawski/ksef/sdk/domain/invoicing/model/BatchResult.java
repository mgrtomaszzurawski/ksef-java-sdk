/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Outcome of a synchronous batch submission via {@code Invoices.submitBatch(...)}.
 *
 * <p>Carries one entry in {@code cleared} for every successfully accepted
 * invoice and one entry in {@code failed} for every rejection. Returned
 * only when KSeF has reached a terminal state on the batch session — by
 * the time the call returns, every accepted invoice already has its UPO
 * downloaded.
 *
 * <p><strong>Note on {@code cleared} element type:</strong> currently
 * {@link UpoEntry} (per-invoice ref + UPO XML). PR15 will widen this to
 * {@code ClearedInvoice}, which embeds the full {@code SubmittedInvoice}
 * chain (input invoice + KSeF number + acquisition timestamp + UPO).
 *
 * @param sessionRef the KSeF batch session reference number
 * @param cleared one entry per accepted invoice (raw UPO bytes today;
 *     {@code ClearedInvoice} after PR15)
 * @param failed one entry per rejected invoice
 * @param totalCount total invoices submitted in the batch
 * @param successfulCount equal to {@code cleared.size()}
 * @param failedCount equal to {@code failed.size()}
 * @param processingStartedAt wall-clock time the SDK started the batch
 *     (request kickoff, not server-side)
 * @param processingCompletedAt wall-clock time the SDK observed the
 *     terminal status from KSeF and finished UPO downloads
 *
 * @since 1.0.0
 */
public record BatchResult(
        String sessionRef,
        List<UpoEntry> cleared,
        List<FailedInvoice> failed,
        int totalCount,
        int successfulCount,
        int failedCount,
        OffsetDateTime processingStartedAt,
        OffsetDateTime processingCompletedAt) {

    private static final String ERR_SESSION_REF_NULL = "sessionRef must not be null";
    private static final String ERR_CLEARED_NULL = "cleared must not be null";
    private static final String ERR_FAILED_NULL = "failed must not be null";
    private static final String ERR_STARTED_NULL = "processingStartedAt must not be null";
    private static final String ERR_COMPLETED_NULL = "processingCompletedAt must not be null";

    public BatchResult {
        Objects.requireNonNull(sessionRef, ERR_SESSION_REF_NULL);
        Objects.requireNonNull(cleared, ERR_CLEARED_NULL);
        Objects.requireNonNull(failed, ERR_FAILED_NULL);
        Objects.requireNonNull(processingStartedAt, ERR_STARTED_NULL);
        Objects.requireNonNull(processingCompletedAt, ERR_COMPLETED_NULL);
        cleared = List.copyOf(cleared);
        failed = List.copyOf(failed);
    }
}
