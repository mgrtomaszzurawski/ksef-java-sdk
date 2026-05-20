/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentialsDescriptor;

import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.AuthorizationPolicy;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;

import java.util.Objects;
import java.util.Optional;

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
 *
 * @since 1.0.0
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
     * Masked summary of this credentials instance — safe to log and
     * include in diagnostics (no secrets surface). Used by
     * {@link KsefClient#config()} to build the public
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentialsDescriptor}.
     *
     * @return masked descriptor with auth method + identifier type +
     *     {@code ***NNNN} tail of the identifier value
     */
    KsefCredentialsDescriptor asDescriptor();

    /**
     * Optional IP allow-list policy applied at authentication time. Codex
     * 2026-05-05 #7 / F6.
     *
     * <p>When empty, the SDK passes only the challenge's reported
     * {@code clientIp} as a single exact address. When present, the SDK
     * sends the full policy (exact addresses + ranges + CIDR masks) to
     * the server's {@code authorizationPolicy.allowedIps} field.
     *
     * <p>Default returns empty so existing implementations need no
     * change.
     */
    default Optional<AuthorizationPolicy> authorizationPolicy() {
        return Optional.empty();
    }

    /**
     * The NIP (Polish tax identification number) of the authenticating entity.
     *
     * <p>Convenience accessor for NIP-typed credentials. Returns
     * {@code identifier().value()} only when the underlying identifier type is
     * {@link KsefIdentifier.Type#NIP}; otherwise throws {@link IllegalStateException}.
     * For non-NIP identifier types (PESEL, internal, VATUE, peppol), use
     * {@link #identifier()} directly.
     *
     * @return 10-digit NIP string
     * @throws IllegalStateException if the identifier type is not {@code NIP}
     */
    default String nip() {
        KsefIdentifier identifier = identifier();
        if (identifier.type() != KsefIdentifier.Type.NIP) {
            throw new IllegalStateException(ERR_NOT_NIP);
        }
        return identifier.value();
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
