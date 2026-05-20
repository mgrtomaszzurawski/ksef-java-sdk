/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.AuthorizationPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.AuthMethod;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentialsDescriptor;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * KSeF token-based authentication credentials.
 *
 * <p>The KSeF token is a pre-generated authorization token obtained from the KSeF portal
 * or via the KSeF API. The SDK encrypts it with the KSeF public key during the
 * challenge-response authentication flow.
 *
 * @param ksefToken the KSeF authorization token
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 * @param authPolicy optional IP allow-list for the authentication ticket
 *     (Codex 2026-05-05 #7 / F6); when {@code null}, the SDK falls back
 *     to the challenge's reported {@code clientIp} as a single exact
 *     address
 *
 * @since 1.0.0
 */
public record KsefTokenCredentials(String ksefToken, KsefIdentifier identifier,
                                   @Nullable AuthorizationPolicy authPolicy)
        implements KsefCredentials {

    private static final String ERR_NULL_TOKEN = "ksefToken must not be null";
    private static final String ERR_BLANK_TOKEN = "ksefToken must not be blank";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";

    /**
     * Canonical constructor — validates the token and identifier.
     */
    public KsefTokenCredentials {
        Objects.requireNonNull(ksefToken, ERR_NULL_TOKEN);
        if (ksefToken.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_TOKEN);
        }
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
    }

    /**
     * Convenience constructor — token + identifier with no custom
     * authorization policy.
     */
    public KsefTokenCredentials(String ksefToken, KsefIdentifier identifier) {
        this(ksefToken, identifier, null);
    }

    /**
     * Convenience constructor — accepts a plain NIP string.
     *
     * @param ksefToken the KSeF authorization token
     * @param nip 10-digit Polish tax identification number
     */
    public KsefTokenCredentials(String ksefToken, String nip) {
        this(ksefToken, KsefIdentifier.nip(nip), null);
    }

    @Override
    public Optional<AuthorizationPolicy> authorizationPolicy() {
        return Optional.ofNullable(authPolicy);
    }

    @Override
    public String toString() {
        return "KsefTokenCredentials[identifier=" + identifier
                + (authPolicy == null ? "" : ", authPolicy=set") + "]";
    }

    @Override
    public KsefCredentialsDescriptor asDescriptor() {
        return KsefCredentialsDescriptor.of(AuthMethod.TOKEN, identifier);
    }

}
