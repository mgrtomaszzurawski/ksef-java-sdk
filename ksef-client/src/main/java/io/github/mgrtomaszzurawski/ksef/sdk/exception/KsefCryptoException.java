/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown on encryption or signing failures.
 */
public class KsefCryptoException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
