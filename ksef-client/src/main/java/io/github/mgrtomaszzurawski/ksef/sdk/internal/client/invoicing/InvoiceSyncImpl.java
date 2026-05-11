/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceSync;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incremental-sync implementation of {@link InvoiceSync}.
 *
 * @since 1.0.0
 */
public final class InvoiceSyncImpl implements InvoiceSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceSyncImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String OP_SYNC_AS_STREAM = "syncAsStream";

    private static final String ERR_NULL_SYNC_PLAN = "plan must not be null";
    private static final String ERR_NULL_CHECKPOINT_STORE = "checkpointStore must not be null";

    private final HttpRuntime runtime;
    private final InvoiceExport export;

    public InvoiceSyncImpl(HttpRuntime runtime, InvoiceExport export) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
        this.export = Objects.requireNonNull(export, "export must not be null");
    }

    @Override
    public Stream<DecryptedInvoice> asStream(IncrementalSyncPlan plan, CheckpointStore checkpointStore) {
        Objects.requireNonNull(plan, ERR_NULL_SYNC_PLAN);
        Objects.requireNonNull(checkpointStore, ERR_NULL_CHECKPOINT_STORE);
        LOGGER.debug(LOG_CALL, OP_SYNC_AS_STREAM);
        DecryptedInvoiceSyncSpliterator spliterator =
                new DecryptedInvoiceSyncSpliterator(export, runtime.objectMapper(), plan, checkpointStore);
        return java.util.stream.StreamSupport.stream(spliterator, false)
                .onClose(spliterator::close);
    }
}
