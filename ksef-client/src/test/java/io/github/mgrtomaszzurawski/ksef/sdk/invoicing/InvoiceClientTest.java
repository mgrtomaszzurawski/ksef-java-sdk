/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefTokenCredentials;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder.InvoiceQueryBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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

@WireMockTest
class InvoiceClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";
    private static final String TEST_KSEF_NUMBER = "1234567890-20260404-ABCDEF123456-78";
    private static final String TEST_EXPORT_REF = "20260404-EX-1234567890-ABCDEF1234-05";

    private static final int HTTP_OK = 200;
    private static final int KSEF_STATUS_OK = 200;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final byte[] TEST_INVOICE_XML = "<Faktura>test invoice</Faktura>".getBytes(StandardCharsets.UTF_8);

    private static final String QUERY_METADATA_RESPONSE = """
            {
              "invoices": [
                {
                  "ksefNumber": "%s",
                  "invoiceType": "Vat"
                }
              ],
              "hasMore": false
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
        stubFor(get(urlEqualTo("/api/v2/invoices/ksef/" + TEST_KSEF_NUMBER))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/xml")
                        .withBody(TEST_INVOICE_XML)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        byte[] invoiceXml = ksef.invoices().getByKsefNumber(TEST_KSEF_NUMBER);

        // then
        assertArrayEquals(TEST_INVOICE_XML, invoiceXml);
    }

    @Test
    void getByKsefNumber_whenNotFound_throwsNotFoundException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/invoices/ksef/" + TEST_KSEF_NUMBER))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefNotFoundException.class,
                () -> ksef.invoices().getByKsefNumber(TEST_KSEF_NUMBER));
    }

    @Test
    void queryMetadata_whenFiltersMatch_returnsMetadataList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/invoices/query/metadata"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(QUERY_METADATA_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1));
        InvoiceMetadataResult response = ksef.invoices().queryMetadata(query);

        // then
        assertEquals(1, response.invoices().size());
        assertFalse(response.hasMore());
    }

    @Test
    void exportInvoices_whenRequested_returnsExportReference(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo("/api/v2/invoices/exports"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EXPORT_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

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

    @Test
    void getExportStatus_whenExportCompleted_returnsCompletedStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/invoices/exports/" + TEST_EXPORT_REF))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EXPORT_STATUS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        InvoiceExportStatus response = ksef.invoices().getExportStatus(TEST_EXPORT_REF);

        // then
        assertEquals(KSEF_STATUS_OK, response.status().code());
    }

    @Test
    void exportInvoices_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubFor(post(urlEqualTo("/api/v2/invoices/exports"))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);
        InvoiceExportBuilder exportBuilder = InvoiceExportBuilder.create(
                        TestCertificates.generateRsa().certificate().getPublicKey())
                .filters(InvoiceQueryBuilder.seller()
                        .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1)))
                .metadataOnly();

        // then
        assertThrows(KsefServerException.class,
                () -> ksef.invoices().exportInvoices(exportBuilder));
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
