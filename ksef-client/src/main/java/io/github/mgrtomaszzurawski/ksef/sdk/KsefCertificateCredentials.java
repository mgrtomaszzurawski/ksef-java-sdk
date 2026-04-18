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
    private static final String ERR_NULL_NIP = "nip must not be null";
    private static final String ERR_INVALID_NIP = "nip must be exactly 10 digits";
    private static final int NIP_LENGTH = 10;

    public KsefCertificateCredentials {
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        Objects.requireNonNull(privateKey, ERR_NULL_KEY);
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        validateNip(nip);
    }

    private static void validateNip(String nip) {
        if (nip.length() != NIP_LENGTH || !nip.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(ERR_INVALID_NIP);
        }
    }
}
