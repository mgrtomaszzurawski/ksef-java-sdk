/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class ClearedFromArchiveTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_NIP = "1111111111";
    private static final String SESSION_REF = "20260509-SE-1111111111-AABBCC1234-01";
    private static final String INVOICE_REF = "20260509-EE-1111111111-DDEEFF5678-22";
    private static final String KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final byte[] INVOICE_XML = "<Faktura>recovered</Faktura>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] UPO_XML = "<Potwierdzenie>upo bytes</Potwierdzenie>".getBytes(StandardCharsets.UTF_8);

    private static final String STATUS_ACCEPTED_RESPONSE = """
            {
              "ordinalNumber": 1,
              "referenceNumber": "%s",
              "ksefNumber": "%s",
              "invoiceHash": "ZHVtbXktaGFzaA==",
              "invoicingDate": "2026-05-09T10:00:00Z",
              "status": {"code": 200, "description": "Sukces"}
            }
            """.formatted(INVOICE_REF, KSEF_NUMBER);

    private static final String STATUS_PROCESSING_RESPONSE = """
            {
              "ordinalNumber": 1,
              "referenceNumber": "%s",
              "invoiceHash": "ZHVtbXktaGFzaA==",
              "invoicingDate": "2026-05-09T10:00:00Z",
              "status": {"code": 150, "description": "Trwa przetwarzanie"}
            }
            """.formatted(INVOICE_REF);

    @Test
    void clearedFromArchive_whenAccepted_returnsRecoveredInvoice(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/v2/sessions/" + SESSION_REF + "/invoices/" + INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo("/v2/invoices/ksef/" + KSEF_NUMBER))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_XML)
                        .withBody(INVOICE_XML)));
        stubFor(get(urlEqualTo("/v2/sessions/" + SESSION_REF + "/invoices/" + INVOICE_REF + "/upo"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_XML)
                        .withBody(UPO_XML)));

        try (KsefClient ksef = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, TEST_NIP)) {
            ClearedInvoice cleared = ksef.invoices().clearedFromArchive(SESSION_REF, INVOICE_REF);

            assertEquals(INVOICE_REF, cleared.submitted().referenceNumber());
            assertEquals(KSEF_NUMBER, cleared.submitted().ksefNumber().orElseThrow().value());
            assertArrayEquals(INVOICE_XML, cleared.submitted().invoice().xml());
            assertEquals(INVOICE_REF, cleared.upo().referenceNumber());
            assertArrayEquals(UPO_XML, cleared.upo().xmlBytes());
        }
    }

    @Test
    void clearedFromArchive_whenNotAccepted_throwsKsefException(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/v2/sessions/" + SESSION_REF + "/invoices/" + INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_PROCESSING_RESPONSE)));

        try (KsefClient ksef = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, TEST_NIP)) {
            var invoices = ksef.invoices();
            assertThrows(KsefException.class, () -> invoices.clearedFromArchive(SESSION_REF, INVOICE_REF));
        }
    }
}
