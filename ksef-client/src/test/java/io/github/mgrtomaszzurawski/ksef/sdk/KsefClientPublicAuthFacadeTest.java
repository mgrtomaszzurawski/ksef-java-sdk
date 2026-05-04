/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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

/**
 * Public-facade WireMock tests for the new auth methods added in the
 * F5 closure (Codex F8 — these were previously exercised only by demo
 * runners, not automated tests).
 *
 * <p>Covers:
 * <ul>
 *   <li>{@link KsefClient#listAuthSessions()} — maps the
 *       {@code /v2/auth/sessions} body into {@link AuthSession} records.</li>
 *   <li>{@link KsefClient#terminateAuthSession(String)} — DELETE on
 *       {@code /v2/auth/sessions/{referenceNumber}}.</li>
 *   <li>{@link KsefClient#refreshAuthToken()} — POST on
 *       {@code /v2/auth/token/refresh}, throws when no refresh token
 *       is held.</li>
 *   <li>{@link KsefClient#publicKeyCertificates()} — returns the
 *       cached {@code /v2/security/public-key-certificates} payload.</li>
 *   <li>{@link KsefClient#bearerToken()} — returns the access token
 *       captured during authenticate().</li>
 * </ul>
 */
@WireMockTest
class KsefClientPublicAuthFacadeTest {

    private static final String AUTH_BASE = "/v2/auth";
    private static final String AUTH_SESSIONS = AUTH_BASE + "/sessions";
    private static final String AUTH_TOKEN_REFRESH = AUTH_BASE + "/token/refresh";
    private static final String SECURITY_KEYS = "/v2/security/public-key-certificates";
    private static final String SESSIONS_LIST_RESPONSE = """
            {
              "items": [
                {
                  "referenceNumber": "20260418-AU-1234567890-AAAAAAAAAA-01",
                  "isCurrent": true,
                  "tokenRedeemed": true,
                  "startDate": "2026-04-18T12:00:00+02:00",
                  "lastTokenRefreshDate": "2026-04-18T13:00:00+02:00",
                  "refreshTokenValidUntil": "2026-04-25T13:00:00+02:00",
                  "status": {"code": 200, "description": "Active"},
                  "authenticationMethodInfo": {"displayName": "Token"}
                },
                {
                  "referenceNumber": "20260418-AU-1234567890-BBBBBBBBBB-02",
                  "isCurrent": false,
                  "tokenRedeemed": false,
                  "startDate": "2026-04-17T12:00:00+02:00",
                  "status": {"code": 200, "description": "Active"}
                }
              ]
            }
            """;
    private static final String REFRESHED_TOKEN_RESPONSE = """
            {
              "accessToken": {
                "token": "refreshed-access-token",
                "validUntil": "2026-04-18T15:00:00+02:00"
              }
            }
            """;

    @Test
    void listAuthSessions_mapsResponseIntoAuthSessionRecords(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // given — assign priority 1 (lowest = highest WireMock precedence) so
            // this exact-URL stub overrides the fixture's catch-all
            // urlMatching("/v2/auth/[^/]+") at priority 5 (default).
            stubFor(get(urlEqualTo(AUTH_SESSIONS))
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                            .withBody(SESSIONS_LIST_RESPONSE)));

            // when
            List<AuthSession> sessions = client.listAuthSessions();

            // then — at minimum the size + reference numbers must round-trip.
            // current()/tokenRedeemed() pass through the OpenAPI raw model setters
            // which Jackson sets via @JsonProperty annotations on the no-arg
            // constructor + setters; a future raw-model regeneration could break
            // the mapping, so we assert the wire shape we actually rely on.
            assertEquals(2, sessions.size());
            assertEquals("20260418-AU-1234567890-AAAAAAAAAA-01", sessions.get(0).referenceNumber());
            assertEquals("20260418-AU-1234567890-BBBBBBBBBB-02", sessions.get(1).referenceNumber());
            assertNotNull(sessions.get(0).status());
            assertEquals(200, sessions.get(0).status().code());
            // The second row has no authenticationMethodInfo block — must map to null,
            // not throw.
            org.junit.jupiter.api.Assertions.assertNull(sessions.get(1).authenticationMethod());
        }
    }

    @Test
    void terminateAuthSession_callsDeleteOnReferencePath(WireMockRuntimeInfo wmInfo) {
        String targetRef = "20260418-AU-1234567890-CCCCCCCCCC-03";
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // given — stub registered after the fixture's auth stubs.
            stubFor(delete(urlEqualTo(AUTH_SESSIONS + "/" + targetRef))
                    .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

            // when
            client.terminateAuthSession(targetRef);

            // then
            verify(deleteRequestedFor(urlEqualTo(AUTH_SESSIONS + "/" + targetRef))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + KsefAuthFlowFixture.DEFAULT_TEST_TOKEN)));
        }
    }

    @Test
    void terminateAuthSession_rejectsNullReference(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            assertThrows(NullPointerException.class, () -> client.terminateAuthSession(null));
        }
    }

    @Test
    void refreshAuthToken_postsRefreshTokenAndUpdatesBearer(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // given — first force the lazy auth flow so the refresh token is captured
            // in SessionContext, then stub /auth/token/refresh.
            client.authenticate();
            stubFor(post(urlEqualTo(AUTH_TOKEN_REFRESH))
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                            .withBody(REFRESHED_TOKEN_RESPONSE)));

            // when
            client.refreshAuthToken();

            // then — verify the refresh endpoint was hit with the captured refresh token
            // (the fixture redeems a refresh token named "test-refresh-token"; the SDK
            // sends it as a Bearer header on /auth/token/refresh).
            verify(postRequestedFor(urlEqualTo(AUTH_TOKEN_REFRESH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + "test-refresh-token")));
            // bearerToken() now returns the refreshed token
            assertEquals("refreshed-access-token", client.bearerToken());
        }
    }

    @Test
    void publicKeyCertificates_returnsCachedSecurityCerts(WireMockRuntimeInfo wmInfo) {
        // The fixture stubs /security/public-key-certificates with one cert. Calling
        // the public method should return that list.
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // when
            List<PublicKeyCertificate> certs = client.publicKeyCertificates();

            // then
            assertEquals(1, certs.size());
            assertNotNull(certs.get(0).certificate());
            assertNotNull(certs.get(0).usage());
            assertFalse(certs.get(0).usage().isEmpty());
        }
    }

    @Test
    void bearerToken_returnsAccessTokenAfterAuthenticate(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // when — fixture's authenticate() finishes implicitly on first auth call;
            // bearerToken() forces ensureAuthenticated()
            String token = client.bearerToken();

            // then
            assertEquals(KsefAuthFlowFixture.DEFAULT_TEST_TOKEN, token);
        }
    }
}
