/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Public-facade WireMock tests for the auth-session management methods
 * exposed via {@code KsefClient.auth()} after the PR6 surface trim.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code client.auth().streamAuthSessions()} — maps the
 *       {@code /v2/auth/sessions} body into {@link AuthSession} records.</li>
 *   <li>{@code client.auth().terminateSession(String)} — DELETE on
 *       {@code /v2/auth/sessions/{referenceNumber}}.</li>
 * </ul>
 */
@WireMockTest
class KsefClientPublicAuthFacadeTest {

    private static final String AUTH_BASE = "/v2/auth";
    private static final String AUTH_SESSIONS = AUTH_BASE + "/sessions";
    private static final String SESSIONS_LIST_RESPONSE = """
            {
              "items": [
                {
                  "referenceNumber": "20260418-AU-1111111111-AAAAAAAAAA-01",
                  "isCurrent": true,
                  "tokenRedeemed": true,
                  "startDate": "2026-04-18T12:00:00+02:00",
                  "lastTokenRefreshDate": "2026-04-18T13:00:00+02:00",
                  "refreshTokenValidUntil": "2026-04-25T13:00:00+02:00",
                  "status": {"code": 200, "description": "Active"},
                  "authenticationMethodInfo": {"displayName": "Token"}
                },
                {
                  "referenceNumber": "20260418-AU-1111111111-BBBBBBBBBB-02",
                  "isCurrent": false,
                  "tokenRedeemed": false,
                  "startDate": "2026-04-17T12:00:00+02:00",
                  "status": {"code": 200, "description": "Active"}
                }
              ]
            }
            """;

    @Test
    void streamSessions_mapsResponseIntoAuthSessionRecords(WireMockRuntimeInfo wmInfo) {
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
            List<AuthSession> sessions = client.auth().streamAuthSessions().toList();

            // then — at minimum the size + reference numbers must round-trip.
            // current()/tokenRedeemed() pass through the OpenAPI raw model setters
            // which Jackson sets via @JsonProperty annotations on the no-arg
            // constructor + setters; a future raw-model regeneration could break
            // the mapping, so we assert the wire shape we actually rely on.
            assertEquals(2, sessions.size());
            assertEquals("20260418-AU-1111111111-AAAAAAAAAA-01", sessions.get(0).referenceNumber());
            assertEquals("20260418-AU-1111111111-BBBBBBBBBB-02", sessions.get(1).referenceNumber());
            assertNotNull(sessions.get(0).status());
            assertEquals(200, sessions.get(0).status().code());
            // The second row has no authenticationMethodInfo block — must map to null,
            // not throw.
            org.junit.jupiter.api.Assertions.assertNull(sessions.get(1).authenticationMethod());
        }
    }

    @Test
    void terminateSession_callsDeleteOnReferencePath(WireMockRuntimeInfo wmInfo) {
        String targetRef = "20260418-AU-1111111111-CCCCCCCCCC-03";
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // given — stub registered after the fixture's auth stubs.
            stubFor(delete(urlEqualTo(AUTH_SESSIONS + "/" + targetRef))
                    .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

            // when
            client.auth().terminateSession(targetRef);

            // then
            verify(deleteRequestedFor(urlEqualTo(AUTH_SESSIONS + "/" + targetRef))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + KsefAuthFlowFixture.DEFAULT_TEST_TOKEN)));
        }
    }

    @Test
    void terminateSession_rejectsNullReference(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            assertThrows(NullPointerException.class, () -> client.auth().terminateSession(null));
        }
    }
}
