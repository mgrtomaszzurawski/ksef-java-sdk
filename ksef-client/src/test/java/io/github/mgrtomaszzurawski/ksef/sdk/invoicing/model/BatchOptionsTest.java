/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the validation contract on {@link BatchOptions}: parallelism is
 * symmetrically rejected outside [{@link BatchOptions#MIN_PARALLELISM},
 * {@link BatchOptions#MAX_PARALLELISM}], non-positive timeouts are
 * rejected, and {@link BatchOptions#defaults()} returns a usable instance.
 *
 * <p>Symmetric throw matters: silent clamping on overshoot would surprise
 * callers whose documented setting is not the one used.
 */
class BatchOptionsTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    void constructor_whenParallelismBelowMinimum_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new BatchOptions(ONE_MINUTE, BatchOptions.MIN_PARALLELISM - 1));
    }

    @Test
    void constructor_whenParallelismAboveMaximum_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new BatchOptions(ONE_MINUTE, BatchOptions.MAX_PARALLELISM + 1));
    }

    @Test
    void constructor_whenParallelismAtMinimum_succeeds() {
        BatchOptions options = new BatchOptions(ONE_MINUTE, BatchOptions.MIN_PARALLELISM);

        assertEquals(BatchOptions.MIN_PARALLELISM, options.parallelism());
    }

    @Test
    void constructor_whenParallelismAtMaximum_succeeds() {
        BatchOptions options = new BatchOptions(ONE_MINUTE, BatchOptions.MAX_PARALLELISM);

        assertEquals(BatchOptions.MAX_PARALLELISM, options.parallelism());
    }

    @Test
    void constructor_whenTimeoutZero_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new BatchOptions(Duration.ZERO, BatchOptions.MIN_PARALLELISM));
    }

    @Test
    void constructor_whenTimeoutNegative_throws() {
        Duration negativeTimeout = Duration.ofMinutes(-1);
        assertThrows(IllegalArgumentException.class, () ->
                new BatchOptions(negativeTimeout, BatchOptions.MIN_PARALLELISM));
    }

    @Test
    void constructor_whenTimeoutNull_throws() {
        assertThrows(NullPointerException.class, () ->
                new BatchOptions(null, BatchOptions.MIN_PARALLELISM));
    }

    @Test
    void defaults_returnsUsableInstance() {
        BatchOptions defaults = BatchOptions.defaults();

        assertNotNull(defaults.timeout());
        assertEquals(Duration.ofMinutes(30), defaults.timeout());
        assertEquals(4, defaults.parallelism());
    }
}
