/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct coverage for the {@link KsefValidationError} record's compact
 * constructor (null guards + defensive copy of {@code details}) and the
 * {@link KsefValidationError#of(int, String, String)} convenience factory.
 */
class KsefValidationErrorTest {

    private static final int TEST_CODE = 21405;
    private static final String TEST_DESCRIPTION = "Invalid field";
    private static final String TEST_DETAIL = "filters.dateRange.from is in the future";

    @Test
    void constructor_whenDescriptionNull_throwsNullPointerException() {
        List<String> emptyDetails = List.of();
        assertThrows(NullPointerException.class,
                () -> new KsefValidationError(TEST_CODE, null, emptyDetails));
    }

    @Test
    void constructor_whenDetailsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new KsefValidationError(TEST_CODE, TEST_DESCRIPTION, null));
    }

    @Test
    void constructor_takesDefensiveCopyOfDetails() {
        // Mutating the source list after construction must not affect
        // the record's view — pins the immutability contract.
        List<String> source = new ArrayList<>();
        source.add(TEST_DETAIL);

        KsefValidationError error = new KsefValidationError(TEST_CODE, TEST_DESCRIPTION, source);
        source.clear();

        assertEquals(1, error.details().size());
        assertEquals(TEST_DETAIL, error.details().get(0));
    }

    @Test
    void of_whenDetailNonNull_returnsSingleElementList() {
        KsefValidationError error = KsefValidationError.of(TEST_CODE, TEST_DESCRIPTION, TEST_DETAIL);

        assertEquals(TEST_CODE, error.code());
        assertEquals(TEST_DESCRIPTION, error.description());
        assertEquals(1, error.details().size());
        assertEquals(TEST_DETAIL, error.details().get(0));
    }

    @Test
    void of_whenDetailNull_returnsEmptyList() {
        KsefValidationError error = KsefValidationError.of(TEST_CODE, TEST_DESCRIPTION, null);

        assertTrue(error.details().isEmpty());
    }
}
