/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Incremental invoice sync — high-water-mark cursor walk over KSeF
 * permanent-storage windows with crash-safe checkpointing.
 *
 * <p>Headline types:
 * <ul>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient}
 *       — orchestrates per-subject-type cursor walks and dispatches
 *       parsed invoices to the caller's {@code InvoiceSink}.</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan}
 *       — immutable plan describing the sync window, subject types,
 *       output directory, and full-content vs metadata-only mode.</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore}
 *       — pluggable persistence interface for the per-subject-type
 *       continuation cursor (commit-after-accept).</li>
 *   <li>{@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSink}
 *       — caller-supplied callback receiving each parsed invoice.</li>
 * </ul>
 *
 * <p>The sync uses {@code InvoiceQueryDateType.PERMANENT_STORAGE}
 * exclusively — see {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan}
 * for the rationale.
 *
 * @since 1.0.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;
