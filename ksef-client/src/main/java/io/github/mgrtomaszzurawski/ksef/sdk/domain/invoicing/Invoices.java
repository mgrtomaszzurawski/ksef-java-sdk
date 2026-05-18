/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

/**
 * Coordinator for KSeF invoice operations grouped into five focused
 * sub-areas. Each accessor returns a narrow interface that owns a single
 * area of responsibility:
 *
 * <ul>
 *   <li>{@link #archive()} — retrieve by KSeF number, reconstruct
 *       cleared invoices, query/stream metadata.</li>
 *   <li>{@link #sessions()} — open online sessions and stream session
 *       summaries.</li>
 *   <li>{@link #batch()} — synchronous batch submission (blocking,
 *       minutes to hours).</li>
 *   <li>{@link #export()} — kick off export jobs and poll for
 *       completion.</li>
 *   <li>{@link #sync()} — incremental sync as a lazy
 *       {@link java.util.stream.Stream}.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface Invoices {

    /**
     * Archive-side access: retrieve invoice documents by KSeF number,
     * reconstruct cleared invoices from persisted references, and
     * query/stream invoice metadata.
     */
    InvoiceArchive archive();

    /**
     * Online-session lifecycle: open an interactive session for sending
     * invoices, and stream session summaries.
     */
    InvoiceSessions sessions();

    /**
     * Batch submission: synchronous, blocking flow for sending up to
     * 10 000 invoices in a single encrypted package.
     */
    InvoiceBatch batch();

    /**
     * Invoice export jobs: start an export, poll its status, download
     * and decrypt the resulting package.
     */
    InvoiceExport export();

    /**
     * Incremental sync: lazily walk the KSeF download endpoint and
     * advance a checkpoint per consumed element.
     */
    InvoiceSync sync();
}
