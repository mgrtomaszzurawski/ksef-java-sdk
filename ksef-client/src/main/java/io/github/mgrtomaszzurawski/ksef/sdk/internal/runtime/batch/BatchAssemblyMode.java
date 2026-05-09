/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Internal — where {@link BatchPackageBuilder} should assemble + encrypt
 * batch parts before they get uploaded to KSeF. Two strategies:
 *
 * <ul>
 *   <li>{@link OnDisk} — stream the ZIP through chunked AES encryption
 *       to part files in a configurable temp directory. Only realistic
 *       option for full 5 GiB / 50-part batches.</li>
 *   <li>{@link InMemory} — keep everything in heap, capped by an
 *       explicit byte ceiling.</li>
 * </ul>
 *
 * @apiNote Internal — module-private after PR11.
 *
 * @since 1.0.0
 */
public sealed interface BatchAssemblyMode {

    /** Default — stream-through to {@code java.io.tmpdir}. */
    static BatchAssemblyMode onDisk() {
        return new OnDisk(null);
    }

    /** Stream-through to a caller-specified temp directory. */
    static BatchAssemblyMode onDisk(Path tempDirectory) {
        return new OnDisk(Objects.requireNonNull(tempDirectory, "tempDirectory must not be null"));
    }

    /** Keep batch parts entirely in heap, capped at {@code maxBytes} across all parts combined. */
    static BatchAssemblyMode inMemory(long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive (was " + maxBytes + ")");
        }
        return new InMemory(maxBytes);
    }

    /** Stream parts to disk. {@code tempDirectory == null} means {@code java.io.tmpdir}. */
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
