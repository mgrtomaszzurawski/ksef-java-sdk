/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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

@WireMockTest
class TokenClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_TOKEN_REF = "20260404-TK-1234567890-ABCDEF1234-07";
    private static final String TEST_GENERATED_TOKEN = "generated-ksef-token-value-abc123";

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_UNAUTHORIZED = 401;

    private static final String GENERATE_RESPONSE = """
            {
              "referenceNumber": "%s",
              "token": "%s"
            }
            """.formatted(TEST_TOKEN_REF, TEST_GENERATED_TOKEN);

    private static final String LIST_RESPONSE = """
            {
              "tokens": [
                {"referenceNumber": "%s", "description": "Test token"}
              ]
            }
            """.formatted(TEST_TOKEN_REF);

    private static final String STATUS_RESPONSE = """
            {
              "referenceNumber": "%s",
              "description": "Test token",
              "status": "Active",
              "dateCreated": "2026-04-04T12:00:00+02:00"
            }
            """.formatted(TEST_TOKEN_REF);

    @Test
    void generate_whenAuthenticated_returnsTokenResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/tokens"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(GENERATE_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        GenerateTokenResponseRaw response = ksef.tokens().generate(new GenerateTokenRequestRaw());

        // then
        assertEquals(TEST_TOKEN_REF, response.getReferenceNumber());
        assertEquals(TEST_GENERATED_TOKEN, response.getToken());
    }

    @Test
    void list_whenAuthenticated_returnsTokenList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/tokens"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(LIST_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        QueryTokensResponseRaw response = ksef.tokens().list();

        // then
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals(TEST_TOKEN_REF, response.getTokens().get(0).getReferenceNumber());
    }

    @Test
    void getStatus_whenTokenExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/tokens/" + TEST_TOKEN_REF))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STATUS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        TokenStatusResponseRaw response = ksef.tokens().getStatus(TEST_TOKEN_REF);

        // then
        assertEquals(TEST_TOKEN_REF, response.getReferenceNumber());
        assertNotNull(response.getDateCreated());
    }

    @Test
    void revoke_whenTokenExists_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        String deletePath = "/api/v2/tokens/" + TEST_TOKEN_REF;
        stubFor(delete(urlEqualTo(deletePath))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.tokens().revoke(TEST_TOKEN_REF);

        // then
        verify(deleteRequestedFor(urlEqualTo(deletePath))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void generate_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/tokens"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefAuthException.class,
                () -> ksef.tokens().generate(new GenerateTokenRequestRaw()));
    }

    @Test
    void getStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        assertThrows(IllegalArgumentException.class,
                () -> ksef.tokens().getStatus("../../../etc/passwd"));
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
