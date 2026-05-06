/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown on HTTP 404 (Not Found). HTTP 410 (Gone) maps to the more
 * specific {@link KsefRetentionExpiredException} subtype — catching
 * {@link KsefNotFoundException} still handles 410 because the subtype
 * extends this class.
 *
 * @since 1.0.0
 */
public class KsefNotFoundException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefNotFoundException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
