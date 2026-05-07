/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on encryption or signing failures.
 *
 * @since 1.0.0
 */
public class KsefCryptoException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefCryptoException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
