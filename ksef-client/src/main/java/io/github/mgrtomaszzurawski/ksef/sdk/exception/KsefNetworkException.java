/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on I/O errors, timeouts, and connection failures.
 * Status code is always 0 (no HTTP response received).
 *
 * @since 1.0.0
 */
public class KsefNetworkException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefNetworkException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
