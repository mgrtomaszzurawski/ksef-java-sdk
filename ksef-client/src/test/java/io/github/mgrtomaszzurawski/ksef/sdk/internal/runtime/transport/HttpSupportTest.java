/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
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
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

/**
 * Tests for HttpSupport using generated model classes from the opened client.model package.
 * Uses AuthenticationChallengeResponseRaw as a representative response type since
 * it is opened to Jackson via module-info.
 */
@WireMockTest
class HttpSupportTest {

    private static final String TEST_PATH = "/test/resource";
    private static final String STUB_PATH = "/v2/test/resource";
    private static final String TEST_CHALLENGE = "20260404-CR-AAAAAAAAAA-BBBBBBBBBB-CC";
    private static final String TEST_TOKEN = "test-token";
    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final String OPERATION_GET = "testGet";
    private static final String OPERATION_POST = "testPost";
    private static final String OPERATION_DELETE = "testDelete";
    private static final String EMPTY_JSON = "{}";
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
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.get(
                TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_GET);

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void get_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody(EMPTY_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefAuthException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_GET));
    }

    @Test
    void get_whenNotFound_throwsNotFoundException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(EMPTY_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefNotFoundException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_GET));
    }

    @Test
    void get_whenTooManyRequests_throwsRateLimitException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS).withBody(EMPTY_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefRateLimitException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_GET));
    }

    @Test
    void get_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody(EMPTY_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_GET));
    }

    @Test
    void getAuthenticated_whenBearerToken_sendsAuthorizationHeader(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.getAuthenticated(
                TEST_PATH, TEST_TOKEN, AuthenticationChallengeResponseRaw.class, OPERATION_GET);

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void postJson_whenOk_returnsDeserializedResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(STUB_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, containing(TestHttpConstants.APPLICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CHALLENGE_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        AuthenticationChallengeResponseRaw response = http.postJson(
                TEST_PATH, EMPTY_JSON, AuthenticationChallengeResponseRaw.class, OPERATION_POST);

        // then
        assertEquals(TEST_CHALLENGE, response.getChallenge());
    }

    @Test
    void deleteAuthenticated_whenNoContent_sendsDeleteWithAuthHeader(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(STUB_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when
        http.deleteAuthenticated(TEST_PATH, TEST_TOKEN, OPERATION_DELETE);

        // then
        verify(deleteRequestedFor(urlEqualTo(STUB_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(BEARER_TOKEN)));
    }

    @Test
    void deleteAuthenticated_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(STUB_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody(EMPTY_JSON)));

        HttpSupport http = createHttpSupport(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> http.deleteAuthenticated(TEST_PATH, TEST_TOKEN, OPERATION_DELETE));
    }

    private static HttpSupport createHttpSupport(WireMockRuntimeInfo wmInfo) {
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        return new HttpSupport(runtime);
    }
}
