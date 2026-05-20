/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Outcome of a synchronous batch submission via {@code sessions().batch().submit(...)}.
 *
 * <p>Carries one entry in {@code cleared} for every successfully accepted
 * invoice and one entry in {@code failed} for every rejection. Returned
 * only when KSeF has reached a terminal state on the batch session — by
 * the time the call returns, every accepted invoice already has its UPO
 * downloaded.
 *
 * @param <I> the static {@link Invoice} subtype propagated from
 *     {@code InvoiceBatch.submit(List<I>, BatchOptions)}; preserved on
 *     each embedded {@link ClearedInvoice} so callers do not need to
 *     downcast {@code result.cleared().get(0).submitted().invoice()}
 *     back to a typed invoice
 * @param sessionRef the KSeF batch session reference number
 * @param cleared one {@link ClearedInvoice} per accepted invoice — embeds
 *     the full {@code SubmittedInvoice} chain (input invoice + KSeF
 *     number + acceptance timestamp) plus the UPO entry (raw XAdES
 *     bytes available via {@link UpoEntry#xmlBytes()})
 * @param failed one entry per rejected invoice
 * @param totalCount total invoices observed by KSeF on the batch session
 *     (always equals {@code successfulCount + failedCount}; reflects the
 *     server-side reconciled count, which may differ from the count the
 *     SDK uploaded if KSeF rejected the manifest)
 * @param successfulCount equal to {@code cleared.size()}
 * @param failedCount equal to {@code failed.size()}
 * @param processingStartedAt wall-clock time the SDK started the batch
 *     (request kickoff, not server-side)
 * @param processingCompletedAt wall-clock time the SDK observed the
 *     terminal status from KSeF and finished UPO downloads
 *
 * @since 1.0.0
 */
public record BatchResult<I extends Invoice>(
        String sessionRef,
        List<ClearedInvoice<I>> cleared,
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
    private static final String ERR_SUCCESS_COUNT_MISMATCH =
            "successfulCount %d must equal cleared.size() %d";
    private static final String ERR_FAILED_COUNT_MISMATCH =
            "failedCount %d must equal failed.size() %d";
    private static final String ERR_TOTAL_COUNT_MISMATCH =
            "totalCount %d must equal successfulCount + failedCount (%d)";

    public BatchResult {
        Objects.requireNonNull(sessionRef, ERR_SESSION_REF_NULL);
        Objects.requireNonNull(cleared, ERR_CLEARED_NULL);
        Objects.requireNonNull(failed, ERR_FAILED_NULL);
        Objects.requireNonNull(processingStartedAt, ERR_STARTED_NULL);
        Objects.requireNonNull(processingCompletedAt, ERR_COMPLETED_NULL);
        cleared = List.copyOf(cleared);
        failed = List.copyOf(failed);
        if (successfulCount != cleared.size()) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_SUCCESS_COUNT_MISMATCH, successfulCount, cleared.size()));
        }
        if (failedCount != failed.size()) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_FAILED_COUNT_MISMATCH, failedCount, failed.size()));
        }
        if (totalCount != successfulCount + failedCount) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_TOTAL_COUNT_MISMATCH, totalCount, successfulCount + failedCount));
        }
    }
}
