/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefTokenCredentials;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.tokens.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.tokens.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.tokens.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.tokens.TokenGenerateBuilder;
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
        GenerateTokenResult response = ksef.tokens().generate(TokenGenerateBuilder.create("test description").invoiceRead());

        // then
        assertEquals(TEST_TOKEN_REF, response.referenceNumber());
        assertEquals(TEST_GENERATED_TOKEN, response.token());
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
        TokenList response = ksef.tokens().list();

        // then
        assertNotNull(response.tokens());
        assertEquals(1, response.tokens().size());
        assertEquals(TEST_TOKEN_REF, response.tokens().get(0).referenceNumber());
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
        TokenDetail response = ksef.tokens().getStatus(TEST_TOKEN_REF);

        // then
        assertEquals(TEST_TOKEN_REF, response.referenceNumber());
        assertNotNull(response.dateCreated());
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
        // given — both the target endpoint and the reauth security endpoint return 401,
        // so after the SDK retries once on 401 the auth exception propagates.
        stubFor(post(urlEqualTo("/api/v2/tokens"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo("/api/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefAuthException.class,
                () -> ksef.tokens().generate(TokenGenerateBuilder.create("test description").invoiceRead()));
    }

    @Test
    void getStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        assertThrows(IllegalArgumentException.class,
                () -> ksef.tokens().getStatus("../../../etc/passwd"));
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
