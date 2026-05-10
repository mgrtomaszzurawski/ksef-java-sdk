/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import org.jspecify.annotations.Nullable;

/**
 * Common parent for SDK timeout exceptions. Lets consumers catch every
 * timeout flavour with a single {@code catch (KsefTimeoutException ex)}
 * block instead of multi-catch on each concrete subtype.
 *
 * @since 1.0.0
 */
public abstract class KsefTimeoutException extends KsefException {

    private static final long serialVersionUID = 1L;

    protected KsefTimeoutException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
