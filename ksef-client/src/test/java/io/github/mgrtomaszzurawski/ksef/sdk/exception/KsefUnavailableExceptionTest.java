/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class KsefUnavailableExceptionTest {

    private static final String TEST_MESSAGE = "KSeF is unavailable";
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;
    private static final String SAMPLE_BODY = "{\"error\":\"unavailable\"}";

    @Test
    void instanceHierarchy_whenConstructed_extendsKsefException() {
        // when
        KsefUnavailableException exception = new KsefUnavailableException(TEST_MESSAGE, null,
                HTTP_SERVICE_UNAVAILABLE, SAMPLE_BODY);

        // then
        assertInstanceOf(KsefException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void messageAndCause_whenConstructedWithCause_arePropagated() {
        // given
        IOException cause = new IOException("connection refused");

        // when
        KsefUnavailableException exception = new KsefUnavailableException(TEST_MESSAGE, cause);

        // then
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void statusCodeAndBody_whenConstructedWithStatus_areAccessible() {
        // when
        KsefUnavailableException exception = new KsefUnavailableException(TEST_MESSAGE, null,
                HTTP_SERVICE_UNAVAILABLE, SAMPLE_BODY);

        // then
        assertEquals(HTTP_SERVICE_UNAVAILABLE, exception.statusCode());
        assertEquals(SAMPLE_BODY, exception.responseBody());
    }
}
