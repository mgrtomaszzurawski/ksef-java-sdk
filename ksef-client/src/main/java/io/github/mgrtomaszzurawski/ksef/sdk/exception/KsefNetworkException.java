/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown on I/O errors, timeouts, and connection failures.
 * Status code is always 0 (no HTTP response received).
 */
public class KsefNetworkException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
