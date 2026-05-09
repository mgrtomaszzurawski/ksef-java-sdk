/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.common.KsefAsync#awaitTerminal}
 * when the configured timeout elapses before the polled status reaches a
 * terminal state.
 *
 * <p>Lives in {@code sdk.exception} (an exported JPMS package) so modular
 * consumers can import it directly.
 *
 * @since 1.0.0
 */
public class KsefAsyncTimeoutException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefAsyncTimeoutException(String message) {
        super(message, null);
    }
}
