/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for HttpSupport using generated model classes from the opened client.model package.
 * Uses AuthenticationChallengeResponseRaw as a representative response type since
 * it is opened to Jackson via module-info.
 */
@WireMockTest
class HttpSupportTest {

    private static final String TEST_PATH = "/api/v2/test/resource";
    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_TOO_MANY = 429;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final String TEST_CHALLENGE = "20260404-CR-AAAAAAAAAA-BBBBBBBBBB-CC";
    private static final String CHALLENGE_JSON = """
            {
              "challenge": "%s",
              "timestamp": "2026-04-04T12:00:00+02:00",
              "timestampMs": 1775386800000,
              "clientIp": "192.168.1.1"
            }
            """.formatted(TEST_CHALLENGE);

    @Test
    void get_whenOk_returnsDeserializedResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.get(
                TEST_PATH, AuthenticationChallengeResponseRaw.class, "testGet");

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void get_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefAuthException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, "testGet"));
    }

    @Test
    void get_whenNotFound_throwsNotFoundException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND).withBody("{}")));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefNotFoundException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, "testGet"));
    }

    @Test
    void get_whenTooManyRequests_throwsRateLimitException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(HTTP_TOO_MANY).withBody("{}")));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefRateLimitException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, "testGet"));
    }

    @Test
    void get_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, "testGet"));
    }

    @Test
    void getAuthenticated_whenBearerToken_sendsAuthorizationHeader(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.getAuthenticated(
                TEST_PATH, "test-token", AuthenticationChallengeResponseRaw.class, "testGet");

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void postJson_whenOk_returnsDeserializedResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TEST_PATH))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.postJson(
                TEST_PATH, "{}", AuthenticationChallengeResponseRaw.class, "testPost");

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void deleteAuthenticated_whenNoContent_sendsDeleteWithAuthHeader(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(TEST_PATH))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        http.deleteAuthenticated(TEST_PATH, "test-token", "testDelete");

        // then
        verify(deleteRequestedFor(urlEqualTo(TEST_PATH))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    void deleteAuthenticated_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> http.deleteAuthenticated(TEST_PATH, "test-token", "testDelete"));
    }

    private static HttpSupport createHttpSupport(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        return new HttpSupport(ksef);
    }
}
