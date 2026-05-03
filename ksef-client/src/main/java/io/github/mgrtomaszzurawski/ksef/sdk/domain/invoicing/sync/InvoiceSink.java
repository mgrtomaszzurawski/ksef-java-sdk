/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;

/**
 * Consumer-implemented sink that receives each invoice during
 * {@link InvoiceSyncClient#sync}.
 *
 * <p>Called once per invoice. The sync orchestrator commits the
 * matching {@link CheckpointStore} entry only AFTER {@code accept(...)}
 * returns normally, so an exception thrown here aborts checkpoint
 * advancement for the current window.
 *
 * <p>Implementations can do anything: write to a database, push to a
 * queue, accumulate in memory, write to disk. The sink does NOT need
 * to be thread-safe — the sync orchestrator calls it from a single
 * thread.
 */
@FunctionalInterface
public interface InvoiceSink {

    /**
     * Called for each invoice in a sync window.
     *
     * @param ksefNumber the validated KSeF number (length, format,
     *     CRC-8 checked) of this invoice
     * @param metadata the metadata returned by KSeF for this invoice
     */
    void accept(KsefNumber ksefNumber, InvoiceMetadata metadata);
}
