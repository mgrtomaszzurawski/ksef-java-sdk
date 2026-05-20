/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on server-side problems — the umbrella for HTTP 5xx responses,
 * connection failures (DNS / refused / reset), and transport-level
 * timeouts. Consumer remediation is identical across these mechanics:
 * wait and retry (the SDK does so internally; if this exception
 * surfaces, retries already failed).
 *
 * @since 0.1.0
 */
public class KsefServerException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefServerException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }

    /**
     * Construct without HTTP context — used at network/transport layer
     * (connection refused, DNS resolution failure, transport timeout)
     * where there is no response body or status code yet.
     */
    public KsefServerException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
