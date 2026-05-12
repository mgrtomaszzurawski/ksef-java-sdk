/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KsefEnvironmentTest {

    private static final String TEST_URL = "https://api-test.ksef.mf.gov.pl/v2";
    private static final String DEMO_URL = "https://api-demo.ksef.mf.gov.pl/v2";
    private static final String PROD_URL = "https://api.ksef.mf.gov.pl/v2";
    private static final String CUSTOM_URL = "http://localhost:8080/v2";
    private static final String EXPECTED_TEST_AUTH = "https://api-test.ksef.mf.gov.pl/v2/auth/challenge";
    private static final String EXPECTED_DEMO_AUTH = "https://api-demo.ksef.mf.gov.pl/v2/auth/challenge";
    private static final String EXPECTED_PROD_AUTH = "https://api.ksef.mf.gov.pl/v2/auth/challenge";
    private static final String CHALLENGE_PATH = "/challenge";

    @Test
    void test_returnsTestUrl() {
        assertEquals(TEST_URL, KsefEnvironment.TEST.baseUrl());
    }

    @Test
    void demo_returnsDemoUrl() {
        assertEquals(DEMO_URL, KsefEnvironment.DEMO.baseUrl());
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

    @Test
    void custom_whenHttpAndLoopback_succeeds() {
        // localhost / 127.0.0.1 / ::1 are explicit loopback exceptions for WireMock + dev.
        // IPv6 literals must be bracketed per RFC 3986 §3.2.2 so URI.getHost() returns "[::1]".
        assertNotNull(KsefEnvironment.custom("http://localhost:8080/v2"));
        assertNotNull(KsefEnvironment.custom("http://127.0.0.1:8080/v2"));
        assertNotNull(KsefEnvironment.custom("http://[::1]:8080/v2"));
    }

    @Test
    void custom_whenHttpAndNonLoopbackHost_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> KsefEnvironment.custom("http://api.example.com/v2"));
    }

    @Test
    void test_concatWithApiPathsAuth_producesValidChallengeUri() {
        assertEquals(EXPECTED_TEST_AUTH,
                KsefEnvironment.TEST.baseUrl() + ApiPaths.AUTH + CHALLENGE_PATH);
    }

    @Test
    void demo_concatWithApiPathsAuth_producesValidChallengeUri() {
        assertEquals(EXPECTED_DEMO_AUTH,
                KsefEnvironment.DEMO.baseUrl() + ApiPaths.AUTH + CHALLENGE_PATH);
    }

    @Test
    void prod_concatWithApiPathsAuth_producesValidChallengeUri() {
        assertEquals(EXPECTED_PROD_AUTH,
                KsefEnvironment.PROD.baseUrl() + ApiPaths.AUTH + CHALLENGE_PATH);
    }
}
