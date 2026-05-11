/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InvoiceMarshallingPreheatTest {

    @Test
    void preheatAll_completesWithoutException() {
        assertDoesNotThrow(InvoiceMarshallingPreheat::preheatAll);
    }

    @Test
    void preheatAll_isIdempotent_acrossCalls() {
        InvoiceMarshallingPreheat.preheatAll();
        assertDoesNotThrow(InvoiceMarshallingPreheat::preheatAll);
    }
}
