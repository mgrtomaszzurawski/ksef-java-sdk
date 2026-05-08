/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
    void of_whenGone_returnsRetentionExpiredException() {
        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, HTTP_GONE, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefRetentionExpiredException.class, result);
        assertInstanceOf(KsefNotFoundException.class, result);
        assertEquals(HTTP_GONE, result.statusCode());
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
    void of_whenBadRequest_returnsValidationException() {
        // when — body is non-validation JSON; parser yields empty errors list
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_BAD_REQUEST, RESPONSE_BODY);

        // then
        assertInstanceOf(KsefValidationException.class, result);
        KsefValidationException validation = (KsefValidationException) result;
        assertEquals(0, validation.errors().size());
        assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, validation.statusCode());
    }

    @Test
    void of_whenBadRequestWithProblemDetails_parsesErrors() {
        // given — RFC 7807 Problem Details with two errors
        String problemDetails = """
                {
                  "title": "Bad Request",
                  "status": 400,
                  "errors": [
                    {"code": 21405, "description": "Invalid field", "details": ["filters.dateRange.from is in the future"]},
                    {"code": 21405, "description": "Invalid field", "details": ["sellerNip is not a valid NIP"]}
                  ]
                }
                """;

        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_BAD_REQUEST, problemDetails);

        // then
        KsefValidationException validation = assertInstanceOf(KsefValidationException.class, result);
        assertEquals(2, validation.errors().size());
        assertEquals(21405, validation.errors().get(0).code());
        assertEquals("filters.dateRange.from is in the future", validation.errors().get(0).details().get(0));
        assertEquals(Integer.valueOf(21405), validation.exceptionCode());
    }

    @Test
    void of_whenBadRequestWithLegacyFormat_parsesErrors() {
        // given — legacy application/json envelope
        String legacyBody = """
                {
                  "exception": {
                    "exceptionDetailList": [
                      {"exceptionCode": 21001, "exceptionDescription": "JSON parse error", "details": ["unexpected token at line 5"]}
                    ]
                  }
                }
                """;

        // when
        KsefException result = KsefException.of(ERROR_MESSAGE, null, TestHttpConstants.HTTP_BAD_REQUEST, legacyBody);

        // then
        KsefValidationException validation = assertInstanceOf(KsefValidationException.class, result);
        assertEquals(1, validation.errors().size());
        assertEquals(21001, validation.errors().get(0).code());
        assertEquals("JSON parse error", validation.errors().get(0).description());
        assertEquals(Integer.valueOf(21001), validation.exceptionCode());
    }

    @Test
    void of_whenBadRequestBodyMalformed_emptyErrorsList() {
        // when — body is not valid JSON at all
        KsefException result = KsefException.of(ERROR_MESSAGE, null,
                TestHttpConstants.HTTP_BAD_REQUEST, "garbage<not>json");

        // then — no parse, but still validation type with empty list
        KsefValidationException validation = assertInstanceOf(KsefValidationException.class, result);
        assertEquals(0, validation.errors().size());
        assertEquals(null, validation.exceptionCode());
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
