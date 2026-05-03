/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime;

/**
 * Masks personally-identifiable identifier values for logging.
 * <p>Polish NIP/PESEL/internal IDs are PII — DEBUG logs that surface them
 * verbatim leak data to log aggregators. The masking pattern keeps the last
 * four characters so operators can still correlate sessions.
 */
public final class IdentifierMasking {

    private static final int VISIBLE_TAIL_LENGTH = 4;
    private static final String MASK_PREFIX = "***";

    private IdentifierMasking() {
    }

    /**
     * Mask the leading characters of {@code value}, keeping the last
     * {@value #VISIBLE_TAIL_LENGTH}. Returns {@code "***"} for null/empty
     * input and {@code "***" + value} when the value is shorter than the
     * tail length.
     */
    public static String maskTail(String value) {
        if (value == null || value.isEmpty()) {
            return MASK_PREFIX;
        }
        int from = Math.max(0, value.length() - VISIBLE_TAIL_LENGTH);
        return MASK_PREFIX + value.substring(from);
    }
}
