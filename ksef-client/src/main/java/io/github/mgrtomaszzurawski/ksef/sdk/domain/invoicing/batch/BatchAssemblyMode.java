/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Where the SDK should assemble + encrypt batch parts before they get
 * uploaded to KSeF. Two strategies:
 *
 * <ul>
 *   <li>{@link OnDisk} — stream the ZIP through chunked AES encryption
 *       to part files in a configurable temp directory. Default for
 *       large batches; only realistic option for full 5 GiB / 50-part
 *       batches. Files are deleted on session close (or earlier on
 *       error / orphaned-cleanup at next {@code KsefClient} startup).</li>
 *   <li>{@link InMemory} — keep everything in heap, capped by an
 *       explicit byte ceiling. Fail-fast when the cap would be
 *       exceeded. Use this in restricted-filesystem environments
 *       (read-only containers, small Lambda {@code /tmp},
 *       {@code SecurityManager} sandboxes).</li>
 * </ul>
 *
 * <p>Use {@link #onDisk()} (default temp directory),
 * {@link #onDisk(Path)} (custom temp directory), or
 * {@link #inMemory(long)} (heap-only with explicit byte cap).
 *
 * @since 1.0.0
 */
public sealed interface BatchAssemblyMode {

    /**
     * Default — stream-through to {@code java.io.tmpdir}. Equivalent to
     * pre-existing behavior.
     */
    static BatchAssemblyMode onDisk() {
        return new OnDisk(null);
    }

    /**
     * Stream-through to a caller-specified temp directory. The
     * directory must exist and be writable; the SDK creates one
     * temp file per batch part, deletes them on session close.
     */
    static BatchAssemblyMode onDisk(Path tempDirectory) {
        return new OnDisk(Objects.requireNonNull(tempDirectory, "tempDirectory must not be null"));
    }

    /**
     * Keep batch parts entirely in heap, capped at {@code maxBytes}
     * across all parts combined (pre-encryption ZIP size).
     * Construction fails fast with {@link IllegalStateException} if
     * the actual batch payload exceeds the cap.
     *
     * @param maxBytes positive byte ceiling; recommended at least
     *     {@code 100_000_000} (one 100 MB part)
     */
    static BatchAssemblyMode inMemory(long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive (was " + maxBytes + ")");
        }
        return new InMemory(maxBytes);
    }

    /**
     * Stream parts to disk. {@code tempDirectory == null} means
     * {@code java.io.tmpdir}.
     */
    record OnDisk(@Nullable Path tempDirectory) implements BatchAssemblyMode { }

    /** Hold parts in heap, fail-fast at {@code maxBytes}. */
    record InMemory(long maxBytes) implements BatchAssemblyMode {
        public InMemory {
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be positive (was " + maxBytes + ")");
            }
        }
    }
}
