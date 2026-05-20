/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import java.util.Objects;
import java.util.regex.Pattern;

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
 *
 * @since 0.1.0
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
    private static final int[] NIP_CHECKSUM_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};
    private static final int NIP_CHECKSUM_MODULUS = 11;
    private static final int NIP_CHECKSUM_INVALID = 10;
    private static final String ERR_NULL_TYPE = "type must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";
    private static final String ERR_BLANK_VALUE = "value must not be blank";
    private static final String ERR_INVALID_NIP = "NIP must be exactly 10 digits with valid checksum";
    private static final String ERR_INVALID_INTERNAL_ID =
            "internalId must match <10-digit NIP>-<5 digits>";
    private static final String ERR_INVALID_NIP_VAT_UE =
            "nipVatUe must contain only [A-Z0-9-] and be at least 4 characters long";
    private static final String ERR_INVALID_PEPPOL_ID =
            "peppolId must contain only [A-Z0-9] and be 4-30 characters long";
    private static final String ERR_UNSUPPORTED_TYPE = "Unsupported identifier type: ";
    private static final int RADIX_DECIMAL = 10;

    private static final Pattern INTERNAL_ID_PATTERN = Pattern.compile("^\\d{10}-\\d{5}$");
    private static final Pattern NIP_VAT_UE_PATTERN = Pattern.compile("^[A-Z0-9-]{4,40}$");
    private static final Pattern PEPPOL_ID_PATTERN = Pattern.compile("^[A-Z0-9]{4,30}$");

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
        switch (type) {
            case NIP -> {
                if (!isValidNip(value)) {
                    throw new IllegalArgumentException(ERR_INVALID_NIP);
                }
            }
            case INTERNAL_ID -> {
                if (!INTERNAL_ID_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException(ERR_INVALID_INTERNAL_ID);
                }
            }
            case NIP_VAT_UE -> {
                if (!NIP_VAT_UE_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException(ERR_INVALID_NIP_VAT_UE);
                }
            }
            case PEPPOL_ID -> {
                if (!PEPPOL_ID_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException(ERR_INVALID_PEPPOL_ID);
                }
            }
            default -> throw new IllegalArgumentException(ERR_UNSUPPORTED_TYPE + type);
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
        if (nip.length() != NIP_LENGTH || !nip.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < NIP_CHECKSUM_WEIGHTS.length; i++) {
            sum += Character.digit(nip.charAt(i), RADIX_DECIMAL) * NIP_CHECKSUM_WEIGHTS[i];
        }
        int checksum = sum % NIP_CHECKSUM_MODULUS;
        return checksum != NIP_CHECKSUM_INVALID
                && checksum == Character.digit(nip.charAt(NIP_LENGTH - 1), RADIX_DECIMAL);
    }
}
