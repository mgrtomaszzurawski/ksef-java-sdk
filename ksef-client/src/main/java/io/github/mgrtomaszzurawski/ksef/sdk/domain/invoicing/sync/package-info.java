/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Incremental invoice sync — high-water-mark cursor walk over KSeF
 * permanent-storage windows with crash-safe checkpointing.
 *
 * <p>Public consumer types:
 * <ul>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan}
 *       — immutable plan describing the sync window, subject types,
 *       output directory, and full-content vs metadata-only mode.</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore}
 *       — pluggable persistence interface for the per-subject-type
 *       continuation cursor (commit-after-accept).</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint}
 *       — immutable cursor pointer persisted by the {@code CheckpointStore}.</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice}
 *       — decrypted per-invoice element produced by the streaming sync.</li>
 * </ul>
 *
 * <p>Driven through {@code Invoices.sync().asStream(plan, checkpointStore)}.
 * The sync uses {@code InvoiceQueryDateType.PERMANENT_STORAGE}
 * exclusively — see
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan}
 * for the rationale.
 *
 * @since 0.1.0
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

