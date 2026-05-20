/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.async;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.async.KsefAsync;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavior of the public {@link KsefAsync#awaitTerminal} poll loop:
 * returns as soon as the predicate is satisfied, throws
 * {@link KsefAsyncTimeoutException} on deadline exceeded, and accepts
 * configured poll intervals below the documented minimum (clamped
 * internally).
 */
class KsefAsyncTest {

    private static final String OPERATION_NAME = "test";
    private static final int TERMINAL_THRESHOLD = 3;
    private static final int LAST_SEEN_VALUE = 42;
    private static final Duration GENEROUS_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration TIGHT_TIMEOUT = Duration.ofMillis(300);
    private static final Duration STANDARD_POLL = Duration.ofMillis(100);
    private static final Duration BELOW_MIN_POLL = Duration.ofMillis(1);

    @Test
    void awaitTerminal_whenPredicateSatisfied_returnsImmediately() {
        // given
        AtomicInteger ticks = new AtomicInteger();

        // when
        Integer result = KsefAsync.awaitTerminal(
                new KsefAsync.Config<>(
                        OPERATION_NAME,
                        () -> ticks.incrementAndGet(),
                        value -> value >= TERMINAL_THRESHOLD,
                        value -> value,
                        GENEROUS_TIMEOUT,
                        STANDARD_POLL));

        // then
        assertEquals(TERMINAL_THRESHOLD, result);
        assertTrue(ticks.get() >= TERMINAL_THRESHOLD);
    }

    @Test
    void awaitTerminal_whenDeadlineExceeded_throwsWithOperationNameAndLastStatus() {
        // given
        KsefAsync.Config<Integer> neverTerminal = new KsefAsync.Config<>(
                "neverTerminal",
                () -> LAST_SEEN_VALUE,
                value -> false,
                value -> value,
                TIGHT_TIMEOUT,
                STANDARD_POLL);
        // when
        KsefAsyncTimeoutException ex = assertThrows(
                KsefAsyncTimeoutException.class,
                () -> KsefAsync.awaitTerminal(neverTerminal));

        // then
        assertTrue(ex.getMessage().contains("neverTerminal"),
                "operation name in timeout message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(String.valueOf(LAST_SEEN_VALUE)),
                "last status in timeout message: " + ex.getMessage());
    }

    @Test
    void awaitTerminal_whenPollIntervalBelowMinimum_acceptedAndCompletes() {
        // given / when — below-minimum interval is internally clamped, not rejected;
        // an immediate-terminal predicate verifies the call returns under the clamp
        Integer result = KsefAsync.awaitTerminal(
                new KsefAsync.Config<>(
                        OPERATION_NAME,
                        () -> 1,
                        value -> true,
                        value -> value,
                        Duration.ofSeconds(1),
                        BELOW_MIN_POLL));

        // then
        assertEquals(1, result);
    }
}
