/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.util.Arrays;
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
    private static final String ERR_FILE_SIZE_NON_POSITIVE = "fileSize must be positive (was %d)";
    private static final String ERR_FILE_SIZE_TOO_LARGE = "fileSize exceeds spec maximum of 5 GiB (was %d)";
    private static final String ERR_FILE_HASH_LENGTH = "fileHash must be exactly 32 bytes for SHA-256 (was %d)";
    private static final String ERR_TOO_MANY_PARTS = "parts cannot exceed spec maximum of 50 (was %d)";
    private static final String ERR_ORDINAL_SEQUENCE = "part ordinals must be 1..n in order; expected %d at index %d but found %d";
    /** OpenAPI {@code BatchFile.fileSize} maximum (5 × 1024^3 bytes — 5 GiB). */
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L * 1024L;
    /** Lower bound on file size — files must contain at least one byte. */
    private static final long MIN_FILE_SIZE_BYTES = 1L;
    /** OpenAPI {@code BatchFile.fileParts} maximum array length. */
    private static final int MAX_PARTS = 50;
    /** SHA-256 produces a 32-byte digest. */
    private static final int SHA_256_HASH_BYTES = 32;

    /**
     * Creates a batch file specification.
     *
     * <p>Validation (Codex round-9 fresh review M1) — strict spec-aligned
     * checks rather than the previous null-only guards:
     * <ul>
     *   <li>{@code fileSize} must be in {@code (0, 5 GiB]} per OpenAPI
     *       {@code BatchFile.fileSize} bounds;</li>
     *   <li>{@code fileHash} must be exactly 32 bytes (SHA-256);</li>
     *   <li>{@code parts} must be non-empty and at most {@value #MAX_PARTS}
     *       entries per OpenAPI {@code BatchFile.fileParts} cap;</li>
     *   <li>part ordinals must form the sequence {@code 1..n}.</li>
     * </ul>
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
        if (fileSize < MIN_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_FILE_SIZE_NON_POSITIVE, fileSize));
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_FILE_SIZE_TOO_LARGE, fileSize));
        }
        if (fileHash.length != SHA_256_HASH_BYTES) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_FILE_HASH_LENGTH, fileHash.length));
        }
        if (parts.size() > MAX_PARTS) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_TOO_MANY_PARTS, parts.size()));
        }
        for (int index = 0; index < parts.size(); index++) {
            int expectedOrdinal = index + 1;
            int actualOrdinal = parts.get(index).ordinalNumber();
            if (actualOrdinal != expectedOrdinal) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        ERR_ORDINAL_SEQUENCE, expectedOrdinal, index, actualOrdinal));
            }
        }
        fileHash = fileHash.clone();
        parts = List.copyOf(parts);
    }

    /**
     * SHA-256 hash of the entire file. Returns a defensive copy.
     */
    @Override
    public byte[] fileHash() {
        return fileHash.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchFileSpec other)) {
            return false;
        }
        return fileSize == other.fileSize
                && Arrays.equals(fileHash, other.fileHash)
                && Objects.equals(parts, other.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileSize, parts, Arrays.hashCode(fileHash));
    }

    @Override
    public String toString() {
        return "BatchFileSpec[fileSize=" + fileSize
                + ", fileHash=byte[" + (fileHash == null ? 0 : fileHash.length) + "]"
                + ", parts=" + parts + "]";
    }

    /**
     * Metadata for a single part of the batch file.
     *
     * @param ordinalNumber part sequence number (1-based)
     * @param fileSize part size in bytes (max 100 MB before encryption)
     * @param fileHash SHA-256 hash of the part (raw bytes, not Base64)
     */
    public record Part(int ordinalNumber, long fileSize, byte[] fileHash) {

        /** SHA-256 produces a 32-byte digest. */
        private static final int SHA_256_PART_HASH_BYTES = 32;
        /** Lower bound on per-part ordinal — KSeF parts are 1-indexed. */
        private static final int MIN_ORDINAL_NUMBER = 1;
        /** Lower bound on per-part size — every uploaded part must contain at least one byte. */
        private static final long MIN_PART_SIZE_BYTES = 1L;
        private static final String ERR_PART_HASH_NULL = "part fileHash must not be null";
        private static final String ERR_PART_ORDINAL_NON_POSITIVE = "part ordinalNumber must be >= 1 (was %d)";
        private static final String ERR_PART_FILE_SIZE_NON_POSITIVE = "part fileSize must be positive (was %d)";
        private static final String ERR_PART_HASH_LENGTH = "part fileHash must be exactly 32 bytes for SHA-256 (was %d)";

        /**
         * Creates a batch file part specification.
         *
         * <p>Validation (Codex round-9 fresh review M1):
         * <ul>
         *   <li>{@code ordinalNumber} must be {@code >= 1};</li>
         *   <li>{@code fileSize} must be {@code > 0};</li>
         *   <li>{@code fileHash} must be exactly 32 bytes (SHA-256).</li>
         * </ul>
         *
         * @param ordinalNumber part sequence number (1-based)
         * @param fileSize part size in bytes
         * @param fileHash SHA-256 hash of the part
         */
        public Part {
            Objects.requireNonNull(fileHash, ERR_PART_HASH_NULL);
            if (ordinalNumber < MIN_ORDINAL_NUMBER) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        ERR_PART_ORDINAL_NON_POSITIVE, ordinalNumber));
            }
            if (fileSize < MIN_PART_SIZE_BYTES) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        ERR_PART_FILE_SIZE_NON_POSITIVE, fileSize));
            }
            if (fileHash.length != SHA_256_PART_HASH_BYTES) {
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        ERR_PART_HASH_LENGTH, fileHash.length));
            }
            fileHash = fileHash.clone();
        }

        /**
         * SHA-256 hash of this part. Returns a defensive copy.
         */
        @Override
        public byte[] fileHash() {
            return fileHash.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Part other)) {
                return false;
            }
            return ordinalNumber == other.ordinalNumber
                    && fileSize == other.fileSize
                    && Arrays.equals(fileHash, other.fileHash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ordinalNumber, fileSize, Arrays.hashCode(fileHash));
        }

        @Override
        public String toString() {
            return "Part[ordinalNumber=" + ordinalNumber
                    + ", fileSize=" + fileSize
                    + ", fileHash=byte[" + (fileHash == null ? 0 : fileHash.length) + "]]";
        }
    }
}
