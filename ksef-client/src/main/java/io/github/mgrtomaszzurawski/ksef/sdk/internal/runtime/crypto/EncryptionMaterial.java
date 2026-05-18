/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import java.util.Arrays;
import java.util.Objects;

/**
 * Plaintext AES key + IV pair. Used for KSeF session-level and
 * batch-level encryption.
 *
 * <p>The KSeF protocol uses AES-256-CBC with PKCS#7 padding. A 32-byte
 * key and 16-byte IV are mandatory (REQ-SESS-03, REQ-SESS-04).
 *
 * <p>This type is {@link AutoCloseable}: calling {@link #close()}
 * zeroises both the key and IV bytes. Use try-with-resources when the
 * material is no longer needed:
 *
 * <pre>{@code
 * try (EncryptionMaterial material = cryptoService.generateAesKeyAndIv()) {
 *     byte[] ct = cryptoService.encrypt(plaintext, material);
 *     ...
 * }
 * }</pre>
 *
 * @param aesKey 32-byte AES-256 key (defensively cloned)
 * @param initVector 16-byte AES-CBC IV (defensively cloned)
 *
 * @since 1.0.0
 */
public record EncryptionMaterial(byte[] aesKey, byte[] initVector) implements AutoCloseable {

    private static final int AES_KEY_BYTES = 32;
    private static final int AES_IV_BYTES = 16;
    private static final String ERR_NULL_KEY = "aesKey must not be null";
    private static final String ERR_NULL_IV = "initVector must not be null";
    private static final String ERR_KEY_SIZE = "aesKey must be exactly 32 bytes (AES-256)";
    private static final String ERR_IV_SIZE = "initVector must be exactly 16 bytes (AES-CBC)";

    public EncryptionMaterial {
        Objects.requireNonNull(aesKey, ERR_NULL_KEY);
        Objects.requireNonNull(initVector, ERR_NULL_IV);
        if (aesKey.length != AES_KEY_BYTES) {
            throw new IllegalArgumentException(ERR_KEY_SIZE);
        }
        if (initVector.length != AES_IV_BYTES) {
            throw new IllegalArgumentException(ERR_IV_SIZE);
        }
        aesKey = aesKey.clone();
        initVector = initVector.clone();
    }

    @Override
    public byte[] aesKey() { return aesKey.clone(); }

    @Override
    public byte[] initVector() { return initVector.clone(); }

    /**
     * Zeroise both key and IV bytes. Idempotent.
     */
    @Override
    public void close() {
        Arrays.fill(aesKey, (byte) 0);
        Arrays.fill(initVector, (byte) 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EncryptionMaterial other)) {
            return false;
        }
        return Arrays.equals(aesKey, other.aesKey) && Arrays.equals(initVector, other.initVector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(aesKey), Arrays.hashCode(initVector));
    }

    @Override
    public String toString() {
        return "EncryptionMaterial[aesKey=<32 bytes>, initVector=<16 bytes>]";
    }
}
