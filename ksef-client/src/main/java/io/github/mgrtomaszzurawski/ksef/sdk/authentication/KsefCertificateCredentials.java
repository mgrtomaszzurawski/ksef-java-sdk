/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Certificate-based authentication credentials for XAdES signature flow.
 *
 * <p>The certificate and private key are used to create an XAdES-BASELINE-B signature
 * of the authentication challenge. This is the qualified electronic signature method.
 *
 * @param certificate X.509 certificate (qualified or test)
 * @param privateKey private key corresponding to the certificate
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 */
public record KsefCertificateCredentials(
        X509Certificate certificate,
        PrivateKey privateKey,
        KsefIdentifier identifier
) implements KsefCredentials {

    private static final String ERR_NULL_CERT = "certificate must not be null";
    private static final String ERR_NULL_KEY = "privateKey must not be null";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";

    /**
     * Canonical constructor — validates non-null certificate, private key and identifier.
     */
    public KsefCertificateCredentials {
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        Objects.requireNonNull(privateKey, ERR_NULL_KEY);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
    }

    /**
     * Backwards-compatible constructor — accepts a plain NIP string.
     *
     * @param certificate X.509 certificate
     * @param privateKey private key matching the certificate
     * @param nip 10-digit Polish tax identification number
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, String nip) {
        this(certificate, privateKey, KsefIdentifier.nip(nip));
    }

    @Override
    public String toString() {
        return "KsefCertificateCredentials[identifier=" + identifier + "]";
    }
}
