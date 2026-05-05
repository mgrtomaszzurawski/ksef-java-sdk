/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

/**
 * Options for opening a KSeF batch session. Codex 2026-05-05 F2.
 *
 * <p>Currently exposes only {@code offlineMode}; the record shape is
 * forward-compatible for future flags (e.g. server-side timeouts,
 * retry policy).
 *
 * <p>{@code offlineMode} controls the {@code offlineMode} field on
 * {@code POST /sessions/batch}. Set {@code true} when the batch's
 * invoices were issued during one of KSeF's offline windows
 * (offline24, offline-niedostępność, awaryjny — see
 * {@code ksef-docs/tryby-offline.md}). All three sub-modes share the
 * same wire field — the legal sub-mode distinction is statutory and
 * affects only the deadline for delivery to KSeF, not the request
 * shape.
 *
 * <p>Use the static factories {@link #online()} and {@link #offline()}
 * for clear call sites:
 * <pre>{@code
 * client.openBatchSession(FormCode.FA2, invoices, BatchSessionOptions.online());
 * client.openBatchSession(FormCode.FA2, invoices, BatchSessionOptions.offline());
 * }</pre>
 *
 * @since 1.0.0
 */
public record BatchSessionOptions(boolean offlineMode) {

    /** Default options — {@code offlineMode=false}. */
    public static BatchSessionOptions online() {
        return new BatchSessionOptions(false);
    }

    /** Offline batch ({@code offlineMode=true}). */
    public static BatchSessionOptions offline() {
        return new BatchSessionOptions(true);
    }
}
