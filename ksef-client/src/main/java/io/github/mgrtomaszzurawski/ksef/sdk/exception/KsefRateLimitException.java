/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on HTTP 429 (Too Many Requests).
 * The KSeF API returns rate limit information in the response body and a
 * {@code Retry-After} header indicating when the client should retry.
 *
 * @since 1.0.0
 */
public class KsefRateLimitException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable Long retryAfterSeconds;

    public KsefRateLimitException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        this(message, cause, statusCode, responseBody, null);
    }

    public KsefRateLimitException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody,
                                  @Nullable Long retryAfterSeconds) {
        super(message, cause, statusCode, responseBody);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Server-suggested wait, in seconds, before retrying. {@code null} when the
     * server did not include a {@code Retry-After} header.
     */
    public @Nullable Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
