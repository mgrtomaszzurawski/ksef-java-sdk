/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class InvoiceClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_NIP = "1111111111";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";
    private static final String TEST_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final String TEST_EXPORT_REF = "20260404-EX-1111111111-ABCDEF1234-05";
    private static final int KSEF_STATUS_OK = 200;
    private static final byte[] TEST_INVOICE_XML = "<Faktura>test invoice</Faktura>".getBytes(StandardCharsets.UTF_8);
    private static final String INVOICES_BASE = "/v2/invoices";
    private static final String PATH_EXPORTS = INVOICES_BASE + "/exports";
    private static final String EMPTY_JSON = "{}";

    private static final String QUERY_METADATA_RESPONSE = """
            {
              "invoices": [
                {
                  "ksefNumber": "%s",
                  "invoiceType": "Vat"
                }
              ],
              "hasMore": false,
              "isTruncated": false
            }
            """.formatted(TEST_KSEF_NUMBER);

    private static final String EXPORT_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_EXPORT_REF);

    private static final String EXPORT_STATUS_RESPONSE = """
            {
              "status": {"code": 200, "description": "Completed"}
            }
            """;

    @Test
    void getByKsefNumber_whenInvoiceExists_returnsXmlBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(INVOICES_BASE + "/ksef/" + TEST_KSEF_NUMBER))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_XML)
                        .withBody(TEST_INVOICE_XML)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            byte[] invoiceXml = ksef.invoices().archive().getByKsefNumber(KsefNumber.parse(TEST_KSEF_NUMBER))
                    .xml();

            // then
            assertArrayEquals(TEST_INVOICE_XML, invoiceXml);
        }
    }

    @Test
    void getByKsefNumber_whenNotFound_throwsNotFoundException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(INVOICES_BASE + "/ksef/" + TEST_KSEF_NUMBER))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(EMPTY_JSON)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // then
            var archive = ksef.invoices().archive();

            KsefNumber ksefNumber = KsefNumber.parse(TEST_KSEF_NUMBER);
            assertThrows(KsefNotFoundException.class, () -> archive.getByKsefNumber(ksefNumber));
        }
    }

    @Test
    void queryMetadata_whenFiltersMatch_returnsMetadataList(WireMockRuntimeInfo wmInfo) {
        // given — A.1.2: queryInvoicesByMetadata now appends ?pageOffset=0&pageSize=250
        // for spec-conformant single-page fetch.
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_METADATA_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1));
            InvoiceMetadataResult response = ksef.invoices().archive().queryByMetadata(query.build());

            // then
            assertEquals(1, response.invoices().size());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void streamMetadata_whenSecondPageRequested_preservesAllFiltersBetweenPages(WireMockRuntimeInfo wmInfo) {
        // given — A.1.2 spec algorithm:
        //   hasMore=true && isTruncated=false → pageOffset++ (same dateRange).
        // Page 1 stubs the in-flight state; page 2 closes the loop with hasMore=false.
        String pageOneBody = """
                {
                  "invoices": [{"ksefNumber": "%s", "invoiceType": "Vat"}],
                  "hasMore": true,
                  "isTruncated": false
                }
                """.formatted(TEST_KSEF_NUMBER);
        String pageTwoBody = """
                {
                  "invoices": [],
                  "hasMore": false,
                  "isTruncated": false
                }
                """;
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .inScenario("paginate")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody))
                .willSetStateTo("page2"));
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .inScenario("paginate")
                .whenScenarioStateIs("page2")
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageTwoBody)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            // when
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.parse("2026-04-01T00:00:00Z"))
                    .ksefNumber(TEST_KSEF_NUMBER)
                    .sellerNip(TEST_NIP)
                    .onlineOnly()
                    .selfInvoicing(true)
                    .hasAttachment(true);
            ksef.invoices().archive().streamByMetadata(query.build()).toList();

            // then — both POSTs hit. Page 2 body carries the same caller filters,
            // and the spec-conformant algorithm advanced pageOffset (not the
            // dateRange) since isTruncated=false.
            var requests = findAll(postRequestedFor(urlPathEqualTo(INVOICES_BASE + "/query/metadata")));
            assertEquals(2, requests.size());
            assertTrue(requests.get(0).getUrl().contains("pageOffset=0"),
                    "page 1 should be pageOffset=0, was URL: " + requests.get(0).getUrl());
            assertTrue(requests.get(1).getUrl().contains("pageOffset=1"),
                    "page 2 must advance pageOffset (not dateRange) when isTruncated=false, was URL: "
                            + requests.get(1).getUrl());
            String pageTwoRequestBody = requests.get(1).getBodyAsString();
            assertTrue(pageTwoRequestBody.contains(TEST_KSEF_NUMBER),
                    "page 2 must preserve ksefNumber filter, was: " + pageTwoRequestBody);
            assertTrue(pageTwoRequestBody.contains(TEST_NIP),
                    "page 2 must preserve sellerNip filter, was: " + pageTwoRequestBody);
            assertTrue(pageTwoRequestBody.contains("\"invoicingMode\":\"Online\""),
                    "page 2 must preserve invoicingMode filter, was: " + pageTwoRequestBody);
            assertTrue(pageTwoRequestBody.contains("\"isSelfInvoicing\":true"),
                    "page 2 must preserve isSelfInvoicing filter, was: " + pageTwoRequestBody);
            assertTrue(pageTwoRequestBody.contains("\"hasAttachment\":true"),
                    "page 2 must preserve hasAttachment filter, was: " + pageTwoRequestBody);
        }
    }

    @Test
    void streamMetadata_whenIsTruncated_resetsPageOffsetAndAdvancesDateRange(WireMockRuntimeInfo wmInfo) {
        // given — A.1.2 spec algorithm:
        //   hasMore=true && isTruncated=true → narrow dateRange.from to the
        //                                     LAST RETURNED RECORD's date for
        //                                     the chosen dateType axis +
        //                                     reset pageOffset to 0.
        String lastRecordDate = "2026-04-15T10:00:00Z";
        String pageOneBody = """
                {
                  "invoices": [{"ksefNumber": "%s", "invoiceType": "Vat", "invoicingDate": "%s"}],
                  "hasMore": true,
                  "isTruncated": true,
                  "permanentStorageHwmDate": "2026-04-30T00:00:00Z"
                }
                """.formatted(TEST_KSEF_NUMBER, lastRecordDate);
        String pageTwoBody = """
                {
                  "invoices": [],
                  "hasMore": false,
                  "isTruncated": false
                }
                """;
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .inScenario("truncate")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody))
                .willSetStateTo("page2"));
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .inScenario("truncate")
                .whenScenarioStateIs("page2")
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageTwoBody)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.parse("2026-04-01T00:00:00Z"));
            ksef.invoices().archive().streamByMetadata(query.build()).toList();

            var requests = findAll(postRequestedFor(urlPathEqualTo(INVOICES_BASE + "/query/metadata")));
            assertEquals(2, requests.size());
            assertTrue(requests.get(1).getUrl().contains("pageOffset=0"),
                    "page 2 must reset pageOffset to 0 on truncation, was URL: " + requests.get(1).getUrl());
            String pageTwoBodyOut = requests.get(1).getBodyAsString();
            assertTrue(pageTwoBodyOut.contains(lastRecordDate.substring(0, 10)),
                    "page 2 must advance dateRange.from to last-record date on truncation, was: " + pageTwoBodyOut);
        }
    }

    @Test
    void streamMetadata_truncatedNullCursor_throwsKsefException(WireMockRuntimeInfo wmInfo) {
        // Codex 2026-05-05 F3 — server returns isTruncated=true but the last
        // record on the page has no value on the date axis we picked
        // (invoicingDate here, since the query filters by invoicingDateFrom).
        // Spec doesn't envisage this state; under the new fail-fast contract
        // streamInvoicesByMetadata throws instead of silently dropping pages.
        String pageOneBody = """
                {
                  "invoices": [{"ksefNumber": "%s", "invoiceType": "Vat"}],
                  "hasMore": true,
                  "isTruncated": true,
                  "permanentStorageHwmDate": "2026-04-30T00:00:00Z"
                }
                """.formatted(TEST_KSEF_NUMBER);
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.parse("2026-04-01T00:00:00Z"));
            io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException thrown =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException.class,
                            () -> ksef.invoices().archive().streamByMetadata(query.build()).toList());
            assertTrue(thrown.getMessage().contains("isTruncated"),
                    "exception message must explain truncation, was: " + thrown.getMessage());
        }
    }

    @Test
    void getExportStatus_whenExportCompleted_returnsCompletedStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_EXPORTS + "/" + TEST_EXPORT_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EXPORT_STATUS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            InvoiceExportStatus response = ksef.invoices().export().getStatus(TEST_EXPORT_REF);

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
        }
    }


    @Test
    void getByKsefNumber_whenNoPriorAuthenticate_proactivelyTriggersAuthFlow(WireMockRuntimeInfo wmInfo) {
        // given — no session.activate(). Domain endpoint returns 200 (so the OLD bug,
        // which sends Bearer null on the first call, would silently succeed with this stub).
        // Security/public-key endpoint returns 500 so proactive auth fails fast.
        stubFor(get(urlEqualTo(INVOICES_BASE + "/ksef/" + TEST_KSEF_NUMBER))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_XML)
                        .withBody(TEST_INVOICE_XML)));
        stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody(EMPTY_JSON)));

        try (KsefClient ksef = KsefClient.builder().environment(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build()) {

            // when — first protected domain call must trigger auth via requireToken()
            // BEFORE any HTTP request to the protected endpoint.
            var archive = ksef.invoices().archive();
            KsefNumber ksefNumber = KsefNumber.parse(TEST_KSEF_NUMBER);
            assertThrows(RuntimeException.class, () -> archive.getByKsefNumber(ksefNumber));

            // then — proactive auth attempt observed at /security/public-key-certificates,
            // and the protected domain endpoint was NEVER called (old behavior would have
            // made exactly one call with Bearer null and silently received the 200 stub).
            var securityRequests = findAll(com.github.tomakehurst.wiremock.client.WireMock
                    .getRequestedFor(urlEqualTo("/v2/security/public-key-certificates")));
            assertFalse(securityRequests.isEmpty(),
                    "expected proactive auth to fetch public keys, got: " + securityRequests.size());
            var domainRequests = findAll(com.github.tomakehurst.wiremock.client.WireMock
                    .getRequestedFor(urlEqualTo(INVOICES_BASE + "/ksef/" + TEST_KSEF_NUMBER)));
            assertEquals(0, domainRequests.size(),
                    "no protected domain request must escape before auth completes, got: " + domainRequests.size());
        }
    }

    @Test
    void queryInvoicesByMetadata_whenSortOrderDesc_appendsSortOrderQueryParam(WireMockRuntimeInfo wmInfo) {
        // given — sortOrder=Desc must be appended to the URL query string.
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .withQueryParam("sortOrder", equalTo("Desc"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_METADATA_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            // when
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1))
                    .sortOrder(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder.DESC);
            InvoiceMetadataResult response = ksef.invoices().archive().queryByMetadata(query.build());

            // then
            assertEquals(1, response.invoices().size());
            verify(postRequestedFor(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                    .withQueryParam("sortOrder", equalTo("Desc")));
        }
    }

    @Test
    void queryInvoicesByMetadata_whenSortOrderAsc_appendsSortOrderQueryParam(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                .withQueryParam("sortOrder", equalTo("Asc"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_METADATA_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            // when
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1))
                    .sortOrder(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder.ASC);
            ksef.invoices().archive().queryByMetadata(query.build());

            // then
            verify(postRequestedFor(urlPathEqualTo(INVOICES_BASE + "/query/metadata"))
                    .withQueryParam("sortOrder", equalTo("Asc")));
        }
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1111111111");
    }
}
