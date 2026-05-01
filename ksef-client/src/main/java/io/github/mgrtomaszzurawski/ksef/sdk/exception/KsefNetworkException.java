/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;
/**
 * Thrown on I/O errors, timeouts, and connection failures.
 * Status code is always 0 (no HTTP response received).
 */
public class KsefNetworkException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
