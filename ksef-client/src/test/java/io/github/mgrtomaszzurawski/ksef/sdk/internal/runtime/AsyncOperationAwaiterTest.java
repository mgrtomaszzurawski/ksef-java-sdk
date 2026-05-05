/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codex 2026-05-05 #10 / F7 — poll-until-terminal helper.
 */
class AsyncOperationAwaiterTest {

    @Test
    void awaitTerminal_returnsAsSoonAsTerminalReached() {
        AtomicInteger ticks = new AtomicInteger();
        Integer result = AsyncOperationAwaiter.awaitTerminal(
                "test",
                () -> ticks.incrementAndGet(),
                value -> value >= 3,
                value -> value,
                Duration.ofSeconds(5),
                Duration.ofMillis(100));

        assertEquals(3, result);
        assertTrue(ticks.get() >= 3);
    }

    @Test
    void awaitTerminal_throwsOnTimeoutWithStatusCodeInMessage() {
        io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException ex = assertThrows(
                io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException.class,
                () -> AsyncOperationAwaiter.awaitTerminal(
                        "neverTerminal",
                        () -> 42,
                        value -> false,
                        value -> value,
                        Duration.ofMillis(300),
                        Duration.ofMillis(100)));
        assertTrue(ex.getMessage().contains("neverTerminal"),
                "operation name in timeout message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("42"),
                "last status in timeout message: " + ex.getMessage());
    }

    @Test
    void awaitTerminal_clampsPollIntervalToMinimum() {
        // Below-minimum poll interval still works (clamped, not rejected).
        Integer result = AsyncOperationAwaiter.awaitTerminal(
                "test",
                () -> 1,
                value -> true,
                value -> value,
                Duration.ofSeconds(1),
                Duration.ofMillis(1));
        assertEquals(1, result);
    }
}
