/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown on HTTP 429 (Too Many Requests).
 * The KSeF API returns rate limit information in the response body.
 */
public class KsefRateLimitException extends KsefException {

    public KsefRateLimitException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
