/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.auth;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefTokenCredentials;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationTokens;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class AuthClientTest {

    private static final String PATH_CHALLENGE = "/api/v2/auth/challenge";
    private static final String PATH_XADES = "/api/v2/auth/xades-signature";
    private static final String PATH_TOKEN_REDEEM = "/api/v2/auth/token/redeem";
    private static final String PATH_TOKEN_REFRESH = "/api/v2/auth/token/refresh";
    private static final String PATH_SESSIONS = "/api/v2/auth/sessions";
    private static final String PATH_SESSIONS_CURRENT = "/api/v2/auth/sessions/current";

    private static final int HTTP_OK = 200;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_TOO_MANY = 429;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final String TEST_CHALLENGE = "20260404-CR-AAAAAAAAAA-BBBBBBBBBB-CC";
    private static final String TEST_REFERENCE_NUMBER = "20260404-AU-1234567890-ABCDEF1234-01";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test-payload.signature";
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.access-token.signature";
    private static final String TEST_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh-token.signature";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_TOKEN_EXPIRY = "2026-04-04T14:00:00+02:00";

    private static final String CHALLENGE_RESPONSE = """
            {
              "challenge": "%s",
              "timestamp": "2026-04-04T12:00:00+02:00",
              "timestampMs": 1775386800000,
              "clientIp": "192.168.1.1"
            }
            """.formatted(TEST_CHALLENGE);

    private static final String AUTH_INIT_RESPONSE = """
            {
              "referenceNumber": "%s",
              "authenticationToken": {
                "token": "%s",
                "validUntil": "%s"
              }
            }
            """.formatted(TEST_REFERENCE_NUMBER, TEST_TOKEN, TEST_TOKEN_EXPIRY);

    private static final String TOKENS_RESPONSE = """
            {
              "accessToken": {
                "token": "%s",
                "validUntil": "%s"
              },
              "refreshToken": {
                "token": "%s",
                "validUntil": "2026-04-04T16:00:00+02:00"
              }
            }
            """.formatted(TEST_ACCESS_TOKEN, TEST_TOKEN_EXPIRY, TEST_REFRESH_TOKEN);

    private static final String REFRESH_RESPONSE = """
            {
              "accessToken": {
                "token": "%s",
                "validUntil": "%s"
              }
            }
            """.formatted(TEST_ACCESS_TOKEN, TEST_TOKEN_EXPIRY);

    private static final String STATUS_RESPONSE = """
            {
              "startDate": "2026-04-04T12:00:00+02:00",
              "authenticationMethod": "QualifiedSignature",
              "status": {
                "code": 200,
                "description": "Active"
              }
            }
            """;

    private static final String SESSIONS_LIST_RESPONSE = """
            {
              "items": [
                {
                  "referenceNumber": "%s",
                  "isCurrent": true,
                  "startDate": "2026-04-04T12:00:00+02:00"
                }
              ]
            }
            """.formatted(TEST_REFERENCE_NUMBER);

    @Test
    void requestChallenge_whenServerResponds_returnsChallengeWithTimestamp(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(PATH_CHALLENGE))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CHALLENGE_RESPONSE)));

        KsefClient ksef = createClient(wmInfo);

        // when
        AuthenticationChallenge response = ksef.auth().requestChallenge();

        // then
        assertEquals(TEST_CHALLENGE, response.challenge());
        assertNotNull(response.timestamp());
        assertNotNull(response.clientIp());
    }

    @Test
    void requestChallenge_whenRateLimited_throwsRateLimitException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(PATH_CHALLENGE))
                .willReturn(aResponse()
                        .withStatus(HTTP_TOO_MANY)
                        .withBody("{\"error\":\"Rate limit exceeded\"}")));

        KsefClient ksef = createClient(wmInfo);

        // then
        assertThrows(KsefRateLimitException.class, () -> ksef.auth().requestChallenge());
    }

    @Test
    void authenticateWithXades_whenValidSignature_activatesSession(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .withHeader("Content-Type", containing("application/xml"))
                .willReturn(aResponse()
                        .withStatus(HTTP_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(AUTH_INIT_RESPONSE)));

        KsefClient ksef = createClient(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();

        // when
        AuthenticationInit response = ksef.auth().authenticateWithXades(
                TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        // then
        assertEquals(TEST_REFERENCE_NUMBER, response.referenceNumber());
        assertEquals(TEST_TOKEN, response.authenticationToken().token());
        assertTrue(ksef.sessionContext().isActive());
        assertEquals(TEST_TOKEN, ksef.sessionContext().token());
    }

    @Test
    void redeemTokens_whenAuthenticated_updatesSessionWithAccessToken(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — authenticate first
        stubXadesAuth(wmInfo);
        KsefClient ksef = createClient(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        ksef.auth().authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        stubFor(post(urlEqualTo(PATH_TOKEN_REDEEM))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TOKENS_RESPONSE)));

        // when
        AuthenticationTokens response = ksef.auth().redeemTokens();

        // then
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken().token());
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken().token());
        assertEquals(TEST_ACCESS_TOKEN, ksef.sessionContext().token());
    }

    @Test
    void refreshToken_whenValidRefreshToken_updatesSessionToken(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — authenticate and redeem first
        stubXadesAuth(wmInfo);
        KsefClient ksef = createClient(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        ksef.auth().authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        stubFor(post(urlEqualTo(PATH_TOKEN_REFRESH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_REFRESH_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(REFRESH_RESPONSE)));

        // when
        AuthenticationTokenRefresh response = ksef.auth().refreshToken(TEST_REFRESH_TOKEN);

        // then
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken().token());
        assertEquals(TEST_ACCESS_TOKEN, ksef.sessionContext().token());
    }

    @Test
    void getStatus_whenAuthenticated_returnsOperationStatus(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth(wmInfo);
        KsefClient ksef = createClient(wmInfo);
        authenticateClient(ksef);

        stubFor(get(urlEqualTo("/api/v2/auth/" + TEST_REFERENCE_NUMBER))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STATUS_RESPONSE)));

        // when
        AuthenticationStatus response = ksef.auth().getStatus(TEST_REFERENCE_NUMBER);

        // then
        assertNotNull(response.status());
    }

    @Test
    void listSessions_whenAuthenticated_returnsSessionList(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth(wmInfo);
        KsefClient ksef = createClient(wmInfo);
        authenticateClient(ksef);

        stubFor(get(urlEqualTo(PATH_SESSIONS))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SESSIONS_LIST_RESPONSE)));

        // when
        AuthenticationList response = ksef.auth().listSessions();

        // then
        assertNotNull(response.items());
        assertFalse(response.items().isEmpty());
    }

    @Test
    void terminateCurrentSession_whenAuthenticated_clearsSessionContext(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth(wmInfo);
        KsefClient ksef = createClient(wmInfo);
        authenticateClient(ksef);
        assertTrue(ksef.sessionContext().isActive());

        stubFor(delete(urlEqualTo(PATH_SESSIONS_CURRENT))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        // when
        ksef.auth().terminateCurrentSession();

        // then
        assertFalse(ksef.sessionContext().isActive());
    }

    @Test
    void authenticateWithXades_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(HTTP_UNAUTHORIZED)
                        .withBody("{\"error\":\"Invalid certificate\"}")));

        KsefClient ksef = createClient(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();

        // then
        assertThrows(KsefAuthException.class,
                () -> ksef.auth().authenticateWithXades(
                        TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP));
    }

    @Test
    void authenticateWithXades_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(HTTP_SERVER_ERROR)
                        .withBody("{\"error\":\"Internal error\"}")));

        KsefClient ksef = createClient(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();

        // then
        assertThrows(KsefServerException.class,
                () -> ksef.auth().authenticateWithXades(
                        TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP));
    }

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }

    private static void stubXadesAuth(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(HTTP_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(AUTH_INIT_RESPONSE)));
    }

    private static void authenticateClient(KsefClient ksef) throws Exception {
        TestCertificates testCerts = TestCertificates.generateRsa();
        ksef.auth().authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);
    }
}
