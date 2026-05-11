/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Public utility for the "submit + poll a status endpoint until terminal"
 * pattern.
 *
 * <p>The KSeF SDK exposes async-by-spec operations (certificate enrollment,
 * token generation, permission grant/revoke) as bare submit calls returning
 * a reference number; the caller polls the corresponding status endpoint
 * until the operation reaches a terminal state. {@code KsefAsync} provides
 * the canonical poll loop for that pattern, so consumers do not have to
 * write the timeout / interval / interrupt-handling boilerplate themselves.
 *
 * <p>Typical use (terminal threshold pulled from the relevant client interface
 * — see {@code Permissions.TERMINAL_STATUS_CODE_THRESHOLD} for the
 * documented poll-loop terminal value):
 * <pre>{@code
 * EnrollCertificateResult result = client.certificates().enroll(request);
 * CertificateEnrollmentStatus terminal = KsefAsync.awaitTerminal(
 *     new KsefAsync.Config<>(
 *         "enrollCertificate",
 *         () -> client.certificates().getEnrollmentStatus(result.referenceNumber()),
 *         status -> status.status() != null
 *                   && status.status().code() >= Permissions.TERMINAL_STATUS_CODE_THRESHOLD,
 *         status -> status.status() == null ? null : status.status().code(),
 *         Duration.ofMinutes(5),
 *         null));   // null = use default poll interval
 * }</pre>
 *
 * <p>Default poll interval is 1 second. Configurable interval is clamped
 * to {@code [100ms, 5s]} to bound server load and keep reactivity bounded
 * for long-running operations.
 *
 * @since 1.0.0
 */
public final class KsefAsync {

    /** Default poll interval — balances reactivity against server load. */
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);

    private static final long MIN_POLL_MILLIS = 100L;
    private static final long MAX_POLL_MILLIS = 5_000L;

    private static final String ERR_TIMEOUT_FORMAT =
            "Async operation %s did not reach terminal state within %s (last status code: %s)";
    private static final String ERR_INTERRUPTED = "Interrupted while awaiting async operation";

    private KsefAsync() { }

    /**
     * Configuration for {@link #awaitTerminal(Config)}. Bundles the six
     * parameters as a record so callers can express the await contract
     * without a wide positional argument list.
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
                             Predicate<S> isTerminal,
                             @Nullable Function<S, @Nullable Object> statusCodeOf,
                             Duration timeout,
                             @Nullable Duration pollInterval) {
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
     * @throws KsefAsyncTimeoutException when the timeout elapses before any
     *     polled status reaches a terminal state
     * @throws KsefException when the polling thread is interrupted
     */
    public static <S> S awaitTerminal(Config<S> config) {
        long pollMillis = clamp(config.pollInterval() == null
                ? DEFAULT_POLL_INTERVAL.toMillis() : config.pollInterval().toMillis());
        Instant deadline = Instant.now().plus(config.timeout());
        while (true) {
            S status = config.statusFetcher().get();
            if (config.isTerminal().test(status)) {
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
