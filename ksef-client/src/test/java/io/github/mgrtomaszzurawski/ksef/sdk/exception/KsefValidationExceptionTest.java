/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Direct coverage for {@link KsefValidationException} guards that the
 * factory path through {@link KsefException#of(String, Throwable, int, String)}
 * does not reach (it always supplies a non-null list).
 */
class KsefValidationExceptionTest {

    private static final String MESSAGE = "validation failed";
    private static final String RESPONSE_BODY = "{}";
    private static final int STATUS_CODE = 400;
    private static final int TEST_CODE = 21405;
    private static final String TEST_DESCRIPTION = "Invalid field";

    @Test
    void constructor_whenErrorsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new KsefValidationException(MESSAGE, null, STATUS_CODE, RESPONSE_BODY, null));
    }

    @Test
    void constructor_takesDefensiveCopyOfErrors() {
        // Mutating the source list after construction must not affect the
        // exception's errors() view — pins the immutability contract.
        List<KsefValidationError> source = new ArrayList<>();
        source.add(new KsefValidationError(TEST_CODE, TEST_DESCRIPTION, List.of()));

        KsefValidationException exception =
                new KsefValidationException(MESSAGE, null, STATUS_CODE, RESPONSE_BODY, source);
        source.clear();

        assertEquals(1, exception.errors().size());
        assertEquals(TEST_CODE, exception.errors().get(0).code());
    }

    @Test
    void exceptionCode_whenErrorsEmpty_returnsNull() {
        KsefValidationException exception =
                new KsefValidationException(MESSAGE, null, STATUS_CODE, RESPONSE_BODY, List.of());

        assertNull(exception.exceptionCode());
    }

    @Test
    void exceptionCode_whenErrorsPresent_returnsFirstErrorCode() {
        // Pins the override semantics — base class returns null, this
        // subclass returns the first parsed error's code.
        List<KsefValidationError> errors = List.of(
                new KsefValidationError(TEST_CODE, TEST_DESCRIPTION, List.of()));

        KsefValidationException exception =
                new KsefValidationException(MESSAGE, null, STATUS_CODE, RESPONSE_BODY, errors);

        assertEquals(Integer.valueOf(TEST_CODE), exception.exceptionCode());
    }
}
