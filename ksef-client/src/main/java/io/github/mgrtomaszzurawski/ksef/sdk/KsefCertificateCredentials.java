/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

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
 * @param nip 10-digit Polish tax identification number (NIP)
 */
public record KsefCertificateCredentials(
        X509Certificate certificate,
        PrivateKey privateKey,
        String nip
) implements KsefCredentials {

    private static final String ERR_NULL_CERT = "certificate must not be null";
    private static final String ERR_NULL_KEY = "privateKey must not be null";
    public KsefCertificateCredentials {
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        Objects.requireNonNull(privateKey, ERR_NULL_KEY);
        KsefCredentials.validateNip(nip);
    }

    @Override
    public String toString() {
        return "KsefCertificateCredentials[nip=" + nip + "]";
    }
}
