/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.util.List;
import java.util.Objects;

/**
 * Describes the structure of a batch file (encrypted ZIP) for a batch session.
 *
 * <p>The consumer must split the encrypted ZIP into parts and provide metadata
 * (size, SHA-256 hash) for the whole file and each part. KSeF uses this to
 * validate uploaded parts and reassemble the file.
 *
 * <p>Example:
 * <pre>{@code
 * BatchFileSpec spec = new BatchFileSpec(totalSize, totalHash, List.of(
 *     new BatchFileSpec.Part(1, part1Size, part1Hash),
 *     new BatchFileSpec.Part(2, part2Size, part2Hash)
 * ));
 * }</pre>
 *
 * @param fileSize total file size in bytes (max 5 GB)
 * @param fileHash SHA-256 hash of the entire file (raw bytes, not Base64)
 * @param parts metadata for each file part (max 50 parts, each max 100 MB before encryption)
 */
public record BatchFileSpec(long fileSize, byte[] fileHash, List<Part> parts) {

    private static final String ERR_FILE_HASH_NULL = "fileHash must not be null";
    private static final String ERR_PARTS_NULL = "parts must not be null";
    private static final String ERR_PARTS_EMPTY = "parts must not be empty";

    /**
     * Creates a batch file specification.
     *
     * @param fileSize total file size in bytes
     * @param fileHash SHA-256 hash of the entire file
     * @param parts part metadata list (must not be empty)
     */
    public BatchFileSpec {
        Objects.requireNonNull(fileHash, ERR_FILE_HASH_NULL);
        Objects.requireNonNull(parts, ERR_PARTS_NULL);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException(ERR_PARTS_EMPTY);
        }
        parts = List.copyOf(parts);
    }

    /**
     * Metadata for a single part of the batch file.
     *
     * @param ordinalNumber part sequence number (1-based)
     * @param fileSize part size in bytes (max 100 MB before encryption)
     * @param fileHash SHA-256 hash of the part (raw bytes, not Base64)
     */
    public record Part(int ordinalNumber, long fileSize, byte[] fileHash) {

        private static final String ERR_PART_HASH_NULL = "part fileHash must not be null";

        /**
         * Creates a batch file part specification.
         *
         * @param ordinalNumber part sequence number (1-based)
         * @param fileSize part size in bytes
         * @param fileHash SHA-256 hash of the part
         */
        public Part {
            Objects.requireNonNull(fileHash, ERR_PART_HASH_NULL);
        }
    }
}
