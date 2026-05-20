/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.RetryHandler;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

class RetryHandlerTest {

    private static final String OPERATION_NAME = "test-operation";
    private static final String EXPECTED_RESULT = "success";
    private static final String ERROR_MESSAGE = "server error";
    private static final String RESPONSE_BODY = "{}";
    @Test
    void execute_whenNoRetry_callsOnce() {
        // given
        RetryPolicy policy = RetryPolicy.builder().enabled(false).build();
        RetryHandler handler = new RetryHandler(policy);

        // when
        String result = handler.execute(() -> EXPECTED_RESULT, OPERATION_NAME);

        // then
        assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    void execute_whenSuccess_returnsResult() {
        // given
        RetryPolicy policy = RetryPolicy.builder().build();
        RetryHandler handler = new RetryHandler(policy);

        // when
        String result = handler.execute(() -> EXPECTED_RESULT, OPERATION_NAME);

        // then
        assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    void execute_whenServerErrorAndRetryDisabled_throwsImmediately() {
        // given
        RetryPolicy policy = RetryPolicy.builder().retryOn5xx(false).build();
        RetryHandler handler = new RetryHandler(policy);

        // then
        assertThrows(KsefServerException.class, () ->
                handler.execute(() -> {
                    throw new KsefServerException(ERROR_MESSAGE, null, TestHttpConstants.HTTP_SERVER_ERROR, RESPONSE_BODY);
                }, OPERATION_NAME));
    }

    @Test
    void execute_whenRateLimitAndRetryDisabled_throwsImmediately() {
        // given
        RetryPolicy policy = RetryPolicy.builder().retryOn429(false).build();
        RetryHandler handler = new RetryHandler(policy);

        // then
        assertThrows(KsefRateLimitException.class, () ->
                handler.execute(() -> {
                    throw new KsefRateLimitException(ERROR_MESSAGE, null, TestHttpConstants.HTTP_TOO_MANY_REQUESTS, RESPONSE_BODY);
                }, OPERATION_NAME));
    }

    @Test
    void execute_whenIOException_wrapsInNetworkException() {
        // given
        RetryPolicy policy = RetryPolicy.builder().enabled(false).build();
        RetryHandler handler = new RetryHandler(policy);

        // then
        assertThrows(KsefNetworkException.class, () ->
                handler.execute(() -> {
                    throw new IOException("connection refused");
                }, OPERATION_NAME));
    }

    @Test
    void executePost_whenRetryPostDisabled_callsOnce() {
        // given
        RetryPolicy policy = RetryPolicy.builder().retryPost(false).build();
        RetryHandler handler = new RetryHandler(policy);

        // when
        String result = handler.executePost(() -> EXPECTED_RESULT, OPERATION_NAME);

        // then
        assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    void run_whenSuccess_completes() {
        // given
        RetryPolicy policy = RetryPolicy.builder().enabled(false).build();
        RetryHandler handler = new RetryHandler(policy);
        boolean[] called = {false};

        // when
        handler.run(() -> called[0] = true, OPERATION_NAME);

        // then
        assertEquals(true, called[0]);
    }
}
