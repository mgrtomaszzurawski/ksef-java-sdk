/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

/**
 * Threshold constants for KSeF async operation status polling.
 *
 * <p>KSeF operation status codes are conventionally {@code 100/110}
 * (in-progress), {@code 200} (success), and {@code 4xx/5xx} (failure).
 * Anything {@code &gt;= 200} is terminal.
 *
 * <p>Useful when building a poll predicate for
 * {@link KsefAsync#awaitTerminal}:
 * <pre>{@code
 * status -> status.status() != null
 *           && status.status().code() >= KsefAsyncStatus.TERMINAL_STATUS_CODE_THRESHOLD
 * }</pre>
 *
 * @since 1.0.0
 */
public final class KsefAsyncStatus {

    /**
     * Lower bound of the "terminal" status-code range. KSeF marks an
     * async operation as in-progress with codes below {@code 200} and as
     * terminal (success or failure) at or above it.
     */
    public static final int TERMINAL_STATUS_CODE_THRESHOLD = 200;

    private KsefAsyncStatus() {
    }
}
