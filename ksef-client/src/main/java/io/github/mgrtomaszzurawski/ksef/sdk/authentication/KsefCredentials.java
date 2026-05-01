/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
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
    String ERR_NOT_NIP = "identifier is not a NIP — use identifier() instead";

    /**
     * The authentication context identifier (NIP / internal id / EU VAT / Peppol).
     *
     * @return identifier (type + value)
     */
    KsefIdentifier identifier();

    /**
     * The NIP (Polish tax identification number) of the authenticating entity.
     *
     * <p>Backwards-compatible accessor. Returns {@code identifier().value()} only when
     * the underlying identifier type is {@link KsefIdentifier.Type#NIP}; otherwise
     * throws {@link IllegalStateException}.
     *
     * @return 10-digit NIP string
     * @throws IllegalStateException if the identifier type is not {@code NIP}
     * @deprecated use {@link #identifier()} — supports all four KSeF identifier types
     */
    @Deprecated(since = "0.2.0", forRemoval = false)
    default String nip() {
        KsefIdentifier id = identifier();
        if (id.type() != KsefIdentifier.Type.NIP) {
            throw new IllegalStateException(ERR_NOT_NIP);
        }
        return id.value();
    }

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
