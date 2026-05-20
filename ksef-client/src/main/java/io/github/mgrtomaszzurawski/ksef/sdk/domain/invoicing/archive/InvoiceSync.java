/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import java.util.stream.Stream;

/**
 * Incremental sync — lazily walk the KSeF download endpoint, advancing
 * a {@link CheckpointStore} atomically per consumed element.
 *
 * <p>Reached via {@link Invoices#sync()}.
 *
 * @since 0.1.0
 */
public interface InvoiceSync {

    /**
     * Stream-based incremental sync — returns a lazy {@link Stream} of
     * {@link DecryptedInvoice} elements walked across the configured
     * subject types and date windows.
     *
     * <p>Stream is {@link AutoCloseable} — caller MUST consume via
     * try-with-resources to release the underlying paginator and ensure
     * the final checkpoint commit. Each consumed-and-not-skipped element
     * advances the {@code checkpointStore} atomically per element so a
     * caller breaking out early via {@link Stream#limit(long)} leaves the
     * checkpoint at the last successfully consumed element.
     *
     * <p>Spec citation: {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}.
     *
     * @param plan sync configuration
     * @param checkpointStore where checkpoints are persisted between runs
     * @return lazy {@link Stream} of decrypted invoices
     */
    Stream<DecryptedInvoice> asStream(IncrementalSyncPlan plan, CheckpointStore checkpointStore);
}
