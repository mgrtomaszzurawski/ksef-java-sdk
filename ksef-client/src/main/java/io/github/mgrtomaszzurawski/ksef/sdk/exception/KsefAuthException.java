/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown on HTTP 401 (Unauthorized) or 403 (Forbidden).
 * Indicates authentication or authorization failure.
 */
public class KsefAuthException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefAuthException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
