/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.core;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable value object representing the <strong>KSeF-assigned</strong>
 * invoice identifier — the value the KSeF system returns after invoice
 * acceptance. <em>Distinct from</em> the issuer-assigned {@code invoiceNumber}
 * the seller puts inside the FA(2)/FA(3) document (e.g. {@code "FA/2025/07/01"}):
 * both fields ship together in {@code InvoiceMetadata} and elsewhere,
 * but they identify different things.
 *
 * <p>Format defined by {@code faktury/numer-ksef.md}:
 * {@code NIP(10)-YYYYMMDD-TECHNICAL(12 hex)-CRC8(2 hex)}, total 35 characters.
 * The NIP segment is the <strong>seller's</strong> NIP, the date is the
 * KSeF-side acceptance date.
 * Example: {@code 5265877635-20250826-0100001AF629-AF}.
 *
 * <p>Use {@link #parse(String)} or the canonical constructor to obtain
 * instances. Both validate length, separator placement, segment alphabets,
 * date validity, and the CRC-8 checksum (polynomial {@code 0x07}, initial
 * value {@code 0x00}). Construction with an invalid number throws
 * {@link IllegalArgumentException}; once an instance exists, all derived
 * accessors are guaranteed correct.
 *
 * <p><b>Lowercase rejection.</b> The KSeF spec uses uppercase hex
 * throughout for the technical and checksum segments; the official
 * {@code KSeFNumberValidator} reference implementation accepts only
 * uppercase. This SDK matches that and does not silently normalize
 * lowercase input — passing a lowercase technical or checksum segment
 * throws {@link IllegalArgumentException}. Callers who receive lowercase
 * input from upstream systems should normalize via
 * {@link String#toUpperCase(java.util.Locale)} with {@link java.util.Locale#ROOT}
 * before parsing.
 *
 * <p>Spec reference: REQ-SESS-18, REQ-SESS-19, REQ-SESS-20.
 *
 * @since 0.1.0
 */
public record KsefNumber(String value) {

    private static final int TOTAL_LENGTH = 35;
    private static final int CRC_DATA_LENGTH = 32;
    private static final int NIP_LENGTH = 10;
    private static final int DATE_LENGTH = 8;
    private static final int TECHNICAL_LENGTH = 12;
    private static final int NIP_END = NIP_LENGTH;
    private static final int DATE_START = NIP_LENGTH + 1;
    private static final int DATE_END = DATE_START + DATE_LENGTH;
    private static final int TECHNICAL_START = DATE_END + 1;
    private static final int TECHNICAL_END = TECHNICAL_START + TECHNICAL_LENGTH;
    private static final int CHECKSUM_START = TECHNICAL_END + 1;
    private static final char SEPARATOR = '-';
    private static final int CRC_POLYNOMIAL = 0x07;
    private static final int CRC_INIT = 0x00;
    private static final int CRC_HIGH_BIT = 0x80;
    private static final int CRC_BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;
    private static final String CRC_FORMAT = "%02X";
    /**
     * Year pattern uses {@code uuuu} (proleptic ISO year) instead of
     * {@code yyyy} (year-of-era) so {@link ResolverStyle#STRICT} can reject
     * malformed dates like {@code 20250230} unambiguously. The {@code yyyy}
     * + smart resolver combination silently shifts overflowed days into the
     * next month; {@code uuuu} + STRICT throws.
     */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private static final String ERR_NULL = "ksef number must not be null";
    private static final String ERR_LENGTH = "ksef number must be %d characters, got %d";
    private static final String ERR_SEPARATOR = "ksef number must have separators '-' at positions %d, %d, %d";
    private static final String ERR_NIP_FORMAT = "ksef number NIP segment must be 10 decimal digits";
    private static final String ERR_DATE_FORMAT = "ksef number date segment must be a valid YYYYMMDD date";
    private static final String ERR_TECHNICAL_FORMAT = "ksef number technical segment must be 12 uppercase hex characters";
    private static final String ERR_CHECKSUM_FORMAT = "ksef number checksum segment must be 2 uppercase hex characters";
    private static final String ERR_CHECKSUM_MISMATCH = "ksef number CRC-8 checksum mismatch: expected %s, computed %s";

    /**
     * Canonical constructor. Validates the supplied string against the KSeF
     * number specification. Equivalent to {@link #parse(String)}.
     *
     * @throws IllegalArgumentException when the string fails any validation rule
     * @throws NullPointerException     when the string is {@code null}
     */
    public KsefNumber {
        Objects.requireNonNull(value, ERR_NULL);
        validateLength(value);
        validateSeparators(value);
        validateNip(value);
        parseDate(value);
        validateTechnical(value);
        validateChecksumFormat(value);
        validateChecksumValue(value);
    }

    /**
     * Parses and validates a KSeF number string.
     *
     * @return an immutable {@link KsefNumber} instance
     * @throws IllegalArgumentException when the string fails any validation rule
     * @throws NullPointerException     when the string is {@code null}
     */
    public static KsefNumber parse(String raw) {
        return new KsefNumber(raw);
    }

    /** The 10-digit seller NIP segment. */
    public String sellerNip() {
        return value.substring(0, NIP_END);
    }

    /** The acceptance date encoded in the YYYYMMDD segment. */
    public LocalDate acceptanceDate() {
        return LocalDate.parse(value.substring(DATE_START, DATE_END), DATE_FORMAT);
    }

    /** The 12-character uppercase hex technical segment. */
    public String technicalPart() {
        return value.substring(TECHNICAL_START, TECHNICAL_END);
    }

    /** The 2-character uppercase hex CRC-8 checksum segment. */
    public String checksum() {
        return value.substring(CHECKSUM_START);
    }

    @Override
    public String toString() {
        return value;
    }

    private static void validateLength(String value) {
        if (value.length() != TOTAL_LENGTH) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, ERR_LENGTH, TOTAL_LENGTH, value.length()));
        }
    }

    private static void validateSeparators(String value) {
        if (value.charAt(NIP_END) != SEPARATOR
                || value.charAt(DATE_END) != SEPARATOR
                || value.charAt(TECHNICAL_END) != SEPARATOR) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, ERR_SEPARATOR, NIP_END, DATE_END, TECHNICAL_END));
        }
    }

    private static void validateNip(String value) {
        for (int i = 0; i < NIP_LENGTH; i++) {
            if (!Character.isDigit(value.charAt(i))) {
                throw new IllegalArgumentException(ERR_NIP_FORMAT);
            }
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.substring(DATE_START, DATE_END), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(ERR_DATE_FORMAT, ex);
        }
    }

    private static void validateTechnical(String value) {
        for (int i = TECHNICAL_START; i < TECHNICAL_END; i++) {
            if (!isUppercaseHex(value.charAt(i))) {
                throw new IllegalArgumentException(ERR_TECHNICAL_FORMAT);
            }
        }
    }

    private static void validateChecksumFormat(String value) {
        for (int i = CHECKSUM_START; i < TOTAL_LENGTH; i++) {
            if (!isUppercaseHex(value.charAt(i))) {
                throw new IllegalArgumentException(ERR_CHECKSUM_FORMAT);
            }
        }
    }

    private static void validateChecksumValue(String value) {
        String declared = value.substring(CHECKSUM_START);
        String computed = computeCrc8(value.substring(0, CRC_DATA_LENGTH).getBytes(StandardCharsets.UTF_8));
        if (!declared.equals(computed)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, ERR_CHECKSUM_MISMATCH, declared, computed));
        }
    }

    private static boolean isUppercaseHex(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F');
    }

    /**
     * Computes the KSeF CRC-8 checksum.
     *
     * Implementation kept in {@code int} with explicit {@code & 0xFF} masking
     * to avoid Java's signed-byte sign extension bugs that bite naive CRC
     * implementations on input bytes >= 0x80.
     */
    private static String computeCrc8(byte[] data) {
        int crc = CRC_INIT;
        for (byte b : data) {
            crc ^= b & CRC_BYTE_MASK;
            for (int i = 0; i < BITS_PER_BYTE; i++) {
                crc = (crc & CRC_HIGH_BIT) == 0
                        ? (crc << 1) & CRC_BYTE_MASK
                        : ((crc << 1) ^ CRC_POLYNOMIAL) & CRC_BYTE_MASK;
            }
        }
        return String.format(Locale.ROOT, CRC_FORMAT, crc);
    }
}
