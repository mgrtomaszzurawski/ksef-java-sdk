/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on HTTP 404 (Not Found). HTTP 410 (Gone) maps to the more
 * specific {@link KsefRetentionExpiredException} subtype — catching
 * {@link KsefNotFoundException} still handles 410 because the subtype
 * extends this class.
 *
 * @since 0.1.0
 */
public class KsefNotFoundException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefNotFoundException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
