/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Objects;

/**
 * Result of {@link CsrSupport#generate(CsrRequest)}.
 *
 * <p>The {@link #pkcs10Der} bytes are the DER-encoded PKCS#10
 * Certificate Signing Request ready to submit to the KSeF
 * {@code POST /certificates/enrollments} endpoint.
 *
 * <p>The {@link #keyPair} echoes the input key pair so callers can use
 * the result as a single bundle when persisting (e.g. write
 * {@code keyPair.getPrivate()} to a keystore alongside the eventually
 * issued certificate).
 *
 * @param pkcs10Der DER-encoded PKCS#10 CSR (defensively cloned)
 * @param keyPair the key pair used to sign the CSR
 *
 * @since 0.1.0
 */
public record CsrResult(byte[] pkcs10Der, KeyPair keyPair) {

    private static final String ERR_NULL_DER = "pkcs10Der must not be null";
    private static final String ERR_NULL_KEYPAIR = "keyPair must not be null";

    public CsrResult {
        Objects.requireNonNull(pkcs10Der, ERR_NULL_DER);
        Objects.requireNonNull(keyPair, ERR_NULL_KEYPAIR);
        pkcs10Der = pkcs10Der.clone();
    }

    @Override
    public byte[] pkcs10Der() {
        return pkcs10Der.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CsrResult other)) {
            return false;
        }
        return Arrays.equals(pkcs10Der, other.pkcs10Der) && Objects.equals(keyPair, other.keyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(pkcs10Der), keyPair);
    }

    @Override
    public String toString() {
        return "CsrResult[pkcs10Der=" + pkcs10Der.length + " bytes, keyPair=" + keyPair + "]";
    }
}
