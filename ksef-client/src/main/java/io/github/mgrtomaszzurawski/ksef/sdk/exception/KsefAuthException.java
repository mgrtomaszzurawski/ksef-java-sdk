/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;
/**
 * Thrown on HTTP 401 (Unauthorized) or 403 (Forbidden).
 * Indicates authentication or authorization failure.
 */
public class KsefAuthException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefAuthException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
