/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefCredentials;

import java.util.Objects;

/**
 * KSeF token-based authentication credentials.
 *
 * <p>The KSeF token is a pre-generated authorization token obtained from the KSeF portal
 * or via the KSeF API. The SDK encrypts it with the KSeF public key during the
 * challenge-response authentication flow.
 *
 * @param ksefToken the KSeF authorization token
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 */
public record KsefTokenCredentials(String ksefToken, KsefIdentifier identifier)
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
     * Backwards-compatible constructor — accepts a plain NIP string.
     *
     * @param ksefToken the KSeF authorization token
     * @param nip 10-digit Polish tax identification number
     */
    public KsefTokenCredentials(String ksefToken, String nip) {
        this(ksefToken, KsefIdentifier.nip(nip));
    }

    @Override
    public String toString() {
        return "KsefTokenCredentials[identifier=" + identifier + "]";
    }

}
