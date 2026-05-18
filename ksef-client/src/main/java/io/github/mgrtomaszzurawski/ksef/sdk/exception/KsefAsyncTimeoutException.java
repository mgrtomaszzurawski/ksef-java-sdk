/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

/**
 * Thrown by the SDK's internal async-polling helper when the configured
 * timeout elapses before the polled operation status reaches a terminal
 * state. Surfaced verbatim from synchronous operations that internally
 * poll (e.g. {@code Permissions.grant*} / {@code revoke*},
 * {@code Certificates.requestNewCertificate}) after their per-call
 * {@code Duration} budget is exhausted.
 *
 * <p>Lives in {@code sdk.exception} (an exported JPMS package) so modular
 * consumers can import it directly.
 *
 * @since 1.0.0
 */
@SuppressWarnings("java:S110")
public class KsefAsyncTimeoutException extends KsefTimeoutException {

    private static final long serialVersionUID = 1L;

    public KsefAsyncTimeoutException(String message) {
        super(message, null);
    }
}
