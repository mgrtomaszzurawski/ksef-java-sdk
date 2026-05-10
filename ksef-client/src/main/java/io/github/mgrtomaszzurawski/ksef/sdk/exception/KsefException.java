/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ServerErrorParser;
import java.io.Serial;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Base exception for all KSeF SDK errors.
 * All subclasses are unchecked (extend RuntimeException).
 *
 * @since 1.0.0
 */
public class KsefException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String ERR_UNKNOWN_STATUS = "Unexpected HTTP status: ";
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_GONE = 410;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MIN = 500;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;
    private static final String ERR_KSEF_UNAVAILABLE_503 =
            "KSeF returned 503 Service Unavailable — switch to offline mode (catch KsefUnavailableException)";

    private final int statusCode;
    private final @Nullable String responseBody;

    public KsefException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public KsefException(String message, int statusCode, @Nullable String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public KsefException(String message, @Nullable Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public int statusCode() {
        return statusCode;
    }

    public @Nullable String responseBody() {
        return responseBody;
    }

    /**
     * KSeF-internal error code parsed from {@link #responseBody()} when the
     * server returns a structured error envelope (e.g. {@code 21405} per-field
     * validation, {@code 21001} JSON parsing, {@code 21205} batch empty).
     * Returns {@code null} when the body could not be parsed or carries no code.
     *
     * <p>The base class always returns {@code null} — only
     * {@link KsefValidationException} (HTTP 400) ships a structured error
     * envelope, and overrides this method to surface the parsed code without
     * re-parsing the response body on every call.
     *
     * <p>Consumers can branch on this without parsing
     * {@link #responseBody()} themselves:
     * <pre>{@code
     * if (ex.exceptionCode() != null && ex.exceptionCode() == 21205) { ... }
     * }</pre>
     */
    public @Nullable Integer exceptionCode() {
        return null;
    }

    /**
     * Factory method that maps HTTP status codes to typed exception subclasses.
     */
    public static KsefException of(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        return of(message, cause, statusCode, responseBody, null);
    }

    /**
     * Factory method that maps HTTP status codes to typed exception subclasses,
     * preserving the server-supplied {@code Retry-After} hint on 429 responses.
     */
    public static KsefException of(String message, @Nullable Throwable cause, int statusCode,
                                   @Nullable String responseBody, @Nullable Long retryAfterSeconds) {
        if (statusCode == HTTP_BAD_REQUEST) {
            List<KsefValidationError> errors = ServerErrorParser.parseErrors(responseBody);
            return new KsefValidationException(message, cause, statusCode, responseBody, errors);
        }
        if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
            return new KsefAuthException(message, cause, statusCode, responseBody);
        }
        if (statusCode == HTTP_GONE) {
            return new KsefRetentionExpiredException(message, cause, statusCode, responseBody);
        }
        if (statusCode == HTTP_NOT_FOUND) {
            return new KsefNotFoundException(message, cause, statusCode, responseBody);
        }
        if (statusCode == HTTP_TOO_MANY_REQUESTS) {
            return new KsefRateLimitException(message, cause, statusCode, responseBody, retryAfterSeconds);
        }
        if (statusCode == HTTP_SERVICE_UNAVAILABLE) {
            return new KsefUnavailableException(ERR_KSEF_UNAVAILABLE_503, cause, statusCode, responseBody);
        }
        if (statusCode >= HTTP_SERVER_ERROR_MIN) {
            return new KsefServerException(message, cause, statusCode, responseBody);
        }
        return new KsefException(ERR_UNKNOWN_STATUS + statusCode, cause, statusCode, responseBody);
    }
}
