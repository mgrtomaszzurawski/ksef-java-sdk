/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown by the public {@code *AndAwait(builder, Duration)} helpers
 * (Codex 2026-05-05 #10 / F7) when the supplied timeout elapses before
 * the polled operation reaches a terminal state.
 *
 * <p>Lives in {@code sdk.exception} (an exported JPMS package) so
 * modular consumers can import it directly. The internal
 * poll-until-terminal helper that throws it remains in
 * {@code sdk.internal.runtime}.
 *
 * @since 1.0.0
 */
public class KsefAsyncTimeoutException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefAsyncTimeoutException(String message) {
        super(message, null);
    }
}
