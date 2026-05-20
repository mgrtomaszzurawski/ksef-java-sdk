/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.InvoiceSessions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceSync;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceExport;


/**
 * Coordinator for KSeF invoice operations grouped into five focused
 * sub-areas. Each accessor returns a narrow interface that owns a single
 * area of responsibility:
 *
 * <ul>
 *   <li>{@link #archive()} — retrieve by KSeF number, reconstruct
 *       cleared invoices, query/stream metadata.</li>
 *   <li>{@link #sessions()} — full session lifecycle:
 *       {@link InvoiceSessions#online(FormCode) online} for
 *       interactive sends, {@link InvoiceSessions#batch() batch} for
 *       package submission, and {@link InvoiceSessions#stream
 *       session-summary} streaming covering both types.</li>
 *   <li>{@link #offline()} — build-only offline issuance:
 *       assemble an {@link OfflineInvoice} (KOD I + KOD II) ready to be
 *       sent through an online session.</li>
 *   <li>{@link #export()} — kick off export jobs and poll for
 *       completion.</li>
 *   <li>{@link #sync()} — incremental sync as a lazy
 *       {@link java.util.stream.Stream}.</li>
 * </ul>
 *
 * @since 0.1.0
 */
public interface Invoices {

    /**
     * Archive-side access: retrieve invoice documents by KSeF number,
     * reconstruct cleared invoices from persisted references, and
     * query/stream invoice metadata.
     */
    InvoiceArchive archive();

    /**
     * Full session lifecycle: open online sessions, drive batch
     * submissions, and stream session summaries.
     */
    InvoiceSessions sessions();

    /**
     * Build-only offline issuance: assemble {@link OfflineInvoice}
     * instances (KOD I + KOD II QR codes, optional technical-correction
     * hash) for later send via
     * {@code sessions().online(formCode).sendOfflineInvoice(...)}.
     */
    OfflineInvoices offline();

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
