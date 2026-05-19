/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Batch invoice submission — synchronous, blocking flow that encrypts
 * a fresh session key, splits the encrypted package into parts, uploads,
 * polls until terminal, and downloads UPOs for accepted invoices.
 *
 * <p>Reached via {@link InvoiceSessions#batch()}.
 *
 * <p><strong>Threading warning.</strong> Methods on this interface block
 * the calling thread for minutes to hours depending on batch size and
 * upload bandwidth. KSeF batch can be up to 5 GB. Do not call from UI
 * threads, HTTP request handlers, or reactive framework dispatch threads.
 * Wrap with a dedicated executor for async use.
 *
 * @since 1.0.0
 */
public interface InvoiceBatch {

    /**
     * Submit a batch of invoices synchronously. SDK encrypts every invoice with
     * a fresh AES session key, splits the encrypted ZIP into parts, opens a
     * KSeF batch session, uploads every part, closes the session, polls until
     * KSeF reports a terminal state, and downloads UPOs for accepted invoices.
     *
     * <p>By the time this method returns, every accepted invoice's UPO is
     * already in {@link BatchResult#cleared()}.
     *
     * <p><strong>Form code derivation (R1-6 REWIZJA #2).</strong> The
     * session-level {@link FormCode} is derived from the first invoice's
     * own {@link Invoice#formCode()} — the SDK then validates every other
     * invoice in the list matches (Phase 1 cheap check, R1-19). Passing
     * a mixed-schema list throws {@link IllegalArgumentException} with
     * the index of the first mismatching element before any wire traffic.
     * KSeF accepts only single-schema batch sessions per spec
     * ({@code /sessions/batch} {@code OpenBatchSessionRequest.formCode}
     * is a single field, not an array).
     *
     * <p><strong>Warning:</strong> This method blocks the calling thread for
     * minutes to hours, depending on batch size and upload bandwidth. KSeF
     * batch can be up to 5 GB. Do not call from UI threads, HTTP request
     * handlers, or reactive framework dispatch threads. Wrap with a dedicated
     * executor for async use.
     *
     * @param <I> the static {@link Invoice} subtype submitted — preserved
     *     on the returned {@link BatchResult} so each
     *     {@code cleared().get(i).submitted().invoice()} retains its
     *     typed identity (e.g. {@code Fa3Invoice})
     * @param invoices invoices to submit (non-empty, homogeneous form code)
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     * @throws IllegalArgumentException if {@code invoices} is empty or
     *     contains a mixed-schema element
     */
    <I extends Invoice> BatchResult<I> submit(List<I> invoices, BatchOptions options);

    /**
     * File-streaming variant of {@link #submit(List, BatchOptions)}. Each
     * invoice is read straight from disk into the batch ZIP rather than
     * materialised as a {@code byte[]} in heap. Use this for large batches —
     * e.g. the spec cap of 10 000 invoices (REQ-SESS-41) — so peak heap stays
     * bounded by the chunk-encryption buffer.
     *
     * <p>Files do not carry an inline {@link Invoice#formCode()}; the
     * explicit {@code formCode} parameter is therefore required. The
     * returned {@link BatchResult} is raw-typed
     * ({@code BatchResult<Invoice>}) because file-only inputs cannot
     * propagate a static invoice subtype.
     *
     * <p><strong>Warning:</strong> This method blocks the calling thread for
     * minutes to hours, depending on batch size and upload bandwidth. KSeF
     * batch can be up to 5 GB. Do not call from UI threads, HTTP request
     * handlers, or reactive framework dispatch threads. Wrap with a dedicated
     * executor for async use.
     *
     * @param formCode form code for the batch
     * @param files non-empty list of paths to invoice XML files
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     */
    BatchResult<Invoice> submitFromFiles(FormCode formCode, List<Path> files, BatchOptions options);
}
