/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Internal — describes the structure of a batch file (encrypted ZIP) for a batch session.
 *
 * <p>Used by {@link BatchPackageBuilder} to publish the assembled package's
 * size + SHA-256 hash to the open-batch HTTP request body. NOT part of the
 * public SDK surface — consumers go through {@code Invoices.batch().submit(...)}
 * which encapsulates the whole flow.
 *
 * @param fileSize total file size in bytes (max 5 GB)
 * @param fileHash SHA-256 hash of the entire file (raw bytes, not Base64)
 * @param parts metadata for each file part (max 50 parts, each max 100 MB before encryption)
 *
 * @apiNote Internal — module-private after PR11. Was previously exposed at
 *     {@code sdk.domain.invoicing.batch.BatchFileSpec}.
 *
 * @since 1.0.0
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

    /** SHA-256 hash of the entire file. Returns a defensive copy. */
    @Override
    public byte[] fileHash() {
        return fileHash.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BatchFileSpec that)) {
            return false;
        }
        return fileSize == that.fileSize
                && Arrays.equals(fileHash, that.fileHash)
                && Objects.equals(parts, that.parts);
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

        /** SHA-256 hash of this part. Returns a defensive copy. */
        @Override
        public byte[] fileHash() {
            return fileHash.clone();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Part that)) {
                return false;
            }
            return ordinalNumber == that.ordinalNumber
                    && fileSize == that.fileSize
                    && Arrays.equals(fileHash, that.fileHash);
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
