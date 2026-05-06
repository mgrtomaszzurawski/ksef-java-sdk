/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal poll-until-terminal helper used by the public
 * {@code *AndAwait(...)} convenience methods on permission/token/
 * certificate clients (Codex 2026-05-05 #10 / F7).
 *
 * <p>Polls a status endpoint at a configurable interval until either:
 * <ul>
 *   <li>the supplied {@code isTerminal} predicate returns {@code true}
 *       — returns the terminal status object;</li>
 *   <li>the wall-clock timeout elapses — throws
 *       {@link KsefAsyncTimeoutException}.</li>
 * </ul>
 *
 * <p>Default poll interval is 1 second. Min is 100 ms. Long polls cap
 * at 5 s to avoid waking up too rarely on long-running operations.
 *
 * @since 1.0.0
 */
public final class AsyncOperationAwaiter {

    /** Default poll interval — balances reactivity against server load. */
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);

    private static final long MIN_POLL_MILLIS = 100L;
    private static final long MAX_POLL_MILLIS = 5_000L;

    private static final String ERR_TIMEOUT_FORMAT =
            "Async operation %s did not reach terminal state within %s (last status code: %s)";
    private static final String ERR_INTERRUPTED = "Interrupted while awaiting async operation";

    private AsyncOperationAwaiter() { }

    /**
     * Configuration for {@link #awaitTerminal(Config)}. Bundles the six
     * parameters previously taken individually so callers can express
     * the await contract without a wide positional argument list.
     *
     * @param <S> the status type the fetcher yields
     * @param operationName diagnostic name for the timeout exception
     * @param statusFetcher supplies the current status object on each tick
     * @param isTerminal returns true when the status object represents a
     *     terminal state (success or failure — caller decides which)
     * @param statusCodeOf optional accessor for "last seen status code"
     *     used in the timeout message; may be null
     * @param timeout overall budget; throws {@link KsefAsyncTimeoutException}
     *     when exceeded
     * @param pollInterval delay between polls (clamped to
     *     [{@value #MIN_POLL_MILLIS}, {@value #MAX_POLL_MILLIS}] ms);
     *     {@code null} uses {@link #DEFAULT_POLL_INTERVAL}
     */
    public record Config<S>(String operationName,
                             Supplier<S> statusFetcher,
                             Function<S, Boolean> isTerminal,
                             Function<S, Object> statusCodeOf,
                             Duration timeout,
                             Duration pollInterval) {
        public Config {
            Objects.requireNonNull(operationName, "operationName must not be null");
            Objects.requireNonNull(statusFetcher, "statusFetcher must not be null");
            Objects.requireNonNull(isTerminal, "isTerminal must not be null");
            Objects.requireNonNull(timeout, "timeout must not be null");
        }
    }

    /**
     * Poll the configured status fetcher until {@code isTerminal} returns
     * true or {@code timeout} elapses.
     *
     * @return the first terminal status object returned by the fetcher
     */
    public static <S> S awaitTerminal(Config<S> config) {
        long pollMillis = clamp(config.pollInterval() == null
                ? DEFAULT_POLL_INTERVAL.toMillis() : config.pollInterval().toMillis());
        Instant deadline = Instant.now().plus(config.timeout());
        while (true) {
            S status = config.statusFetcher().get();
            if (Boolean.TRUE.equals(config.isTerminal().apply(status))) {
                return status;
            }
            if (Instant.now().isAfter(deadline)) {
                Object lastCode = config.statusCodeOf() == null ? "?" : config.statusCodeOf().apply(status);
                throw new KsefAsyncTimeoutException(String.format(
                        ERR_TIMEOUT_FORMAT, config.operationName(), config.timeout(), lastCode));
            }
            try {
                Thread.sleep(pollMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new KsefException(ERR_INTERRUPTED, ex);
            }
        }
    }

    private static long clamp(long requested) {
        return Math.max(MIN_POLL_MILLIS, Math.min(MAX_POLL_MILLIS, requested));
    }

}
