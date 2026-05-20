/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import java.util.Arrays;
import java.util.Objects;

/**
 * File size + SHA-256 hash. Used by KSeF to verify invoice content,
 * batch package parts, and export package parts.
 *
 * @param size byte size of the content
 * @param sha256 32-byte SHA-256 hash
 *
 * @since 0.1.0
 */
public record FileMetadata(long size, byte[] sha256) {

    private static final int SHA256_BYTES = 32;
    private static final String ERR_NULL_HASH = "sha256 must not be null";
    private static final String ERR_HASH_SIZE = "sha256 must be exactly 32 bytes";
    private static final String ERR_NEGATIVE_SIZE = "size must not be negative";

    public FileMetadata {
        Objects.requireNonNull(sha256, ERR_NULL_HASH);
        if (sha256.length != SHA256_BYTES) {
            throw new IllegalArgumentException(ERR_HASH_SIZE);
        }
        if (size < 0) {
            throw new IllegalArgumentException(ERR_NEGATIVE_SIZE);
        }
        sha256 = sha256.clone();
    }

    @Override
    public byte[] sha256() { return sha256.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileMetadata other)) {
            return false;
        }
        return size == other.size && Arrays.equals(sha256, other.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, Arrays.hashCode(sha256));
    }

    @Override
    public String toString() {
        return "FileMetadata[size=" + size + ", sha256=" + java.util.HexFormat.of().formatHex(sha256) + "]";
    }
}
