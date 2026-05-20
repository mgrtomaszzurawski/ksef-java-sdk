/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Tunables for {@code sessions().batch().submit(...)} / {@code sessions().batch().submitFromFiles(...)}.
 *
 * <p>{@code timeout} bounds the entire synchronous {@code batch().submit} call —
 * encryption + upload + close + status polling + UPO fetch. The SDK enforces
 * the deadline against a monotonic clock. Set generously: KSeF batch processing
 * can take minutes to hours depending on invoice count and server load.
 *
 * <p>{@code parallelism} caps concurrent part uploads. KSeF returns one upload
 * URL per part (max {@value #MAX_PARTS_PER_BATCH}); the SDK uploads them with up
 * to {@code parallelism} workers. Validated to
 * [{@value #MIN_PARALLELISM}, {@value #MAX_PARALLELISM}] — values outside that
 * range throw {@link IllegalArgumentException} symmetrically (silent clamping
 * surprises callers when the documented setting is not the one used).
 *
 * <p><strong>No progress listener.</strong> Per ADR-008/D1, callback-style
 * "SDK calls your function on each event" inverts control of the consumer's
 * thread context. Callers needing progress UI should wrap the synchronous
 * {@code batch().submit} call in {@link java.util.concurrent.CompletableFuture#supplyAsync}
 * or equivalent, owning their own executor and progress mechanism.
 *
 * @param timeout overall budget for the whole {@code batch().submit} call
 * @param parallelism number of concurrent part-upload workers (clamped to
 *     [{@value #MIN_PARALLELISM}, {@value #MAX_PARALLELISM}])
 *
 * @since 0.1.0
 */
public record BatchOptions(Duration timeout, int parallelism) {

    /** Lower clamp for {@link #parallelism()} — single-threaded upload. */
    public static final int MIN_PARALLELISM = 1;
    /** Upper clamp for {@link #parallelism()} — matches KSeF's per-batch part cap. */
    public static final int MAX_PARALLELISM = 16;
    /** Spec maximum part count per batch (KSeF {@code BatchFile.fileParts} limit). */
    public static final int MAX_PARTS_PER_BATCH = 50;

    private static final String ERR_TIMEOUT_NULL = "timeout must not be null";
    private static final String ERR_TIMEOUT_NON_POSITIVE = "timeout must be positive (was %s)";
    private static final String ERR_PARALLELISM_OUT_OF_RANGE =
            "parallelism must be in [" + MIN_PARALLELISM + ", " + MAX_PARALLELISM + "] (was %d)";

    /** Default overall deadline: 30 minutes. */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    /** Default upload concurrency: 4 workers. */
    private static final int DEFAULT_PARALLELISM = 4;

    public BatchOptions {
        Objects.requireNonNull(timeout, ERR_TIMEOUT_NULL);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_TIMEOUT_NON_POSITIVE, timeout));
        }
        if (parallelism < MIN_PARALLELISM || parallelism > MAX_PARALLELISM) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    ERR_PARALLELISM_OUT_OF_RANGE, parallelism));
        }
    }

    /**
     * Sensible defaults for typical batches: 30-minute total deadline,
     * 4 parallel part uploaders.
     *
     * @return default {@link BatchOptions}
     */
    public static BatchOptions defaults() {
        return new BatchOptions(DEFAULT_TIMEOUT, DEFAULT_PARALLELISM);
    }
}
