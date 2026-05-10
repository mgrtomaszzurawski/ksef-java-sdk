/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OfflineModeTest {

    private static final String EXPECTED_WIRE_VALUE = "true";
    private static final int EXPECTED_CONSTANT_COUNT = 3;

    @Test
    void wireValue_whenOffline24_returnsTrue() {
        // then
        assertEquals(EXPECTED_WIRE_VALUE, OfflineMode.OFFLINE_24.wireValue());
    }

    @Test
    void wireValue_whenKsefUnavailability_returnsTrue() {
        // then
        assertEquals(EXPECTED_WIRE_VALUE, OfflineMode.KSEF_UNAVAILABILITY.wireValue());
    }

    @Test
    void wireValue_whenKsefEmergency_returnsTrue() {
        // then
        assertEquals(EXPECTED_WIRE_VALUE, OfflineMode.KSEF_EMERGENCY.wireValue());
    }

    @Test
    void values_whenEnumQueried_exposesThreeConstants() {
        // when
        OfflineMode[] values = OfflineMode.values();

        // then
        assertEquals(EXPECTED_CONSTANT_COUNT, values.length);
        assertNotNull(OfflineMode.valueOf("OFFLINE_24"));
        assertNotNull(OfflineMode.valueOf("KSEF_UNAVAILABILITY"));
        assertNotNull(OfflineMode.valueOf("KSEF_EMERGENCY"));
    }
}
