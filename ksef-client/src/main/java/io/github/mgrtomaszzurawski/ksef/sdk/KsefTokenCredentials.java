/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.util.Objects;

/**
 * KSeF token-based authentication credentials.
 *
 * <p>The KSeF token is a pre-generated authorization token obtained from the KSeF portal
 * or via the KSeF API. The SDK encrypts it with the KSeF public key during the
 * challenge-response authentication flow.
 *
 * @param ksefToken the KSeF authorization token
 * @param nip 10-digit Polish tax identification number (NIP)
 */
public record KsefTokenCredentials(String ksefToken, String nip) implements KsefCredentials {

    private static final String ERR_NULL_TOKEN = "ksefToken must not be null";
    private static final String ERR_BLANK_TOKEN = "ksefToken must not be blank";
    private static final String ERR_NULL_NIP = "nip must not be null";
    private static final String ERR_INVALID_NIP = "nip must be exactly 10 digits";
    private static final int NIP_LENGTH = 10;

    public KsefTokenCredentials {
        Objects.requireNonNull(ksefToken, ERR_NULL_TOKEN);
        if (ksefToken.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_TOKEN);
        }
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        validateNip(nip);
    }

    private static void validateNip(String nip) {
        if (nip.length() != NIP_LENGTH || !nip.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(ERR_INVALID_NIP);
        }
    }
}
