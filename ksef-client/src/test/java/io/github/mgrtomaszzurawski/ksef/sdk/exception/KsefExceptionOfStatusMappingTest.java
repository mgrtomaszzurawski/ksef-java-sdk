/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins {@link KsefException#of(String, Throwable, int, String)} — the
 * functional HTTP-status → typed-exception mapper {@code HttpSupport}
 * uses to surface KSeF errors. Premium SDK contract: consumers catch
 * remediation-action types ({@code KsefValidationException},
 * {@code KsefAuthException}, {@code KsefRateLimitException}, etc.) and
 * react to them — without parsing HTTP status codes themselves.
 *
 * <p>Regression test for R3 plan item #8 + finding G-4a: pins both the
 * dispatch (correct subtype per status code) and the body-parsing
 * (errors list extracted from 400 KSeF Problem Details envelope).
 */
class KsefExceptionOfStatusMappingTest {

    private static final String OPERATION_NAME = "POST /v2/sessions/online";
    private static final String KSEF_ERROR_BODY = """
            {
              "exception": {
                "serviceCode": "21405",
                "serviceName": "Validation failed",
                "exceptionDetailList": [
                  {"exceptionCode": 21405, "exceptionDescription": "NIP invalid"},
                  {"exceptionCode": 21405, "exceptionDescription": "dateRange.from must precede dateRange.to"}
                ]
              }
            }
            """;

    @Test
    void of_whenStatus400_returnsKsefValidationExceptionWithParsedErrors() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 400, KSEF_ERROR_BODY);

        KsefValidationException validation = assertInstanceOf(KsefValidationException.class, result);
        List<KsefValidationError> errors = validation.errors();
        assertNotNull(errors, "errors() must not be null");
        assertEquals(2, errors.size(), "Both KSeF detail entries must surface in errors()");
        assertEquals("NIP invalid", errors.get(0).description());
        assertEquals("dateRange.from must precede dateRange.to", errors.get(1).description());
    }

    @Test
    void of_whenStatus400AndEmptyBody_returnsKsefValidationExceptionWithEmptyErrors() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 400, null);

        KsefValidationException validation = assertInstanceOf(KsefValidationException.class, result);
        assertEquals(0, validation.errors().size(), "Empty body → empty errors list, not null");
    }

    @Test
    void of_whenStatus401_returnsKsefAuthException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 401, null);
        assertInstanceOf(KsefAuthException.class, result);
    }

    @Test
    void of_whenStatus403_returnsKsefAuthException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 403, null);
        assertInstanceOf(KsefAuthException.class, result);
    }

    @Test
    void of_whenStatus404_returnsKsefNotFoundException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 404, null);
        assertInstanceOf(KsefNotFoundException.class, result);
    }

    @Test
    void of_whenStatus410_returnsKsefRetentionExpiredException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 410, null);
        // Retention is the more specific subtype; NotFound is its parent.
        assertInstanceOf(KsefRetentionExpiredException.class, result);
        assertInstanceOf(KsefNotFoundException.class, result);
    }

    @Test
    void of_whenStatus429AndRetryAfter_returnsKsefRateLimitExceptionWithHint() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 429, null, 42L);

        KsefRateLimitException rateLimit = assertInstanceOf(KsefRateLimitException.class, result);
        assertEquals(42L, rateLimit.retryAfterSeconds());
    }

    @Test
    void of_whenStatus500_returnsKsefServerException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 500, null);
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenStatus503_returnsKsefServerException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 503, null);
        assertInstanceOf(KsefServerException.class, result);
    }

    @Test
    void of_whenStatusIsUnmappedClient_returnsGenericKsefException() {
        KsefException result = KsefException.of(OPERATION_NAME, null, 418, null);
        // 418 has no specific subtype — falls through to base KsefException
        assertEquals(KsefException.class, result.getClass());
        assertEquals(418, result.statusCode());
    }
}
