/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.nio.file.Path;
import java.util.Objects;

/**
 * PKCS#12 keystore-based authentication credentials.
 *
 * <p>Convenience variant of {@link KsefCertificateCredentials} that loads the certificate
 * and private key from a PKCS#12 (.p12) keystore file at authentication time.
 * The first alias in the keystore is used automatically.
 *
 * @param keystorePath path to the PKCS#12 keystore file
 * @param password keystore and private key password
 * @param nip 10-digit Polish tax identification number (NIP)
 */
public record KsefPkcs12Credentials(Path keystorePath, char[] password, String nip)
        implements KsefCredentials {

    private static final String ERR_NULL_PATH = "keystorePath must not be null";
    private static final String ERR_NULL_PASSWORD = "password must not be null";
    public KsefPkcs12Credentials {
        Objects.requireNonNull(keystorePath, ERR_NULL_PATH);
        Objects.requireNonNull(password, ERR_NULL_PASSWORD);
        KsefCredentials.validateNip(nip);
    }

    @Override
    public String toString() {
        return "KsefPkcs12Credentials[nip=" + nip + ", keystorePath=" + keystorePath + "]";
    }
}
