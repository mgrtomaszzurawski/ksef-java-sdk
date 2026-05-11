/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.peppol;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class PeppolClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1111111111-ABCDEF1234-01";
    private static final String AUTH_HEADER = TestHttpConstants.AUTHORIZATION_HEADER;
    private static final String BEARER_TOKEN = TestHttpConstants.BEARER_PREFIX + TEST_TOKEN;
    private static final String CONTENT_TYPE_HEADER = TestHttpConstants.CONTENT_TYPE_HEADER;
    private static final String APPLICATION_JSON = TestHttpConstants.APPLICATION_JSON;
    private static final String DEFAULT_QUERY_URL = "/v2/peppol/query?pageOffset=0&pageSize=10";
    private static final String PAGED_QUERY_URL = "/v2/peppol/query?pageOffset=2&pageSize=5";

    private static final int DEFAULT_PAGE_OFFSET = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int CUSTOM_PAGE_OFFSET = 2;
    private static final int CUSTOM_PAGE_SIZE = 5;
    private static final int EXPECTED_PROVIDER_COUNT = 2;

    private static final String PROVIDER_ID_1 = "PL-9999000001";
    private static final String PROVIDER_NAME_1 = "Acme Peppol Sp. z o.o.";
    private static final String PROVIDER_DATE_1 = "2026-01-15T10:30:00Z";

    private static final String PROVIDER_ID_2 = "PL-9999000002";
    private static final String PROVIDER_NAME_2 = "Beta Peppol S.A.";
    private static final String PROVIDER_DATE_2 = "2026-02-20T08:15:00Z";

    private static final String TWO_PROVIDERS_RESPONSE = """
            {
              "peppolProviders": [
                {"id": "%s", "name": "%s", "dateCreated": "%s"},
                {"id": "%s", "name": "%s", "dateCreated": "%s"}
              ],
              "hasMore": true
            }
            """.formatted(PROVIDER_ID_1, PROVIDER_NAME_1, PROVIDER_DATE_1,
            PROVIDER_ID_2, PROVIDER_NAME_2, PROVIDER_DATE_2);

    private static final String EMPTY_PROVIDERS_RESPONSE = """
            {
              "peppolProviders": [],
              "hasMore": false
            }
            """;

    @Test
    void query_whenNoArguments_usesDefaultPagingAndReturnsProviders(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(DEFAULT_QUERY_URL))
                .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody(TWO_PROVIDERS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PeppolProvidersResult result = ksef.peppol().queryProviders(new PeppolProvidersQueryRequest(DEFAULT_PAGE_OFFSET, DEFAULT_PAGE_SIZE));

            // then
            assertNotNull(result);
            assertEquals(EXPECTED_PROVIDER_COUNT, result.providers().size());
            assertEquals(PROVIDER_ID_1, result.providers().get(0).id());
            assertEquals(PROVIDER_NAME_1, result.providers().get(0).name());
            assertNotNull(result.providers().get(0).dateCreated());
            assertEquals(PROVIDER_ID_2, result.providers().get(1).id());
            assertTrue(result.hasMore());
            verify(getRequestedFor(urlEqualTo(DEFAULT_QUERY_URL))
                    .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN)));
        }
    }

    @Test
    void query_whenPaginationProvided_sendsRequestedPagingParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PAGED_QUERY_URL))
                .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody(TWO_PROVIDERS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PeppolProvidersResult result = ksef.peppol().queryProviders(new PeppolProvidersQueryRequest(CUSTOM_PAGE_OFFSET, CUSTOM_PAGE_SIZE));

            // then
            assertNotNull(result);
            assertEquals(EXPECTED_PROVIDER_COUNT, result.providers().size());
            assertTrue(result.hasMore());
            verify(getRequestedFor(urlEqualTo(PAGED_QUERY_URL))
                    .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN)));
        }
    }

    @Test
    void query_whenNoProviders_returnsEmptyResultWithoutMore(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(DEFAULT_QUERY_URL))
                .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody(EMPTY_PROVIDERS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PeppolProvidersResult result = ksef.peppol().queryProviders(new PeppolProvidersQueryRequest(DEFAULT_PAGE_OFFSET, DEFAULT_PAGE_SIZE));

            // then
            assertNotNull(result);
            assertTrue(result.providers().isEmpty());
            assertFalse(result.hasMore());
        }
    }

    @Test
    void peppolProvidersQueryRequest_whenPageOffsetNegative_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new PeppolProvidersQueryRequest(-1, CUSTOM_PAGE_SIZE));
    }

    @Test
    void peppolProvidersQueryRequest_whenPageSizeZero_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new PeppolProvidersQueryRequest(CUSTOM_PAGE_OFFSET, 0));
    }

    @Test
    void streamProviders_walksUntilHasMoreFalse(WireMockRuntimeInfo wmInfo) {
        // given — 2 pages; first reports hasMore=true, second hasMore=false
        // (stream-page size hardcoded to STREAM_PAGE_SIZE=50 in PeppolProvidersImpl)
        String firstPageBody = """
                {
                  "peppolProviders": [
                    {"id": "%s", "name": "%s", "dateCreated": "%s"}
                  ],
                  "hasMore": true
                }
                """.formatted(PROVIDER_ID_1, PROVIDER_NAME_1, PROVIDER_DATE_1);
        String secondPageBody = """
                {
                  "peppolProviders": [
                    {"id": "%s", "name": "%s", "dateCreated": "%s"}
                  ],
                  "hasMore": false
                }
                """.formatted(PROVIDER_ID_2, PROVIDER_NAME_2, PROVIDER_DATE_2);

        stubFor(get(urlEqualTo("/v2/peppol/query?pageOffset=0&pageSize=50"))
                .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody(firstPageBody)));
        stubFor(get(urlEqualTo("/v2/peppol/query?pageOffset=1&pageSize=50"))
                .withHeader(AUTH_HEADER, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody(secondPageBody)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            var collected = ksef.peppol().streamProviders().toList();

            // then
            assertEquals(EXPECTED_PROVIDER_COUNT, collected.size());
            assertEquals(PROVIDER_ID_1, collected.get(0).id());
            assertEquals(PROVIDER_ID_2, collected.get(1).id());
            verify(getRequestedFor(urlEqualTo("/v2/peppol/query?pageOffset=0&pageSize=50")));
            verify(getRequestedFor(urlEqualTo("/v2/peppol/query?pageOffset=1&pageSize=50")));
        }
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1111111111");
    }
}
