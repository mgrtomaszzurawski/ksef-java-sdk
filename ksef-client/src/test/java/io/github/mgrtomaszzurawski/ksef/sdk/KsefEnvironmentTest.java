/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KsefEnvironmentTest {

    private static final String TEST_URL = "https://api-test.ksef.mf.gov.pl/v2";
    private static final String PREPROD_URL = "https://api-preprod.ksef.mf.gov.pl/v2";
    private static final String PROD_URL = "https://api.ksef.mf.gov.pl/v2";
    private static final String CUSTOM_URL = "http://localhost:8080/v2";

    @Test
    void test_returnsTestUrl() {
        assertEquals(TEST_URL, KsefEnvironment.TEST.baseUrl());
    }

    @Test
    void preprod_returnsPreprodUrl() {
        assertEquals(PREPROD_URL, KsefEnvironment.PREPROD.baseUrl());
    }

    @Test
    void prod_returnsProdUrl() {
        assertEquals(PROD_URL, KsefEnvironment.PROD.baseUrl());
    }

    @Test
    void custom_whenValidUrl_returnsCustomEnvironment() {
        // given
        KsefEnvironment custom = KsefEnvironment.custom(CUSTOM_URL);

        // then
        assertNotNull(custom);
        assertEquals(CUSTOM_URL, custom.baseUrl());
    }

    @Test
    void custom_whenNull_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> KsefEnvironment.custom(null));
    }

    @Test
    void custom_whenInvalidScheme_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> KsefEnvironment.custom("ftp://example.com"));
    }

    @Test
    void custom_whenNoScheme_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> KsefEnvironment.custom("example.com"));
    }
}
