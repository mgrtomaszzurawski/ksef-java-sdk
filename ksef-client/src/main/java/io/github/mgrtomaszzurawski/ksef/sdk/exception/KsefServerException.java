/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on HTTP 5xx server errors.
 *
 * @since 1.0.0
 */
public class KsefServerException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefServerException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
