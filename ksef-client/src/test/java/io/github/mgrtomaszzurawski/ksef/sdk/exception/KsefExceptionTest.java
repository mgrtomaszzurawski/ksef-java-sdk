/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

class KsefExceptionTest {

    private static final String ERROR_MESSAGE = "test error";
    private static final String RESPONSE_BODY = "{\"error\":\"test\"}";
    private static final int HTTP_GONE = 410;
    private static final int HTTP_CONFLICT = 409;

    @Test
    void of_whenUnauthorized_returnsAuthException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_UNAUTHORIZED, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefAuthException.class, result);
        assertEquals(TestHttpConstants.HTTP_UNAUTHORIZED, result.statusCode());
        assertEquals(RESPONSE_BODY, result.responseBody());
    }

    @Test
    void of_whenForbidden_returnsAuthException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_FORBIDDEN, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefAuthException.class, result);
    }

    @Test
    void of_whenNotFound_returnsNotFoundException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_NOT_FOUND, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefNotFoundException.class, result);
    }

    @Test
    void of_whenGone_returnsNotFoundException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_GONE, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefNotFoundException.class, result);
    }

    @Test
    void of_whenTooManyRequests_returnsRateLimitException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_TOO_MANY_REQUESTS, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefRateLimitException.class, result);
    }

    @Test
    void of_whenInternalServerError_returnsServerException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_SERVER_ERROR, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenBadGateway_returnsServerException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_BAD_GATEWAY, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenBadRequest_returnsBaseException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_BAD_REQUEST, RESPONSE_BODY);

        // then
        assertEquals(KsefException.class, result.getClass());
    }

    @Test
    void of_whenConflict_returnsBaseException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_CONFLICT, RESPONSE_BODY);

        // then
        assertEquals(KsefException.class, result.getClass());
    }

    @Test
    void constructor_preservesCauseAndFields() {
        // given
        RuntimeException cause = new RuntimeException("root cause");

        // when
        KsefException result = new KsefException(ERROR_MESSAGE, cause, TestHttpConstants.HTTP_BAD_REQUEST, RESPONSE_BODY);

        // then
        assertEquals(ERROR_MESSAGE, result.getMessage());
        assertEquals(cause, result.getCause());
        assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, result.statusCode());
        assertEquals(RESPONSE_BODY, result.responseBody());
    }
}
