/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown on HTTP 404 (Not Found) or 410 (Gone).
 */
public class KsefNotFoundException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefNotFoundException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
