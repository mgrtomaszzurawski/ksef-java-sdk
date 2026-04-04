/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown when a KSeF session has expired or been terminated.
 * This is a KSeF-specific auth error distinct from generic 401/403.
 */
public class KsefSessionExpiredException extends KsefAuthException {

    private static final long serialVersionUID = 1L;

    public KsefSessionExpiredException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
