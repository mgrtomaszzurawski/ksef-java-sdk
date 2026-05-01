/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.transport;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry handler with configurable backoff for KSeF API calls.
 */
public final class RetryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);

    private static final String LOG_RETRY = "Retry attempt {}/{} after {}ms for: {}";
    private static final String LOG_EXHAUSTED = "All {} retry attempts exhausted for: {}";
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

        KsefException lastException = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return call.execute();
            } catch (KsefException exception) {
                lastException = exception;
                if (!isRetryable(exception) || attempt == policy.maxAttempts()) {
                    break;
                }
                sleepBeforeRetry(attempt, operationName);
            } catch (IOException exception) {
                lastException = new KsefNetworkException(operationName, exception);
                if (attempt == policy.maxAttempts()) {
                    break;
                }
                sleepBeforeRetry(attempt, operationName);
            }
        }

        LOGGER.error(LOG_EXHAUSTED, policy.maxAttempts(), operationName);
        throw lastException;
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
