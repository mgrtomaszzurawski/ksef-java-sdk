/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry handler with configurable backoff for KSeF API calls.
 *
 * @since 1.0.0
 */
public final class RetryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);

    private static final String LOG_RETRY = "Retry attempt {}/{} after {}ms for: {}";
    private static final String LOG_EXHAUSTED = "All {} retry attempts exhausted for: {}";
    private static final String ERR_RETRY_LOOP_NEVER_ENTERED =
            "Retry loop exited without executing any attempt — likely an invalid RetryPolicy.maxAttempts (must be >= 1)";
    private static final String ERR_KSEF_UNREACHABLE =
            "KSeF is unreachable (connection-level failure) — switch to offline mode";
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
        return java.util.Objects.requireNonNull(doExecute(call, operationName, false));
    }

    /**
     * Execute a POST-like supplier with retry logic, returning a value.
     * POST retry is controlled by {@link RetryPolicy#retryPost()}.
     */
    public <T> T executePost(ApiCall<T> call, String operationName) {
        return java.util.Objects.requireNonNull(doExecute(call, operationName, true));
    }

    /**
     * Execute a void operation with retry logic. Treated as GET-like
     * (idempotent) for retry classification — POST/DELETE no-content
     * operations must use {@link #runPost(ApiRunnable, String)} so
     * {@link RetryPolicy#retryPost()} is honored.
     */
    public void run(ApiRunnable call, String operationName) {
        // Wrap as a Boolean-returning ApiCall so the @NonNull return contract
        // of doExecute is preserved. The Boolean value is discarded.
        ApiCall<Boolean> wrapped = () -> {
            call.run();
            return Boolean.TRUE;
        };
        doExecute(wrapped, operationName, false);
    }

    /**
     * Execute a void POST/DELETE operation with retry logic. Honors
     * {@link RetryPolicy#retryPost()} so mutating operations (session close,
     * token revoke, certificate revoke, testdata mutators) are not silently
     * retried when the consumer explicitly disables POST retry.
     */
    public void runPost(ApiRunnable call, String operationName) {
        ApiCall<Boolean> wrapped = () -> {
            call.run();
            return Boolean.TRUE;
        };
        doExecute(wrapped, operationName, true);
    }

    private <T> @Nullable T doExecute(ApiCall<T> call, String operationName, boolean isPost) {
        if (!policy.enabled() || (isPost && !policy.retryPost())) {
            return callOnce(call, operationName);
        }

        KsefException lastException = sentinelForUnreachableLoopExit();
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            AttemptResult<T> result = attemptOnce(call, attempt, operationName);
            if (result.success()) {
                return result.value();
            }
            lastException = result.exception();
            if (result.terminal()) {
                break;
            }
            sleepBeforeRetry(attempt, operationName, lastException);
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
            return AttemptResult.failed(mapIoFailure(exception, operationName), terminal);
        }
    }

    private static KsefException mapIoFailure(IOException exception, String operationName) {
        // Connection-level failures indicate KSeF is unreachable rather
        // than just slow — surface them as KsefServerException so
        // consumers can branch on offline-mode policy. Other IOExceptions
        // (read timeout, malformed response) stay as KsefServerException.
        if (isUnavailableTransport(exception)) {
            return new KsefServerException(ERR_KSEF_UNREACHABLE + " — " + operationName, exception);
        }
        return new KsefServerException(operationName, exception);
    }

    private static boolean isUnavailableTransport(IOException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record AttemptResult<T>(@Nullable T value, @Nullable KsefException exception, boolean terminal, boolean success) {
        static <T> AttemptResult<T> success(@Nullable T value) {
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
        return exception instanceof KsefServerException;
    }

    private <T> @Nullable T callOnce(ApiCall<T> call, String operationName) {
        try {
            return call.execute();
        } catch (IOException exception) {
            throw mapIoFailure(exception, operationName);
        }
    }

    private void sleepBeforeRetry(int attempt, String operationName, @Nullable KsefException lastException) {
        long jitteredMillis = computeBackoffMillis(attempt, lastException);
        LOGGER.warn(LOG_RETRY, attempt, policy.maxAttempts(), jitteredMillis, operationName);
        try {
            Thread.sleep(jitteredMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefServerException(operationName, interrupted);
        }
    }

    private long computeBackoffMillis(int attempt, @Nullable KsefException lastException) {
        @Nullable Long serverHint = retryAfterSeconds(lastException);
        if (serverHint != null) {
            long capped = Math.min(serverHint, policy.maxRetryAfterSeconds());
            return capped * MILLIS_PER_SECOND;
        }
        long baseMillis = calculateBackoff(attempt);
        return ThreadLocalRandom.current().nextLong(baseMillis / JITTER_DIVISOR, baseMillis + 1);
    }

    private static @Nullable Long retryAfterSeconds(@Nullable KsefException exception) {
        if (exception instanceof KsefRateLimitException rateLimit) {
            return rateLimit.retryAfterSeconds();
        }
        return null;
    }

    /**
     * Returns a sentinel exception that should never surface — only reachable
     * if {@code RetryPolicy.maxAttempts} validation is bypassed.
     * {@code RetryPolicy.Builder} rejects {@code maxAttempts < 1}, so this
     * branch is unreachable in normal flow.
     */
    private static KsefException sentinelForUnreachableLoopExit() {
        return new KsefException(ERR_RETRY_LOOP_NEVER_ENTERED,
                new IllegalStateException(ERR_RETRY_LOOP_NEVER_ENTERED));
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
     * Functional interface for API calls that return a value. The value
     * may be {@code null} when the call wraps a void operation (see the
     * void-returning {@code run}/{@code runPost} entry points which
     * lambda over an {@link ApiRunnable}).
     */
    @FunctionalInterface
    public interface ApiCall<T> {
        @Nullable T execute() throws IOException;
    }

    /**
     * Functional interface for void API calls.
     */
    @FunctionalInterface
    public interface ApiRunnable {
        void run() throws IOException;
    }
}
