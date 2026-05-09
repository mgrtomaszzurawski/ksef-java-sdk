/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.auth;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

/**
 * Tests for automatic token refresh and retry on HTTP 401.
 *
 * <p>When an authenticated request returns 401 (token expired), the SDK should:
 * <ol>
 *   <li>Reset its internal {@code authenticated} state</li>
 *   <li>Re-run the full auth flow (challenge → ksef-token → poll → redeem)</li>
 *   <li>Retry the original request once with the fresh access token</li>
 *   <li>If the retry also returns 401, propagate {@link KsefAuthException}</li>
 * </ol>
 */
@WireMockTest
class AuthAutoRefreshTest {

    private static final String TARGET_PATH = "/v2/rate-limits";
    private static final String PATH_SECURITY = "/v2/security/public-key-certificates";
    private static final String PATH_CHALLENGE = "/v2/auth/challenge";
    private static final String PATH_KSEF_TOKEN = "/v2/auth/ksef-token";
    private static final String PATH_AUTH_STATUS_PATTERN = "/v2/auth/[A-Za-z0-9.\\-]+";
    private static final String PATH_TOKEN_REDEEM = "/v2/auth/token/redeem";
    private static final String NIP = "1234567890";
    private static final String INITIAL_TOKEN = "stale-jwt-from-prior-session";
    private static final String FRESH_TOKEN = "fresh-jwt-after-reauth";
    private static final String OPERATION_TOKEN = "operation-token-from-init";
    private static final String SESSION_REF = "20260404-AU-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP_SHORT = "test";
    private static final String TOKEN_EXPIRY = "2026-12-31T23:59:59+01:00";
    private static final String STORED_REFRESH_TOKEN = "previously-stored-refresh-token";
    private static final String PATH_TOKEN_REFRESH = "/v2/auth/token/refresh";
    private static final String REFRESHED_ACCESS_TOKEN = "access-token-from-refresh-endpoint";

    private static final String TOKEN_REFRESH_RESPONSE = """
            {
              "accessToken": {"token": "%s", "validUntil": "%s"}
            }
            """.formatted(REFRESHED_ACCESS_TOKEN, TOKEN_EXPIRY);

    private static final String CHALLENGE_RESPONSE = """
            {
              "challenge": "20260430-CR-AAAAAAAAAA-BBBBBBBBBB-CC",
              "timestamp": "2026-04-30T12:00:00+02:00",
              "timestampMs": 1777032000000,
              "clientIp": "127.0.0.1"
            }
            """;

    private static final String AUTH_INIT_RESPONSE = """
            {
              "referenceNumber": "%s",
              "authenticationToken": {
                "token": "%s",
                "validUntil": "%s"
              }
            }
            """.formatted(SESSION_REF, OPERATION_TOKEN, TOKEN_EXPIRY);

    private static final String AUTH_STATUS_RESPONSE = """
            {
              "startDate": "2026-04-30T12:00:00+02:00",
              "authenticationMethod": "Token",
              "status": {"code": 200, "description": "Active"}
            }
            """;

    private static final String TOKENS_REDEEM_RESPONSE = """
            {
              "accessToken": {"token": "%s", "validUntil": "%s"},
              "refreshToken": {"token": "refresh-token", "validUntil": "%s"}
            }
            """.formatted(FRESH_TOKEN, TOKEN_EXPIRY, TOKEN_EXPIRY);

    private static final String RATE_LIMITS_OK_BODY = """
            {
              "onlineSession": {"perSecond": 10, "perMinute": 100, "perHour": 1000},
              "batchSession": {"perSecond": 10, "perMinute": 100, "perHour": 1000}
            }
            """;

    @Test
    void request_when401_reauthenticatesAndRetries(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — first call returns 401 with the stale token, second call returns 200 with the fresh token
        stubFor(get(urlEqualTo(TARGET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + INITIAL_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo(TARGET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + FRESH_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(RATE_LIMITS_OK_BODY)));
        try (KsefClient ksef = createClientWithStaleSession(wmInfo)) {
            stubAuthFlowSuccess();

            // when
            ksef.limits().getRateLimits();

            // then — both attempts hit the target, reauth flow ran exactly once
            verify(2, getRequestedFor(urlEqualTo(TARGET_PATH)));
            verify(1, postRequestedFor(urlEqualTo(PATH_CHALLENGE)));
            verify(1, postRequestedFor(urlEqualTo(PATH_KSEF_TOKEN)));
            verify(1, postRequestedFor(urlEqualTo(PATH_TOKEN_REDEEM)));
        }
    }

    @Test
    void request_whenSecond401_throwsAuthException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — both attempts return 401, reauth itself succeeds, exception propagates
        stubFor(get(urlEqualTo(TARGET_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{\"error\":\"expired\"}")));
        try (KsefClient ksef = createClientWithStaleSession(wmInfo)) {
            stubAuthFlowSuccess();

            // then
            var rateLimits = ksef.limits();

            assertThrows(KsefAuthException.class, () -> rateLimits.getRateLimits());
            verify(2, getRequestedFor(urlEqualTo(TARGET_PATH)));
            verify(1, postRequestedFor(urlEqualTo(PATH_CHALLENGE)));
        }
    }

    @Test
    void request_when200_doesNotReauthenticate(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — happy path: target returns 200 immediately, no reauth should happen
        stubFor(get(urlEqualTo(TARGET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + INITIAL_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(RATE_LIMITS_OK_BODY)));
        // Stub auth flow too — it must NOT be called
        try (KsefClient ksef = createClientWithStaleSession(wmInfo)) {
            stubAuthFlowSuccess();

            // when
            ksef.limits().getRateLimits();

            // then — exactly one request, no reauth
            verify(1, getRequestedFor(urlEqualTo(TARGET_PATH)));
            verify(0, postRequestedFor(urlEqualTo(PATH_CHALLENGE)));
            verify(0, postRequestedFor(urlEqualTo(PATH_KSEF_TOKEN)));
            verify(0, postRequestedFor(urlEqualTo(PATH_TOKEN_REDEEM)));
        }
    }

    @Test
    void request_when401WithStoredRefreshToken_callsRefreshEndpointAndSkipsFullAuth(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — first call 401, refresh endpoint succeeds, retry uses refreshed token
        stubFor(get(urlEqualTo(TARGET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + INITIAL_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo(TARGET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + REFRESHED_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(RATE_LIMITS_OK_BODY)));
        stubFor(post(urlEqualTo(PATH_TOKEN_REFRESH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(TOKEN_REFRESH_RESPONSE)));
        // Stub challenge too — must NOT be called
        try (KsefClient ksef = createClientWithStaleSessionAndRefreshToken(wmInfo)) {
            stubAuthFlowSuccess();

            // when
            ksef.limits().getRateLimits();

            // then — refresh endpoint was used, full challenge flow was NOT
            verify(2, getRequestedFor(urlEqualTo(TARGET_PATH)));
            verify(1, postRequestedFor(urlEqualTo(PATH_TOKEN_REFRESH)));
            verify(0, postRequestedFor(urlEqualTo(PATH_CHALLENGE)));
            verify(0, postRequestedFor(urlEqualTo(PATH_KSEF_TOKEN)));
        }
    }

    /**
     * Stubs the full token-based auth flow with valid responses so {@code reauthenticate()}
     * succeeds and yields the {@link #FRESH_TOKEN} access token.
     *
     * <p>Includes the security public-key endpoint (returns a self-signed RSA cert),
     * challenge, ksef-token init, status poll, and redeem.
     */
    private static void stubAuthFlowSuccess() throws Exception {
        TestCertificates rsaCertificate = TestCertificates.generateRsa();
        String certBase64 = Base64.getEncoder().encodeToString(rsaCertificate.certificate().getEncoded());

        String publicKeysResponse = """
                [
                  {
                    "certificate": "%s",
                    "validFrom": "2026-01-01T00:00:00+01:00",
                    "validTo": "2099-01-01T00:00:00+01:00",
                    "usage": ["KsefTokenEncryption", "SymmetricKeyEncryption"]
                  }
                ]
                """.formatted(certBase64);

        stubFor(get(urlEqualTo(PATH_SECURITY))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(publicKeysResponse)));

        stubFor(post(urlEqualTo(PATH_CHALLENGE))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(CHALLENGE_RESPONSE)));

        stubFor(post(urlEqualTo(PATH_KSEF_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_INIT_RESPONSE)));

        stubFor(get(urlPathMatching(PATH_AUTH_STATUS_PATTERN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(AUTH_STATUS_RESPONSE)));

        stubFor(post(urlEqualTo(PATH_TOKEN_REDEEM))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(TOKENS_REDEEM_RESPONSE)));
    }

    /**
     * Build a {@link KsefClient} with a session pre-activated using {@link #INITIAL_TOKEN}.
     * Simulates a process that holds a stale JWT — when the next request returns 401,
     * the SDK re-authenticates and retries with a fresh token.
     */
    private static KsefClient createClientWithStaleSession(WireMockRuntimeInfo wmInfo) {
        // Drive lazy auth via the fixture so the SDK ends up holding
        // INITIAL_TOKEN as its access token. After setup we reset WireMock
        // request counters so the test body's verify() only counts
        // requests made AFTER setup. Test scenarios that override auth
        // stubs (stubAuthFlowSuccess returning FRESH_TOKEN) take effect
        // for the SECOND auth call (after a 401-driven reauth).
        KsefClient ksef = io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture
                .newAuthenticatedClient(wmInfo, INITIAL_TOKEN, NIP);
        forceAuthenticate(ksef);
        com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests();
        return ksef;
    }

    /**
     * Drive lazy auth at a precise point in the test setup. The
     * {@code authenticate()} method on {@link KsefClient} is not
     * exposed publicly (lazy auth is the documented API); reflection
     * is the test seam for forcing the flow before the test body
     * checks state.
     */
    private static void forceAuthenticate(KsefClient ksef) {
        try {
            java.lang.reflect.Method method = KsefClient.class.getDeclaredMethod("authenticate");
            method.setAccessible(true);
            method.invoke(ksef);
        } catch (ReflectiveOperationException reflectiveFailure) {
            throw new IllegalStateException("Test could not force lazy auth", reflectiveFailure);
        }
    }

    private static KsefClient createClientWithStaleSessionAndRefreshToken(WireMockRuntimeInfo wmInfo) {
        // The fixture's redeem stub already returns a refresh token;
        // override it before driving auth so the stored refresh token
        // matches STORED_REFRESH_TOKEN. We re-stub the redeem endpoint
        // (last-write-wins in WireMock) before any KsefClient call
        // triggers auth.
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                        com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo(PATH_TOKEN_REDEEM))
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                .withStatus(TestHttpConstants.HTTP_OK)
                                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                                .withBody("""
                                        {
                                          "accessToken": {"token": "%s", "validUntil": "2030-01-01T00:00:00Z"},
                                          "refreshToken": {"token": "%s", "validUntil": "2030-01-01T00:00:00Z"}
                                        }
                                        """.formatted(INITIAL_TOKEN, STORED_REFRESH_TOKEN))));
        return createClientWithStaleSession(wmInfo);
    }
}
