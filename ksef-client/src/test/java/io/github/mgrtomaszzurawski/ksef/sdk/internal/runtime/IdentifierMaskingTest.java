/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentifierMaskingTest {

    private static final String NIP_VALUE = "1234567890";
    private static final String NIP_MASKED = "***7890";
    private static final String SHORT_VALUE = "ab";
    private static final String SHORT_MASKED = "***ab";
    private static final String EMPTY_MASKED = "***";

    @Test
    void maskTail_whenStandardNip_keepsLastFourCharacters() {
        assertEquals(NIP_MASKED, IdentifierMasking.maskTail(NIP_VALUE));
    }

    @Test
    void maskTail_whenShorterThanTailWindow_keepsEntireValue() {
        assertEquals(SHORT_MASKED, IdentifierMasking.maskTail(SHORT_VALUE));
    }

    @Test
    void maskTail_whenNull_returnsBareMask() {
        assertEquals(EMPTY_MASKED, IdentifierMasking.maskTail(null));
    }

    @Test
    void maskTail_whenEmpty_returnsBareMask() {
        assertEquals(EMPTY_MASKED, IdentifierMasking.maskTail(""));
    }
}
