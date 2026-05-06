/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchAssemblyMode;
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
 * <p>Internal to the SDK transport layer — not part of the public,
 * Maven-Central-exported API surface (lives under
 * {@code sdk.internal.runtime.batch}, which is not exported by
 * {@code module-info.java}). {@code KsefBatchSession.uploadParts}
 * pattern-matches on the subtype to pick the right
 * {@code java.net.http.HttpRequest.BodyPublisher}.
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

    /**
     * Encrypted bytes held in heap.
     *
     * <p><strong>Memory contract:</strong> the {@code bytes} component is
     * stored by reference (no defensive clone). This record is internal
     * (lives in non-exported {@code sdk.internal.runtime.batch}) and is
     * only constructed by {@code BatchPackageBuilder.ChunkSink} from a
     * just-encrypted buffer that has no other live references. The
     * accessor likewise returns the internal buffer; the upload path
     * passes it to {@code BodyPublishers.ofByteArray} which performs its
     * own internal copy. Skipping the defensive clones keeps peak heap
     * at 1× per part instead of 3× (per Codex round-9 fresh review).
     * The {@code hash} component is small (32 bytes) and stays defensive
     * since it is a security-sensitive integrity check.
     */
    record InMemoryPart(int ordinalNumber, byte[] hash, byte[] bytes) implements BatchPart {

        public InMemoryPart {
            Objects.requireNonNull(hash, "hash must not be null");
            Objects.requireNonNull(bytes, "bytes must not be null");
            hash = hash.clone();
        }

        @Override
        public long sizeBytes() {
            return bytes.length;
        }

        @Override
        public byte[] hash() {
            return hash.clone();
        }

        @Override
        public void cleanup() {
            // GC — nothing to release.
        }

        /**
         * Equality by SHA-256 hash + ordinal — does NOT walk the bytes
         * payload (which can be up to 100 MB per part / 5 GiB across
         * a full batch). Two parts with matching ordinal and matching
         * SHA-256 are cryptographically equivalent (collision-resistant
         * hash); the explicit byte-level comparison the JDK record
         * default would emit is unnecessary and a memory-pressure
         * footgun for large batches.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InMemoryPart other)) {
                return false;
            }
            return ordinalNumber == other.ordinalNumber
                    && Arrays.equals(hash, other.hash);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(ordinalNumber);
            result = 31 * result + Arrays.hashCode(hash);
            return result;
        }
    }
}
