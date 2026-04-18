/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.util.Objects;

/**
 * Authentication credentials for KSeF API access.
 *
 * <p>Sealed interface with three implementations for different authentication methods:
 * <ul>
 *   <li>{@link KsefTokenCredentials} — KSeF token-based authentication</li>
 *   <li>{@link KsefCertificateCredentials} — certificate-based authentication (XAdES signature)</li>
 *   <li>{@link KsefPkcs12Credentials} — PKCS#12 keystore-based authentication</li>
 * </ul>
 *
 * @see KsefClient.Builder#credentials(KsefCredentials)
 */
public sealed interface KsefCredentials
        permits KsefTokenCredentials, KsefCertificateCredentials, KsefPkcs12Credentials {

    int NIP_LENGTH = 10;
    String ERR_NULL_NIP = "nip must not be null";
    String ERR_INVALID_NIP = "nip must be exactly 10 digits";

    /**
     * The NIP (Polish tax identification number) of the authenticating entity.
     *
     * @return 10-digit NIP string
     */
    String nip();

    /**
     * Validate that the given NIP is non-null and exactly 10 digits.
     *
     * @param nip the NIP to validate
     * @throws NullPointerException if nip is null
     * @throws IllegalArgumentException if nip is not exactly 10 digits
     */
    static void validateNip(String nip) {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        if (nip.length() != NIP_LENGTH || !nip.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(ERR_INVALID_NIP);
        }
    }
}
