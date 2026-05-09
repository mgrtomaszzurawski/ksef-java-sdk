/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.limits;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class LimitsClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final int RATE_PER_SECOND = 10;
    private static final int RATE_PER_MINUTE = 100;
    private static final int RATE_PER_HOUR = 1000;
    private static final String CONTEXT_LIMITS_RESPONSE = """
            {
              "onlineSession": {"maxInvoicesPerSession": 100, "maxSessionDuration": 3600},
              "batchSession": {"maxInvoicesPerSession": 10000, "maxSessionDuration": 86400}
            }
            """;

    private static final String SUBJECT_LIMITS_RESPONSE = """
            {}
            """;

    private static final String RATE_LIMITS_RESPONSE = """
            {
              "onlineSession": {"perSecond": %d, "perMinute": %d, "perHour": %d},
              "batchSession": {"perSecond": %d, "perMinute": %d, "perHour": %d}
            }
            """.formatted(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR,
            RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR);

    @Test
    void getContextLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/limits/context"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CONTEXT_LIMITS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ContextLimits response = ksef.limits().getContextLimits();

            // then
            assertNotNull(response.onlineSession());
            assertNotNull(response.batchSession());
        }
    }

    @Test
    void getSubjectLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/limits/subject"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SUBJECT_LIMITS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            SubjectLimits response = ksef.limits().getSubjectLimits();

            // then
            assertNotNull(response);
            verify(getRequestedFor(urlEqualTo("/v2/limits/subject"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void getContextLimits_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/limits/context"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody("{}")));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // then
            var limits = ksef.limits();

            assertThrows(KsefServerException.class, limits::getContextLimits);
        }
    }

    @Test
    void getRateLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/rate-limits"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(RATE_LIMITS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            // when
            ApiRateLimits response = ksef.limits().getRateLimits();

            // then
            assertNotNull(response.onlineSession());
            org.junit.jupiter.api.Assertions.assertEquals(
                    Integer.valueOf(RATE_PER_SECOND), response.onlineSession().perSecond());
            org.junit.jupiter.api.Assertions.assertEquals(
                    Integer.valueOf(RATE_PER_MINUTE), response.onlineSession().perMinute());
            org.junit.jupiter.api.Assertions.assertEquals(
                    Integer.valueOf(RATE_PER_HOUR), response.onlineSession().perHour());
        }
    }

    @Test
    void getRateLimits_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given — both the target endpoint and the reauth security endpoint return 401,
        // so after the SDK retries once on 401 the auth exception propagates.
        stubFor(get(urlEqualTo("/v2/rate-limits"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            // then
            var limits = ksef.limits();

            assertThrows(KsefAuthException.class, limits::getRateLimits);
        }
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture
                .newAuthenticatedClient(wmInfo, TEST_TOKEN, "1234567890");
    }
}
