/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when KSeF is unavailable — the server returned HTTP 503
 * (Service Unavailable) or the underlying HTTP transport failed with a
 * connection-level error indicating KSeF is unreachable
 * (e.g. {@code IOException} / {@code ConnectException} during socket
 * connect or TLS handshake).
 *
 * <p>This is the signal consumers must catch to enter offline mode per
 * {@code ksef-docs/offline/awaria-i-niedostepnosc.md}: stop attempting
 * online submission, fall back to local issuance with KOD II, and
 * upload the accumulated offline invoices once KSeF reports back
 * available.
 *
 * <p>Distinguished from {@link KsefServerException} (HTTP 5xx other
 * than 503) and {@link KsefNetworkException} (low-level transport
 * failure) so consumers can branch on {@code instanceof
 * KsefUnavailableException} for offline-fallback policy without
 * sweeping every 5xx into offline mode.
 *
 * @since 1.0.0
 */
public class KsefUnavailableException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** HTTP status code carried when the cause is a 503 response (used for {@link #statusCode()}). */
    public static final int STATUS_SERVICE_UNAVAILABLE = 503;

    public KsefUnavailableException(String message, @Nullable Throwable cause,
                                    int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }

    public KsefUnavailableException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
