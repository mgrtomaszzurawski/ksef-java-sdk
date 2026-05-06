/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * One encrypted part of a batch upload. Two implementations matching
 * the two {@link BatchAssemblyMode}s:
 *
 * <ul>
 *   <li>{@link OnDiskPart} — ciphertext lives in a temp file; {@link #cleanup()}
 *       deletes the file.</li>
 *   <li>{@link InMemoryPart} — ciphertext lives in a {@code byte[]} on the heap;
 *       {@link #cleanup()} is a no-op (GC).</li>
 * </ul>
 *
 * <p>Internal to the SDK transport layer — consumers do not see this
 * type. {@code KsefBatchSession.uploadParts} pattern-matches on the
 * subtype to pick the right {@code java.net.http.HttpRequest.BodyPublisher}.
 *
 * @since 1.0.0
 */
public sealed interface BatchPart {

    /** 1-based ordinal within the batch. */
    int ordinalNumber();

    /** Size of the encrypted bytes for this part. */
    long sizeBytes();

    /** SHA-256 of the encrypted bytes. */
    byte[] hash();

    /**
     * Release any resources held by this part. Idempotent and quiet —
     * a failed delete is logged but not propagated. Called automatically
     * by {@code KsefBatchSession.close()}; safe to call manually too.
     */
    void cleanup();

    /** Encrypted bytes streamed from disk. */
    record OnDiskPart(int ordinalNumber, long sizeBytes, byte[] hash, Path path) implements BatchPart {

        public OnDiskPart {
            Objects.requireNonNull(hash, "hash must not be null");
            Objects.requireNonNull(path, "path must not be null");
            hash = hash.clone();
        }

        @Override
        public byte[] hash() {
            return hash.clone();
        }

        @Override
        public void cleanup() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // best effort — JVM exit hook is the safety net
            }
        }
    }

    /** Encrypted bytes held in heap. */
    record InMemoryPart(int ordinalNumber, byte[] hash, byte[] bytes) implements BatchPart {

        public InMemoryPart {
            Objects.requireNonNull(hash, "hash must not be null");
            Objects.requireNonNull(bytes, "bytes must not be null");
            hash = hash.clone();
            bytes = bytes.clone();
        }

        @Override
        public long sizeBytes() {
            return bytes.length;
        }

        @Override
        public byte[] hash() {
            return hash.clone();
        }

        public byte[] bytes() {
            return bytes.clone();
        }

        @Override
        public void cleanup() {
            // GC — nothing to release.
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InMemoryPart other)) {
                return false;
            }
            return ordinalNumber == other.ordinalNumber
                    && Arrays.equals(hash, other.hash)
                    && Arrays.equals(bytes, other.bytes);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(ordinalNumber);
            result = 31 * result + Arrays.hashCode(hash);
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }
    }
}
