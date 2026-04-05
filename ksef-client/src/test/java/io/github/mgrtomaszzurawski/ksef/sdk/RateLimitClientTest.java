/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.ApiRateLimits;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class RateLimitClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";

    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;

    private static final int RATE_PER_SECOND = 10;
    private static final int RATE_PER_MINUTE = 100;
    private static final int RATE_PER_HOUR = 1000;

    private static final String RATE_LIMITS_RESPONSE = """
            {
              "onlineSession": {"perSecond": %d, "perMinute": %d, "perHour": %d},
              "batchSession": {"perSecond": %d, "perMinute": %d, "perHour": %d}
            }
            """.formatted(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR,
            RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR);

    @Test
    void getRateLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/rate-limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RATE_LIMITS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ApiRateLimits response = ksef.rateLimits().getRateLimits();

        // then
        assertNotNull(response.onlineSession());
        assertEquals(Integer.valueOf(RATE_PER_SECOND), response.onlineSession().perSecond());
        assertEquals(Integer.valueOf(RATE_PER_MINUTE), response.onlineSession().perMinute());
        assertEquals(Integer.valueOf(RATE_PER_HOUR), response.onlineSession().perHour());
    }

    @Test
    void getRateLimits_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/rate-limits"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefAuthException.class, () -> ksef.rateLimits().getRateLimits());
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
