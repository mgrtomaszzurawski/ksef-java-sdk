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
 * <p>Reached via {@link Invoices#batch()}.
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
     * @param formCode form code for the batch — must match every invoice's own
     *     {@link Invoice#formCode()}
     * @param invoices invoices to submit (non-empty)
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     */
    BatchResult submit(FormCode formCode, List<Invoice> invoices, BatchOptions options);

    /**
     * File-streaming variant of {@link #submit(FormCode, List, BatchOptions)}.
     * Each invoice is read straight from disk into the batch ZIP rather than
     * materialised as a {@code byte[]} in heap. Use this for large batches —
     * e.g. the spec cap of 10 000 invoices (REQ-SESS-41) — so peak heap stays
     * bounded by the chunk-encryption buffer.
     *
     * @param formCode form code for the batch
     * @param files non-empty list of paths to invoice XML files
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     */
    BatchResult submitFromFiles(FormCode formCode, List<Path> files, BatchOptions options);
}
