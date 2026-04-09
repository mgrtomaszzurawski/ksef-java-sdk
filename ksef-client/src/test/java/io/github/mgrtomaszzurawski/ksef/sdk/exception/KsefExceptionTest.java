/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KsefExceptionTest {

    private static final String ERROR_MESSAGE = "test error";
    private static final String RESPONSE_BODY = "{\"error\":\"test\"}";
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_GONE = 410;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final int HTTP_BAD_GATEWAY = 502;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_CONFLICT = 409;

    @Test
    void of_whenUnauthorized_returnsAuthException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_UNAUTHORIZED, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefAuthException.class, result);
        assertEquals(HTTP_UNAUTHORIZED, result.statusCode());
        assertEquals(RESPONSE_BODY, result.responseBody());
    }

    @Test
    void of_whenForbidden_returnsAuthException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_FORBIDDEN, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefAuthException.class, result);
    }

    @Test
    void of_whenNotFound_returnsNotFoundException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_NOT_FOUND, RESPONSE_BODY);

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
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_TOO_MANY_REQUESTS, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefRateLimitException.class, result);
    }

    @Test
    void of_whenInternalServerError_returnsServerException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_INTERNAL_SERVER_ERROR, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenBadGateway_returnsServerException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_BAD_GATEWAY, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenBadRequest_returnsBaseException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_BAD_REQUEST, RESPONSE_BODY);

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
        KsefException result = new KsefException(ERROR_MESSAGE, cause, HTTP_BAD_REQUEST, RESPONSE_BODY);

        // then
        assertEquals(ERROR_MESSAGE, result.getMessage());
        assertEquals(cause, result.getCause());
        assertEquals(HTTP_BAD_REQUEST, result.statusCode());
        assertEquals(RESPONSE_BODY, result.responseBody());
    }
}
