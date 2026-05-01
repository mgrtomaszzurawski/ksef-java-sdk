/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry handler with configurable backoff for KSeF API calls.
 */
public final class RetryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);

    private static final String LOG_RETRY = "Retry attempt {}/{} after {}ms for: {}";
    private static final String LOG_EXHAUSTED = "All {} retry attempts exhausted for: {}";
    private static final String ERR_RETRY_LOOP_NEVER_ENTERED =
            "Retry loop exited without executing any attempt — likely an invalid RetryPolicy.maxAttempts (must be >= 1)";
    private static final long BACKOFF_BASE_MILLIS = 1000L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final int JITTER_DIVISOR = 2;

    private final RetryPolicy policy;

    public RetryHandler(RetryPolicy policy) {
        this.policy = policy;
    }

    /**
     * Execute a supplier with retry logic, returning a value.
     */
    public <T> T execute(ApiCall<T> call, String operationName) {
        return doExecute(call, operationName, false);
    }

    /**
     * Execute a POST-like supplier with retry logic, returning a value.
     * POST retry is controlled by {@link RetryPolicy#retryPost()}.
     */
    public <T> T executePost(ApiCall<T> call, String operationName) {
        return doExecute(call, operationName, true);
    }

    /**
     * Execute a void operation with retry logic.
     */
    public void run(ApiRunnable call, String operationName) {
        doExecute(() -> {
            call.run();
            return null;
        }, operationName, false);
    }

    private <T> T doExecute(ApiCall<T> call, String operationName, boolean isPost) {
        if (!policy.enabled() || (isPost && !policy.retryPost())) {
            return callOnce(call, operationName);
        }

        // Sentinel — only surfaced if RetryPolicy.maxAttempts validation is bypassed.
        // RetryPolicy.Builder rejects maxAttempts < 1, so this branch is unreachable in normal flow.
        KsefException lastException = new KsefException(ERR_RETRY_LOOP_NEVER_ENTERED,
                new IllegalStateException(ERR_RETRY_LOOP_NEVER_ENTERED));
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            AttemptResult<T> result = attemptOnce(call, attempt, operationName);
            if (result.success()) {
                return result.value();
            }
            lastException = result.exception();
            if (result.terminal()) {
                break;
            }
            sleepBeforeRetry(attempt, operationName);
        }

        LOGGER.error(LOG_EXHAUSTED, policy.maxAttempts(), operationName);
        throw lastException;
    }

    private <T> AttemptResult<T> attemptOnce(ApiCall<T> call, int attempt, String operationName) {
        try {
            return AttemptResult.success(call.execute());
        } catch (KsefException exception) {
            boolean terminal = !isRetryable(exception) || attempt == policy.maxAttempts();
            return AttemptResult.failed(exception, terminal);
        } catch (IOException exception) {
            boolean terminal = attempt == policy.maxAttempts();
            return AttemptResult.failed(new KsefNetworkException(operationName, exception), terminal);
        }
    }

    private record AttemptResult<T>(T value, KsefException exception, boolean terminal, boolean success) {
        static <T> AttemptResult<T> success(T value) {
            return new AttemptResult<>(value, null, false, true);
        }

        static <T> AttemptResult<T> failed(KsefException exception, boolean terminal) {
            return new AttemptResult<>(null, exception, terminal, false);
        }
    }

    private boolean isRetryable(KsefException exception) {
        if (exception instanceof KsefRateLimitException) {
            return policy.retryOn429();
        }
        if (exception instanceof KsefServerException) {
            return policy.retryOn5xx();
        }
        return exception instanceof KsefNetworkException;
    }

    private <T> T callOnce(ApiCall<T> call, String operationName) {
        try {
            return call.execute();
        } catch (IOException exception) {
            throw new KsefNetworkException(operationName, exception);
        }
    }

    private void sleepBeforeRetry(int attempt, String operationName) {
        long baseMillis = calculateBackoff(attempt);
        long jitteredMillis = ThreadLocalRandom.current()
                .nextLong(baseMillis / JITTER_DIVISOR, baseMillis + 1);
        LOGGER.warn(LOG_RETRY, attempt, policy.maxAttempts(), jitteredMillis, operationName);
        try {
            Thread.sleep(jitteredMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(operationName, interrupted);
        }
    }

    private long calculateBackoff(int attempt) {
        if (policy.backoffStrategy() == RetryPolicy.BackoffStrategy.EXPONENTIAL) {
            long exponentialSeconds = 1L << attempt;
            long cappedSeconds = Math.min(exponentialSeconds, policy.maxRetryAfterSeconds());
            return cappedSeconds * MILLIS_PER_SECOND;
        }
        return BACKOFF_BASE_MILLIS;
    }

    /**
     * Functional interface for API calls that return a value.
     */
    @FunctionalInterface
    public interface ApiCall<T> {
        T execute() throws IOException;
    }

    /**
     * Functional interface for void API calls.
     */
    @FunctionalInterface
    public interface ApiRunnable {
        void run() throws IOException;
    }
}
