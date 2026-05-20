/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.util.Objects;

/**
 * KSeF certificate serial number — typed wrapper around the hex string the
 * server returns from {@code certificates/enrollments/{ref}} and accepts
 * on revoke / retrieve. Validates non-emptiness, hexadecimal-only content,
 * and length bounds at construction so the SDK fails fast before a
 * malformed identifier reaches the wire.
 *
 * @since 0.1.0
 */
public record CertificateSerialNumber(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 64;
    private static final String ERR_NULL_VALUE = "CertificateSerialNumber value must not be null";
    private static final String ERR_EMPTY_VALUE = "CertificateSerialNumber value must not be blank";
    private static final String ERR_TOO_LONG = "CertificateSerialNumber value exceeds %d characters: ";
    private static final String ERR_NON_HEX =
            "CertificateSerialNumber value must contain only hexadecimal characters (0-9 a-f A-F): ";

    public CertificateSerialNumber {
        Objects.requireNonNull(value, ERR_NULL_VALUE);
        if (value.length() < MIN_LENGTH || value.isBlank()) {
            throw new IllegalArgumentException(ERR_EMPTY_VALUE);
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(String.format(ERR_TOO_LONG, MAX_LENGTH) + value);
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isHexCharacter(character)) {
                throw new IllegalArgumentException(ERR_NON_HEX + value);
            }
        }
    }

    private static boolean isHexCharacter(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    /** Convenience factory matching the parse-style {@code KsefNumber.parse} family. */
    public static CertificateSerialNumber parse(String value) {
        return new CertificateSerialNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
