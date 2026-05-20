/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when a KSeF session has expired or been terminated.
 * This is a KSeF-specific auth error distinct from generic 401/403.
 *
 * @since 0.1.0
 */
@SuppressWarnings("java:S110") // Exception hierarchy depth is part of the public API contract: extends KsefAuthException so consumers can catch the broader auth category.
public class KsefSessionExpiredException extends KsefAuthException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefSessionExpiredException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
