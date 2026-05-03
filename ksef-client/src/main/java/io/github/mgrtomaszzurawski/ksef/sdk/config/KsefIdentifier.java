/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.util.Objects;

/**
 * Authentication context identifier — generalisation over the four KSeF identifier types.
 *
 * <p>KSeF allows authenticating in the context of a Polish NIP, an internal entity id,
 * an EU VAT identifier, or a Peppol participant id. The mapping to the wire-level
 * {@code AuthenticationContextIdentifierType} is performed by the SDK.
 *
 * <p>Use the static factory methods to build a validated identifier.
 *
 * @param type identifier type (never {@code null})
 * @param value identifier value (never {@code null} or blank)
 */
public record KsefIdentifier(Type type, String value) {

    /** Identifier types accepted by the KSeF authentication context. */
    public enum Type {
        /** Polish tax identification number (NIP). */
        NIP,
        /** Internal entity identifier. */
        INTERNAL_ID,
        /** EU VAT identifier (NIP-VAT-UE). */
        NIP_VAT_UE,
        /** Peppol participant identifier. */
        PEPPOL_ID
    }

    private static final int NIP_LENGTH = 10;
    private static final String ERR_NULL_TYPE = "type must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";
    private static final String ERR_BLANK_VALUE = "value must not be blank";
    private static final String ERR_INVALID_NIP = "NIP must be exactly 10 digits";

    /**
     * Compact canonical constructor — validates non-null type, non-blank value, and
     * NIP-specific format when {@code type == NIP}.
     *
     * @param type identifier type
     * @param value identifier value
     */
    public KsefIdentifier {
        Objects.requireNonNull(type, ERR_NULL_TYPE);
        Objects.requireNonNull(value, ERR_NULL_VALUE);
        if (value.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_VALUE);
        }
        if (type == Type.NIP && !isValidNip(value)) {
            throw new IllegalArgumentException(ERR_INVALID_NIP);
        }
    }

    /**
     * Build a Polish NIP identifier — value must be exactly 10 digits.
     *
     * @param nip 10-digit Polish tax identification number
     * @return identifier with {@link Type#NIP}
     */
    public static KsefIdentifier nip(String nip) {
        return new KsefIdentifier(Type.NIP, nip);
    }

    /**
     * Build an internal-id identifier (no format validation beyond non-blank).
     *
     * @param id internal identifier value
     * @return identifier with {@link Type#INTERNAL_ID}
     */
    public static KsefIdentifier internalId(String id) {
        return new KsefIdentifier(Type.INTERNAL_ID, id);
    }

    /**
     * Build an EU VAT identifier (no format validation beyond non-blank).
     *
     * @param id EU VAT identifier value
     * @return identifier with {@link Type#NIP_VAT_UE}
     */
    public static KsefIdentifier nipVatUe(String id) {
        return new KsefIdentifier(Type.NIP_VAT_UE, id);
    }

    /**
     * Build a Peppol identifier (no format validation beyond non-blank).
     *
     * @param id Peppol participant identifier value
     * @return identifier with {@link Type#PEPPOL_ID}
     */
    public static KsefIdentifier peppolId(String id) {
        return new KsefIdentifier(Type.PEPPOL_ID, id);
    }

    private static boolean isValidNip(String nip) {
        return nip.length() == NIP_LENGTH && nip.chars().allMatch(Character::isDigit);
    }
}
