/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.nio.file.Path;
import java.util.Arrays;
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
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 */
public record KsefPkcs12Credentials(Path keystorePath, char[] password, KsefIdentifier identifier)
        implements KsefCredentials {

    private static final String ERR_NULL_PATH = "keystorePath must not be null";
    private static final String ERR_NULL_PASSWORD = "password must not be null";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";

    /**
     * Canonical constructor — validates non-null path, password and identifier.
     */
    public KsefPkcs12Credentials {
        Objects.requireNonNull(keystorePath, ERR_NULL_PATH);
        Objects.requireNonNull(password, ERR_NULL_PASSWORD);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
    }

    /**
     * Backwards-compatible constructor — accepts a plain NIP string.
     *
     * @param keystorePath path to PKCS#12 keystore
     * @param password keystore and private key password
     * @param nip 10-digit Polish tax identification number
     */
    public KsefPkcs12Credentials(Path keystorePath, char[] password, String nip) {
        this(keystorePath, password, KsefIdentifier.nip(nip));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KsefPkcs12Credentials other)) {
            return false;
        }
        return Objects.equals(keystorePath, other.keystorePath)
                && Arrays.equals(password, other.password)
                && Objects.equals(identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(keystorePath, identifier);
        result = 31 * result + Arrays.hashCode(password);
        return result;
    }

    @Override
    public String toString() {
        return "KsefPkcs12Credentials[identifier=" + identifier
                + ", keystorePath=" + keystorePath + "]";
    }
}
