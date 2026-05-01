/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;
/**
 * Thrown on encryption or signing failures.
 */
public class KsefCryptoException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
