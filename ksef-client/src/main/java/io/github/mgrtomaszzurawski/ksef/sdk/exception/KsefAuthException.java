/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on HTTP 401 (Unauthorized) or 403 (Forbidden).
 * Indicates authentication or authorization failure.
 *
 * @since 0.1.0
 */
public class KsefAuthException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefAuthException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
