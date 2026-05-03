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
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class InvoiceClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";
    private static final String TEST_KSEF_NUMBER = "1234567890-20260404-ABCDEF123456-78";
    private static final String TEST_EXPORT_REF = "20260404-EX-1234567890-ABCDEF1234-05";
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
            byte[] invoiceXml = ksef.invoices().getByKsefNumber(TEST_KSEF_NUMBER);

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
            var invoices = ksef.invoices();

            assertThrows(KsefNotFoundException.class, () -> invoices.getByKsefNumber(TEST_KSEF_NUMBER));
        }
    }

    @Test
    void queryMetadata_whenFiltersMatch_returnsMetadataList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(INVOICES_BASE + "/query/metadata"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_METADATA_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1));
            InvoiceMetadataResult response = ksef.invoices().queryMetadata(query);

            // then
            assertEquals(1, response.invoices().size());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryAllMetadata_whenSecondPageRequested_preservesAllFiltersBetweenPages(WireMockRuntimeInfo wmInfo) {
        // given — page 1 carries hasMore=true + permanentStorageHwmDate cursor; page 2 closes the loop.
        String hwmCursor = "2026-04-15T10:00:00.000Z";
        String pageOneBody = """
                {
                  "invoices": [{"ksefNumber": "%s", "invoiceType": "Vat"}],
                  "hasMore": true,
                  "isTruncated": false,
                  "permanentStorageHwmDate": "%s"
                }
                """.formatted(TEST_KSEF_NUMBER, hwmCursor);
        String pageTwoBody = """
                {
                  "invoices": [],
                  "hasMore": false,
                  "isTruncated": false
                }
                """;
        stubFor(post(urlEqualTo(INVOICES_BASE + "/query/metadata"))
                .inScenario("paginate")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody))
                .willSetStateTo("page2"));
        stubFor(post(urlEqualTo(INVOICES_BASE + "/query/metadata"))
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
            ksef.invoices().queryAllMetadata(query);

            // then — both POSTs hit, page 2 body carries the same caller filters.
            var requests = findAll(postRequestedFor(urlEqualTo(INVOICES_BASE + "/query/metadata")));
            assertEquals(2, requests.size());
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
            assertTrue(pageTwoRequestBody.contains(hwmCursor.substring(0, 10)),
                    "page 2 must advance dateRange.from to HWM cursor, was: " + pageTwoRequestBody);
        }
    }

    @Test
    void exportInvoices_whenRequested_returnsExportReference(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_EXPORTS))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EXPORT_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            InvoiceExportBuilder exportBuilder = InvoiceExportBuilder.create(
                            TestCertificates.generateRsa().certificate().getPublicKey())
                    .filters(InvoiceQueryBuilder.seller()
                            .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1)))
                    .metadataOnly();
            ExportInvoicesResult response = ksef.invoices().exportInvoices(exportBuilder);

            // then
            assertEquals(TEST_EXPORT_REF, response.referenceNumber());
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
            InvoiceExportStatus response = ksef.invoices().getExportStatus(TEST_EXPORT_REF);

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
        }
    }

    @Test
    void exportInvoices_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo(PATH_EXPORTS))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody(EMPTY_JSON)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            InvoiceExportBuilder exportBuilder = InvoiceExportBuilder.create(
                            TestCertificates.generateRsa().certificate().getPublicKey())
                    .filters(InvoiceQueryBuilder.seller()
                            .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1)))
                    .metadataOnly();

            // then
            var invoices = ksef.invoices();

            assertThrows(KsefServerException.class, () -> invoices.exportInvoices(exportBuilder));
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

        try (KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build()) {

            // when — first protected domain call must trigger auth via requireToken()
            // BEFORE any HTTP request to the protected endpoint.
            var invoices = ksef.invoices();
            assertThrows(RuntimeException.class, () -> invoices.getByKsefNumber(TEST_KSEF_NUMBER));

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

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.runtime().sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
