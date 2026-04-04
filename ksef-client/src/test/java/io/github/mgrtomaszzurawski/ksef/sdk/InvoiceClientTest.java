/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class InvoiceClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
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
        QueryInvoicesMetadataResponseRaw response = ksef.invoices()
                .queryMetadata(new InvoiceQueryFiltersRaw());

        // then
        assertEquals(1, response.getInvoices().size());
        assertEquals(false, response.getHasMore());
    }

    @Test
    void exportInvoices_whenRequested_returnsExportReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/invoices/exports"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EXPORT_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ExportInvoicesResponseRaw response = ksef.invoices()
                .exportInvoices(new InvoiceExportRequestRaw());

        // then
        assertEquals(TEST_EXPORT_REF, response.getReferenceNumber());
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
        InvoiceExportStatusResponseRaw response = ksef.invoices().getExportStatus(TEST_EXPORT_REF);

        // then
        assertEquals(Integer.valueOf(KSEF_STATUS_OK), response.getStatus().getCode());
    }

    @Test
    void exportInvoices_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/invoices/exports"))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> ksef.invoices().exportInvoices(new InvoiceExportRequestRaw()));
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
