/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * SDK request payload returned by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.OnlineSessionBuilder#build()}.
 * <p>Includes the AES key and IV in plaintext so the caller can encrypt
 * invoices within the session; {@code encryptedSymmetricKey} is the same key
 * RSA-encrypted with KSeF's public key (sent on the open-session request).
 */
public record OnlineSessionOpenRequest(
        FormCodeInfo formCode,
        byte[] encryptedSymmetricKey,
        byte[] initVector,
        byte[] aesKey) {

    public OnlineSessionOpenRequest {
        Objects.requireNonNull(formCode, "formCode");
        Objects.requireNonNull(encryptedSymmetricKey, "encryptedSymmetricKey");
        Objects.requireNonNull(initVector, "initVector");
        Objects.requireNonNull(aesKey, "aesKey");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OnlineSessionOpenRequest other)) {
            return false;
        }
        return Objects.equals(formCode, other.formCode)
                && Arrays.equals(encryptedSymmetricKey, other.encryptedSymmetricKey)
                && Arrays.equals(initVector, other.initVector)
                && Arrays.equals(aesKey, other.aesKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formCode,
                Arrays.hashCode(encryptedSymmetricKey),
                Arrays.hashCode(initVector),
                Arrays.hashCode(aesKey));
    }

    @Override
    public String toString() {
        return "OnlineSessionOpenRequest[formCode=" + formCode
                + ", encryptedSymmetricKey=byte[" + encryptedSymmetricKey.length + "]"
                + ", initVector=byte[" + initVector.length + "]"
                + ", aesKey=<redacted>]";
    }
}
