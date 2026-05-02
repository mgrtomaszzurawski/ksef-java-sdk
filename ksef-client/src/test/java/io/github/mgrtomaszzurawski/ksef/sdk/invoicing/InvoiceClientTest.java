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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final String INVOICES_BASE = "/api/v2/invoices";
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

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
