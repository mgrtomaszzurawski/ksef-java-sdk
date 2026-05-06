/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Persistence boundary for incremental sync checkpoints.
 *
 * <p>Consumers implement this to plug their persistence model
 * (database, file, Redis, etc.) into {@link InvoiceSyncClient}.
 *
 * <p><b>Commit semantics</b>: {@code save(...)} is called by the sync
 * orchestrator only AFTER each invoice in the current window has been
 * successfully processed by the {@link InvoiceSink}. This guarantees
 * that resuming from the saved checkpoint never re-processes an invoice
 * that was already accepted, and never skips an invoice that wasn't.
 *
 * <p>Spec citation: ADR (planned) — checkpoint commit semantics.
 *
 * @since 1.0.0
 */
public interface CheckpointStore {

    /**
     * Load the checkpoint for the given subject type, or
     * {@link Optional#empty()} if none has been saved yet (initial run).
     */
    Optional<SyncCheckpoint> load(InvoiceQuerySubjectType subjectType);

    /**
     * Save the checkpoint for the given subject type. Called only after
     * the corresponding window's invoices have all been accepted by the
     * sink.
     */
    void save(InvoiceQuerySubjectType subjectType, SyncCheckpoint checkpoint);

    /**
     * In-memory checkpoint store. Useful for tests and short-lived sync
     * processes that don't need cross-restart resumability.
     */
    static CheckpointStore inMemory() {
        return new InMemoryCheckpointStore();
    }

    /**
     * Default in-memory implementation backed by a thread-safe map.
     */
    final class InMemoryCheckpointStore implements CheckpointStore {

        private final ConcurrentMap<InvoiceQuerySubjectType, SyncCheckpoint> store = new ConcurrentHashMap<>();

        InMemoryCheckpointStore() { }

        @Override
        public Optional<SyncCheckpoint> load(InvoiceQuerySubjectType subjectType) {
            return Optional.ofNullable(store.get(subjectType));
        }

        @Override
        public void save(InvoiceQuerySubjectType subjectType, SyncCheckpoint checkpoint) {
            store.put(subjectType, checkpoint);
        }
    }
}
