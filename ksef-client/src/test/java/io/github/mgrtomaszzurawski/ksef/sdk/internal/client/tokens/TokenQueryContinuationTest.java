/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenQueryRequest;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins {@link TokenQueryBuilder#continuationToken(String)} round-trip
 * through {@code build()} and the wire shape of
 * {@code TokensImpl.queryTokens} when a continuation cursor is set —
 * the cursor must flow as the {@code x-continuation-token} HTTP header
 * (per KSeF spec), not as a query parameter.
 */
@WireMockTest
class TokenQueryContinuationTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_NIP = "1111111111";
    private static final String PATH_TOKENS = "/v2/tokens";
    private static final String HEADER_CONTINUATION = "x-continuation-token";
    private static final String CURSOR_VALUE = "cursor-abc-123";

    private static final String EMPTY_LIST_RESPONSE = """
            {
              "tokens": []
            }
            """;

    @Test
    void continuationToken_roundTripsThroughBuilderBuild() {
        TokenQueryRequest request = TokenQueryBuilder.create()
                .continuationToken(CURSOR_VALUE)
                .build();
        assertEquals(CURSOR_VALUE, request.continuationToken());
    }

    @Test
    void continuationToken_whenNull_throwsNullPointer() {
        TokenQueryBuilder builder = TokenQueryBuilder.create();
        assertThrows(NullPointerException.class,
                () -> builder.continuationToken(null));
    }

    @Test
    void queryTokens_whenCursorSet_sendsXContinuationTokenHeader(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlPathEqualTo(PATH_TOKENS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_LIST_RESPONSE)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, TEST_NIP)) {
            client.tokens().queryTokens(
                    TokenQueryBuilder.create().continuationToken(CURSOR_VALUE).build());

            verify(getRequestedFor(urlPathEqualTo(PATH_TOKENS))
                    .withHeader(HEADER_CONTINUATION, equalTo(CURSOR_VALUE)));
        }
    }

    @Test
    void queryTokens_whenCursorAbsent_omitsXContinuationTokenHeader(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlPathEqualTo(PATH_TOKENS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_LIST_RESPONSE)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, TEST_NIP)) {
            client.tokens().queryTokens(TokenQueryBuilder.create().build());

            verify(getRequestedFor(urlPathEqualTo(PATH_TOKENS))
                    .withHeader(HEADER_CONTINUATION, absent()));
        }
    }
}
