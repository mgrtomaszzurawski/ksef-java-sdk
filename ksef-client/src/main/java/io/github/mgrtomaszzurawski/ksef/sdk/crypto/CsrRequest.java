/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import java.security.KeyPair;
import java.util.Objects;

/**
 * Request for {@link KsefCryptoService#generateCsr(CsrRequest)}.
 *
 * <p>Builds a PKCS#10 Certificate Signing Request signed with
 * {@code keyPair}'s private key for KSeF certificate enrollment.
 *
 * @param subjectDn X.500 subject DN (e.g.
 *     {@code "CN=ACME Sp. z o.o., O=ACME, C=PL, 2.5.4.97=VATPL-1234567890"}).
 *     KSeF requires specific attributes per
 *     {@code ksef-docs/auth/podpis-xades.md} — at minimum givenName +
 *     surname + serialNumber for individuals, organizationName +
 *     organizationIdentifier (OID 2.5.4.97) for organizations.
 * @param keyPair RSA or EC key pair; signature algorithm is auto-selected
 *     by key type ({@code SHA256withRSA} for RSA, {@code SHA256withECDSA}
 *     for EC)
 */
public record CsrRequest(String subjectDn, KeyPair keyPair) {

    private static final String ERR_NULL_SUBJECT = "subjectDn must not be null";
    private static final String ERR_BLANK_SUBJECT = "subjectDn must not be blank";
    private static final String ERR_NULL_KEYPAIR = "keyPair must not be null";

    public CsrRequest {
        Objects.requireNonNull(subjectDn, ERR_NULL_SUBJECT);
        Objects.requireNonNull(keyPair, ERR_NULL_KEYPAIR);
        if (subjectDn.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_SUBJECT);
        }
    }
}
