/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KsefNumberTest {

    /**
     * Canonical example from {@code ksef-docs/faktury/numer-ksef.md:17}.
     * Components: NIP {@code 5265877635}, date {@code 20250826},
     * technical {@code 0100001AF629}, CRC-8 {@code AF}.
     */
    private static final String SPEC_EXAMPLE = "5265877635-20250826-0100001AF629-AF";
    private static final String SPEC_EXAMPLE_NIP = "5265877635";
    private static final String SPEC_EXAMPLE_TECHNICAL = "0100001AF629";
    private static final String SPEC_EXAMPLE_CHECKSUM = "AF";
    private static final LocalDate SPEC_EXAMPLE_DATE = LocalDate.of(2025, Month.AUGUST, 26);

    /**
     * Second spec vector from {@code ksef-docs/api-changelog.md}, used both
     * in the duplicate-error example and the {@code originalKsefNumber}
     * field of a server response. Cross-checks the CRC-8 algorithm against
     * a different technical part and date.
     */
    private static final String SPEC_VECTOR_2 = "5265877635-20250626-010080DD2B5E-26";
    private static final String SPEC_VECTOR_2_TECHNICAL = "010080DD2B5E";
    private static final String SPEC_VECTOR_2_CHECKSUM = "26";
    private static final LocalDate SPEC_VECTOR_2_DATE = LocalDate.of(2025, Month.JUNE, 26);

    private static final String LENGTH_TOO_SHORT = "5265877635-20250826-0100001AF629-A";
    private static final String LENGTH_TOO_LONG = "5265877635-20250826-0100001AF629-AFF";
    private static final String SEPARATOR_WRONG_FIRST = "5265877635X20250826-0100001AF629-AF";
    private static final String SEPARATOR_WRONG_SECOND = "5265877635-20250826X0100001AF629-AF";
    private static final String SEPARATOR_WRONG_THIRD = "5265877635-20250826-0100001AF629XAF";
    private static final String NIP_WITH_LETTER = "526587763A-20250826-0100001AF629-AF";
    private static final String DATE_INVALID_MONTH = "5265877635-20251326-0100001AF629-AF";
    private static final String DATE_INVALID_DAY = "5265877635-20250230-0100001AF629-AF";
    private static final String TECHNICAL_LOWERCASE = "5265877635-20250826-0100001af629-AF";
    private static final String TECHNICAL_NON_HEX = "5265877635-20250826-0100001AG629-AF";
    private static final String CHECKSUM_LOWERCASE = "5265877635-20250826-0100001AF629-af";
    private static final String CHECKSUM_NON_HEX = "5265877635-20250826-0100001AF629-AG";
    private static final String CHECKSUM_VALUE_WRONG = "5265877635-20250826-0100001AF629-00";

    @Test
    void parse_whenSpecExample_acceptsAndExposesAllSegments() {
        // when
        KsefNumber number = KsefNumber.parse(SPEC_EXAMPLE);

        // then
        assertEquals(SPEC_EXAMPLE, number.value());
        assertEquals(SPEC_EXAMPLE_NIP, number.sellerNip());
        assertEquals(SPEC_EXAMPLE_DATE, number.acceptanceDate());
        assertEquals(SPEC_EXAMPLE_TECHNICAL, number.technicalPart());
        assertEquals(SPEC_EXAMPLE_CHECKSUM, number.checksum());
    }

    @Test
    void parse_whenSecondSpecVector_acceptsAndExposesAllSegments() {
        // when
        KsefNumber number = KsefNumber.parse(SPEC_VECTOR_2);

        // then
        assertEquals(SPEC_VECTOR_2, number.value());
        assertEquals(SPEC_EXAMPLE_NIP, number.sellerNip());
        assertEquals(SPEC_VECTOR_2_DATE, number.acceptanceDate());
        assertEquals(SPEC_VECTOR_2_TECHNICAL, number.technicalPart());
        assertEquals(SPEC_VECTOR_2_CHECKSUM, number.checksum());
    }

    @Test
    void parse_whenSpecExample_toStringMatchesValue() {
        // when
        KsefNumber number = KsefNumber.parse(SPEC_EXAMPLE);

        // then
        assertEquals(SPEC_EXAMPLE, number.toString());
    }

    @Test
    void canonicalConstructor_whenSpecExample_isEquivalentToParse() {
        // when
        KsefNumber viaParse = KsefNumber.parse(SPEC_EXAMPLE);
        KsefNumber viaCtor = new KsefNumber(SPEC_EXAMPLE);

        // then
        assertEquals(viaParse, viaCtor);
        assertEquals(viaParse.hashCode(), viaCtor.hashCode());
    }

    @Test
    void parse_whenNull_throwsNullPointer() {
        // when / then
        assertThrows(NullPointerException.class, () -> KsefNumber.parse(null));
    }

    @Test
    void parse_whenLengthTooShort_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(LENGTH_TOO_SHORT));
    }

    @Test
    void parse_whenLengthTooLong_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(LENGTH_TOO_LONG));
    }

    @Test
    void parse_whenFirstSeparatorMissing_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(SEPARATOR_WRONG_FIRST));
    }

    @Test
    void parse_whenSecondSeparatorMissing_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(SEPARATOR_WRONG_SECOND));
    }

    @Test
    void parse_whenThirdSeparatorMissing_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(SEPARATOR_WRONG_THIRD));
    }

    @Test
    void parse_whenNipContainsLetter_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(NIP_WITH_LETTER));
    }

    @Test
    void parse_whenDateMonthInvalid_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(DATE_INVALID_MONTH));
    }

    @Test
    void parse_whenDateDayInvalid_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(DATE_INVALID_DAY));
    }

    @Test
    void parse_whenTechnicalLowercase_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(TECHNICAL_LOWERCASE));
    }

    @Test
    void parse_whenTechnicalNonHex_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(TECHNICAL_NON_HEX));
    }

    @Test
    void parse_whenChecksumLowercase_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(CHECKSUM_LOWERCASE));
    }

    @Test
    void parse_whenChecksumNonHex_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(CHECKSUM_NON_HEX));
    }

    @Test
    void parse_whenChecksumValueIncorrect_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefNumber.parse(CHECKSUM_VALUE_WRONG));
    }

    @Test
    void equals_whenSameValue_areEqual() {
        // given
        KsefNumber first = KsefNumber.parse(SPEC_EXAMPLE);
        KsefNumber second = KsefNumber.parse(SPEC_EXAMPLE);

        // then
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
