/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.time.Duration;
import java.time.Instant;
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
     * Poll {@code statusFetcher} every {@code pollInterval} until
     * {@code isTerminal} returns true or {@code timeout} elapses.
     *
     * @param operationName diagnostic name for the timeout exception
     * @param statusFetcher supplies the current status object on each tick
     * @param isTerminal returns true when the status object represents a
     *     terminal state (success or failure — caller decides which)
     * @param statusCodeOf optional accessor for "last seen status code"
     *     used in the timeout message; may be null
     * @param timeout overall budget; throws {@link KsefAsyncTimeoutException}
     *     when exceeded
     * @param pollInterval delay between polls (clamped to
     *     [{@value #MIN_POLL_MILLIS}, {@value #MAX_POLL_MILLIS}] ms)
     * @return the first terminal status object returned by
     *     {@code statusFetcher}
     */
    public static <S> S awaitTerminal(String operationName,
                                      Supplier<S> statusFetcher,
                                      Function<S, Boolean> isTerminal,
                                      Function<S, Object> statusCodeOf,
                                      Duration timeout,
                                      Duration pollInterval) {
        long pollMillis = clamp(pollInterval == null
                ? DEFAULT_POLL_INTERVAL.toMillis() : pollInterval.toMillis());
        Instant deadline = Instant.now().plus(timeout);
        S status = null;
        while (true) {
            status = statusFetcher.get();
            if (Boolean.TRUE.equals(isTerminal.apply(status))) {
                return status;
            }
            if (Instant.now().isAfter(deadline)) {
                Object lastCode = statusCodeOf == null ? "?" : statusCodeOf.apply(status);
                throw new KsefAsyncTimeoutException(String.format(
                        ERR_TIMEOUT_FORMAT, operationName, timeout, lastCode));
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

    /**
     * Thrown by {@link AsyncOperationAwaiter#awaitTerminal} when the
     * caller-supplied timeout elapses before the operation reached a
     * terminal state. Public so consumers using {@code *AndAwait} can
     * handle it specifically.
     *
     * @since 1.0.0
     */
    public static final class KsefAsyncTimeoutException extends KsefException {

        private static final long serialVersionUID = 1L;

        public KsefAsyncTimeoutException(String message) {
            super(message, null);
        }
    }
}
