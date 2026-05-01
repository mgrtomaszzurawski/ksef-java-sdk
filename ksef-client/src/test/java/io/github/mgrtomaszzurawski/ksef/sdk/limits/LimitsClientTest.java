/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.limits;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.limits.model.SubjectLimits;
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

@WireMockTest
class LimitsClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";

    private static final int HTTP_OK = 200;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final String CONTEXT_LIMITS_RESPONSE = """
            {
              "onlineSession": {"maxInvoicesPerSession": 100, "maxSessionDuration": 3600},
              "batchSession": {"maxInvoicesPerSession": 10000, "maxSessionDuration": 86400}
            }
            """;

    private static final String SUBJECT_LIMITS_RESPONSE = """
            {}
            """;

    @Test
    void getContextLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/limits/context"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CONTEXT_LIMITS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ContextLimits response = ksef.limits().getContextLimits();

        // then
        assertNotNull(response.onlineSession());
        assertNotNull(response.batchSession());
    }

    @Test
    void getSubjectLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/limits/subject"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUBJECT_LIMITS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        SubjectLimits response = ksef.limits().getSubjectLimits();

        // then
        assertNotNull(response);
        verify(getRequestedFor(urlEqualTo("/api/v2/limits/subject"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void getContextLimits_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/limits/context"))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefServerException.class, () -> ksef.limits().getContextLimits());
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
