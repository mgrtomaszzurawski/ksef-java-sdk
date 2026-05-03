/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown on HTTP 429 (Too Many Requests).
 * The KSeF API returns rate limit information in the response body and a
 * {@code Retry-After} header indicating when the client should retry.
 */
public class KsefRateLimitException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long retryAfterSeconds;

    public KsefRateLimitException(String message, Throwable cause, int statusCode, String responseBody) {
        this(message, cause, statusCode, responseBody, null);
    }

    public KsefRateLimitException(String message, Throwable cause, int statusCode, String responseBody,
                                  Long retryAfterSeconds) {
        super(message, cause, statusCode, responseBody);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Server-suggested wait, in seconds, before retrying. {@code null} when the
     * server did not include a {@code Retry-After} header.
     */
    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
