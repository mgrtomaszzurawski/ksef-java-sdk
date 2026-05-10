/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.auth;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefRateLimitException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient;

@WireMockTest
class AuthClientTest {

    private static final String AUTH_BASE = "/v2/auth";
    private static final String PATH_CHALLENGE = AUTH_BASE + "/challenge";
    private static final String PATH_XADES = AUTH_BASE + "/xades-signature";
    private static final String KSEF_TOKEN_PATH = AUTH_BASE + "/ksef-token";
    private static final String TEST_KSEF_TOKEN_VALUE = "test-ksef-token-12345";
    private static final String PATH_TOKEN_REDEEM = AUTH_BASE + "/token/redeem";
    private static final String PATH_TOKEN_REFRESH = AUTH_BASE + "/token/refresh";
    private static final String PATH_SESSIONS = AUTH_BASE + "/sessions";
    private static final String PATH_SESSIONS_CURRENT = AUTH_BASE + "/sessions/current";
    private static final String TEST_CHALLENGE = "20260404-CR-AAAAAAAAAA-BBBBBBBBBB-CC";
    private static final String TEST_REFERENCE_NUMBER = "20260404-AU-1111111111-ABCDEF1234-01";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test-payload.signature";
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.access-token.signature";
    private static final String TEST_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh-token.signature";
    private static final String TEST_NIP = "1111111111";
    private static final String TEST_TOKEN_EXPIRY = "2026-04-04T14:00:00+02:00";
    /** Test client IP — matches CHALLENGE_RESPONSE body and newChallenge() helper. */
    private static final String TEST_CLIENT_IP = "192.168.1.1";
    /** Wire-encoded value for {@code KsefIdentifier.Type.NIP} in the auth request body. */
    private static final String TYPE_NIP = "Nip";

    private static final String CHALLENGE_RESPONSE = """
            {
              "challenge": "%s",
              "timestamp": "2026-04-04T12:00:00+02:00",
              "timestampMs": 1775386800000,
              "clientIp": "%s"
            }
            """.formatted(TEST_CHALLENGE, TEST_CLIENT_IP);

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
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CHALLENGE_RESPONSE)));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);

        // when
        AuthenticationChallenge response = new AuthClient(runtime).requestChallenge();

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
                        .withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withBody("{\"error\":\"Rate limit exceeded\"}")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        AuthClient auth = new AuthClient(runtime);

        // then
        assertThrows(KsefRateLimitException.class, () -> auth.requestChallenge());
    }

    @Test
    void authenticateWithXades_whenValidSignature_activatesSession(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, containing(TestHttpConstants.APPLICATION_XML))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_INIT_RESPONSE)));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();

        // when
        AuthenticationInit response = new AuthClient(runtime).authenticateWithXades(
                TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        // then
        assertEquals(TEST_REFERENCE_NUMBER, response.referenceNumber());
        assertEquals(TEST_TOKEN, response.authenticationToken().token());
        assertTrue(runtime.sessionContext().isActive());
        assertEquals(TEST_TOKEN, runtime.sessionContext().token());
    }

    @Test
    void redeemTokens_whenAuthenticated_updatesSessionWithAccessToken(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — authenticate first
        stubXadesAuth();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        new AuthClient(runtime).authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        stubFor(post(urlEqualTo(PATH_TOKEN_REDEEM))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(TOKENS_RESPONSE)));

        // when
        AuthenticationTokens response = new AuthClient(runtime).redeemTokens();

        // then
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken().token());
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken().token());
        assertEquals(TEST_ACCESS_TOKEN, runtime.sessionContext().token());
    }

    @Test
    void refreshToken_whenValidRefreshToken_updatesSessionToken(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — authenticate and redeem first
        stubXadesAuth();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        new AuthClient(runtime).authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);

        stubFor(post(urlEqualTo(PATH_TOKEN_REFRESH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_REFRESH_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(REFRESH_RESPONSE)));

        // when
        AuthenticationTokenRefresh response = new AuthClient(runtime).refreshToken(TEST_REFRESH_TOKEN);

        // then
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken().token());
        assertEquals(TEST_ACCESS_TOKEN, runtime.sessionContext().token());
    }

    @Test
    void getStatus_whenAuthenticated_returnsOperationStatus(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        authenticateRuntime(runtime);

        stubFor(get(urlEqualTo(AUTH_BASE + "/" + TEST_REFERENCE_NUMBER))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_RESPONSE)));

        // when
        AuthenticationStatus response = new AuthClient(runtime).getStatus(TEST_REFERENCE_NUMBER);

        // then
        assertNotNull(response.status());
    }

    @Test
    void listSessions_whenAuthenticated_returnsSessionList(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        authenticateRuntime(runtime);

        stubFor(get(urlEqualTo(PATH_SESSIONS))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSIONS_LIST_RESPONSE)));

        // when
        AuthenticationList response = new AuthClient(runtime).listSessions();

        // then
        assertNotNull(response.items());
        assertFalse(response.items().isEmpty());
    }

    @Test
    void terminateCurrentSession_whenAuthenticated_clearsSessionContext(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubXadesAuth();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        authenticateRuntime(runtime);
        assertTrue(runtime.sessionContext().isActive());

        stubFor(delete(urlEqualTo(PATH_SESSIONS_CURRENT))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        // when
        new AuthClient(runtime).terminateCurrentSession();

        // then
        assertFalse(runtime.sessionContext().isActive());
    }

    @Test
    void authenticateWithXades_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)
                        .withBody("{\"error\":\"Invalid certificate\"}")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        AuthClient auth = new AuthClient(runtime);
        var cert = testCerts.certificate();
        var privateKey = testCerts.privateKey();

        // then
        assertThrows(KsefAuthException.class,
                () -> auth.authenticateWithXades(TEST_CHALLENGE, cert, privateKey, TEST_NIP));
    }

    @Test
    void authenticateWithXades_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withBody("{\"error\":\"Internal error\"}")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        AuthClient auth = new AuthClient(runtime);
        var cert = testCerts.certificate();
        var privateKey = testCerts.privateKey();

        // then
        assertThrows(KsefServerException.class,
                () -> auth.authenticateWithXades(TEST_CHALLENGE, cert, privateKey, TEST_NIP));
    }

    @Test
    void authenticateWithToken_whenNipString_delegatesToFiveArgFormAndActivatesSession(WireMockRuntimeInfo wmInfo) throws Exception {
        // Codex coverage gap — the (challenge, token, nipString, publicKey)
        // convenience overload must wrap the NIP into KsefIdentifier and
        // delegate to the five-arg form so the session activates exactly
        // like the KsefIdentifier-typed overload.
        stubFor(post(urlEqualTo(KSEF_TOKEN_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_INIT_RESPONSE)));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        AuthenticationChallenge challenge = newChallenge();

        AuthenticationInit response = new AuthClient(runtime).authenticateWithToken(
                challenge, TEST_KSEF_TOKEN_VALUE, TEST_NIP, testCerts.certificate().getPublicKey());

        assertEquals(TEST_REFERENCE_NUMBER, response.referenceNumber());
        assertTrue(runtime.sessionContext().isActive());
        // Wire-shape pin: encrypted token + Nip context identifier must reach the server.
        verify(postRequestedFor(urlEqualTo(KSEF_TOKEN_PATH))
                .withRequestBody(matchingJsonPath("$.challenge", equalTo(TEST_CHALLENGE)))
                .withRequestBody(matchingJsonPath("$.encryptedToken"))
                .withRequestBody(matchingJsonPath("$.contextIdentifier.type", equalTo(TYPE_NIP)))
                .withRequestBody(matchingJsonPath("$.contextIdentifier.value", equalTo(TEST_NIP))));
    }

    @Test
    void authenticateWithToken_whenKsefIdentifierWithoutPolicy_activatesSession(WireMockRuntimeInfo wmInfo) throws Exception {
        // The (challenge, token, KsefIdentifier, publicKey) overload (no
        // AuthorizationPolicy) is the typed equivalent of the String-NIP
        // convenience and must also delegate to the five-arg form.
        stubFor(post(urlEqualTo(KSEF_TOKEN_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_INIT_RESPONSE)));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        TestCertificates testCerts = TestCertificates.generateRsa();
        AuthenticationChallenge challenge = newChallenge();

        AuthenticationInit response = new AuthClient(runtime).authenticateWithToken(
                challenge, TEST_KSEF_TOKEN_VALUE,
                KsefIdentifier.nip(TEST_NIP),
                testCerts.certificate().getPublicKey());

        assertEquals(TEST_REFERENCE_NUMBER, response.referenceNumber());
        assertTrue(runtime.sessionContext().isActive());
        verify(postRequestedFor(urlEqualTo(KSEF_TOKEN_PATH))
                .withRequestBody(matchingJsonPath("$.challenge", equalTo(TEST_CHALLENGE)))
                .withRequestBody(matchingJsonPath("$.encryptedToken"))
                .withRequestBody(matchingJsonPath("$.contextIdentifier.type", equalTo(TYPE_NIP)))
                .withRequestBody(matchingJsonPath("$.contextIdentifier.value", equalTo(TEST_NIP))));
    }

    /**
     * Build a deterministic {@link AuthenticationChallenge} for the
     * token-auth tests. Both the {@code timestamp} and
     * {@code timestampMs} fields are derived from a single fixed
     * {@link Instant} so a JIT pause between accessors cannot drift them
     * apart and surface as a flake on the encryption-window check.
     */
    private static AuthenticationChallenge newChallenge() {
        Instant fixedInstant = Instant.now();
        return new AuthenticationChallenge(
                TEST_CHALLENGE,
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                fixedInstant.toEpochMilli(),
                TEST_CLIENT_IP);
    }

    private static void stubXadesAuth() {
        stubFor(post(urlEqualTo(PATH_XADES))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_INIT_RESPONSE)));
    }

    private static void authenticateRuntime(HttpRuntime runtime) throws Exception {
        TestCertificates testCerts = TestCertificates.generateRsa();
        new AuthClient(runtime).authenticateWithXades(TEST_CHALLENGE, testCerts.certificate(), testCerts.privateKey(), TEST_NIP);
    }
}
