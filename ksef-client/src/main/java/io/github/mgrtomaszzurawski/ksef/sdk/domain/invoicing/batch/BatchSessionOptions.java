/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.util.Objects;

/**
 * Options for opening a KSeF batch session.
 *
 * <p>{@code offlineMode} controls the {@code offlineMode} field on
 * {@code POST /sessions/batch}. Set {@code true} when the batch's
 * invoices were issued during one of KSeF's offline windows
 * (offline24, offline-niedostępność, awaryjny — see
 * {@code ksef-docs/tryby-offline.md}).
 *
 * <p>{@code assembly} selects how the SDK builds + encrypts batch
 * parts before upload — either streaming to a temp directory
 * ({@link BatchAssemblyMode#onDisk()} / {@link BatchAssemblyMode#onDisk(java.nio.file.Path)})
 * or in-heap with a byte cap ({@link BatchAssemblyMode#inMemory(long)}).
 *
 * <p>Use the static factories {@link #online()} / {@link #offline()}
 * for clear call sites with default disk-assembly, or build a custom
 * combination via the canonical constructor:
 * <pre>{@code
 * BatchSessionOptions opts = new BatchSessionOptions(
 *         false,
 *         BatchAssemblyMode.inMemory(500_000_000));
 * }</pre>
 *
 * @since 1.0.0
 */
public record BatchSessionOptions(boolean offlineMode, BatchAssemblyMode assembly) {

    private static final String ERR_NULL_ASSEMBLY = "assembly must not be null";

    public BatchSessionOptions {
        Objects.requireNonNull(assembly, ERR_NULL_ASSEMBLY);
    }

    /** Default options — online batch, on-disk assembly to {@code java.io.tmpdir}. */
    public static BatchSessionOptions online() {
        return new BatchSessionOptions(false, BatchAssemblyMode.onDisk());
    }

    /** Offline batch ({@code offlineMode=true}), on-disk assembly to {@code java.io.tmpdir}. */
    public static BatchSessionOptions offline() {
        return new BatchSessionOptions(true, BatchAssemblyMode.onDisk());
    }

    /** Return a copy with the supplied {@link BatchAssemblyMode}. */
    public BatchSessionOptions withAssembly(BatchAssemblyMode mode) {
        return new BatchSessionOptions(offlineMode, mode);
    }
}
