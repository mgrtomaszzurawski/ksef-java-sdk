/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown on HTTP 5xx server errors.
 */
public class KsefServerException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefServerException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
