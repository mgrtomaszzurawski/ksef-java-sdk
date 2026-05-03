/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import java.util.Arrays;
import java.util.Objects;

/**
 * KSeF encryption info as it appears on the wire.
 *
 * <p>Used in session open / batch open / export init requests to convey
 * the AES key wrapped with the KSeF symmetric-key encryption public key
 * (RSA-OAEP-SHA256) plus the plaintext IV.
 *
 * @param encryptedSymmetricKey RSA-OAEP-wrapped AES key
 * @param initVector plaintext 16-byte AES-CBC IV
 */
public record KsefEncryptionInfo(byte[] encryptedSymmetricKey, byte[] initVector) {

    private static final int IV_BYTES = 16;
    private static final String ERR_NULL_KEY = "encryptedSymmetricKey must not be null";
    private static final String ERR_NULL_IV = "initVector must not be null";
    private static final String ERR_IV_SIZE = "initVector must be exactly 16 bytes";

    public KsefEncryptionInfo {
        Objects.requireNonNull(encryptedSymmetricKey, ERR_NULL_KEY);
        Objects.requireNonNull(initVector, ERR_NULL_IV);
        if (initVector.length != IV_BYTES) {
            throw new IllegalArgumentException(ERR_IV_SIZE);
        }
        encryptedSymmetricKey = encryptedSymmetricKey.clone();
        initVector = initVector.clone();
    }

    @Override
    public byte[] encryptedSymmetricKey() { return encryptedSymmetricKey.clone(); }

    @Override
    public byte[] initVector() { return initVector.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KsefEncryptionInfo other)) {
            return false;
        }
        return Arrays.equals(encryptedSymmetricKey, other.encryptedSymmetricKey)
                && Arrays.equals(initVector, other.initVector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(encryptedSymmetricKey), Arrays.hashCode(initVector));
    }
}
