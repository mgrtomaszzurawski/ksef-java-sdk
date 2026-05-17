/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.batch;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock public-flow test for the new synchronous batch facade
 * {@code Invoices.submitBatch(...)} introduced in PR11. Stubs the full
 * batch lifecycle (open / part upload / close / status poll / per-invoice
 * UPO fetch) and asserts that {@link BatchResult} is populated correctly
 * for both the all-success and partial-failure scenarios.
 */
@WireMockTest
class SubmitBatchTest {

    private static final String BATCH_PATH = "/v2/sessions/batch";
    private static final String BATCH_REF = "20260509-BA-1111111111-AAAAAAAAAA-01";
    private static final String SESSIONS_PATH = "/v2/sessions/" + BATCH_REF;
    private static final String SESSION_INVOICES_PATH = "/v2/sessions/" + BATCH_REF + "/invoices";
    private static final String SESSION_FAILED_PATH = "/v2/sessions/" + BATCH_REF + "/invoices/failed";
    private static final String CLOSE_PATH = BATCH_PATH + "/" + BATCH_REF + "/close";

    private static final String INVOICE_REF_1 = "INV-1";
    private static final String INVOICE_REF_2 = "INV-2";
    private static final String INVOICE_REF_3 = "INV-3";
    private static final String UPLOAD_URL = "http://127.0.0.1:%d/upload/part1";
    private static final byte[] UPO_BYTES_1 = "<UPO>1</UPO>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] UPO_BYTES_2 = "<UPO>2</UPO>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] UPO_BYTES_3 = "<UPO>3</UPO>".getBytes(StandardCharsets.UTF_8);


    private static final int EXPECTED_INVOICE_COUNT = 3;
    private static final int EXPECTED_FAILED_FAILURE = 1;
    private static final int EXPECTED_CLEARED_PARTIAL = 2;

    private static final BatchOptions FAST_OPTIONS = new BatchOptions(Duration.ofMinutes(2), 1);

    @Test
    void submitBatch_whenAllInvoicesAccepted_returnsThreeUpos(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpen(wmInfo.getHttpPort());
            stubPartUploadOk();
            stubBatchClose();
            stubStatusOkAfterClose();
            stubInvoicesPage(invoicesAllSuccess());
            stubFailedInvoicesPage(emptyInvoicesPage());
            stubUpoFetch(INVOICE_REF_1, UPO_BYTES_1);
            stubUpoFetch(INVOICE_REF_2, UPO_BYTES_2);
            stubUpoFetch(INVOICE_REF_3, UPO_BYTES_3);

            List<Invoice> invoices = List.of(
                    Fa3InvoiceFixtures.minimalValid(),
                    Fa3InvoiceFixtures.minimalValid(),
                    Fa3InvoiceFixtures.minimalValid());

            BatchResult result = client.invoices().batch().submit(FormCode.FA3, invoices, FAST_OPTIONS);

            assertEquals(BATCH_REF, result.sessionRef());
            assertEquals(EXPECTED_INVOICE_COUNT, result.totalCount());
            assertEquals(EXPECTED_INVOICE_COUNT, result.successfulCount());
            assertEquals(0, result.failedCount());
            assertEquals(EXPECTED_INVOICE_COUNT, result.cleared().size());
            assertTrue(result.failed().isEmpty());
            assertNotNull(result.processingStartedAt());
            assertNotNull(result.processingCompletedAt());
            assertFalse(result.processingCompletedAt().isBefore(result.processingStartedAt()));
        }
    }

    @Test
    void submitBatch_whenOneInvoiceFails_reportsBreakdown(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpen(wmInfo.getHttpPort());
            stubPartUploadOk();
            stubBatchClose();
            stubStatusOkAfterClose();
            stubInvoicesPage(invoicesAllSuccess());
            stubFailedInvoicesPage(singleFailedInvoicePage());
            stubUpoFetch(INVOICE_REF_1, UPO_BYTES_1);
            stubUpoFetch(INVOICE_REF_2, UPO_BYTES_2);

            List<Invoice> invoices = List.of(
                    Fa3InvoiceFixtures.minimalValid(),
                    Fa3InvoiceFixtures.minimalValid(),
                    Fa3InvoiceFixtures.minimalValid());

            BatchResult result = client.invoices().batch().submit(FormCode.FA3, invoices, FAST_OPTIONS);

            assertEquals(EXPECTED_CLEARED_PARTIAL, result.successfulCount());
            assertEquals(EXPECTED_FAILED_FAILURE, result.failedCount());
            assertEquals(EXPECTED_CLEARED_PARTIAL, result.cleared().size());
            assertEquals(EXPECTED_FAILED_FAILURE, result.failed().size());
            assertEquals(INVOICE_REF_3, result.failed().get(0).invoiceRef());
            assertNotNull(result.failed().get(0).error());
        }
    }

    private static String invoicesAllSuccess() {
        return """
                {
                  "totalCount": 3,
                  "invoices": [
                    {"ordinalNumber":1,"invoiceNumber":"FV/1","ksefNumber":"5265877635-20250826-0100001AF629-AF","referenceNumber":"%s","invoiceFileName":"invoice-1.xml",
                     "acquisitionDate":"2026-05-09T12:00:00+00:00","invoicingDate":"2026-05-09T12:00:00+00:00",
                     "status":{"code":200,"description":"Accepted"}},
                    {"ordinalNumber":2,"invoiceNumber":"FV/2","ksefNumber":"5265877635-20250826-0100001AF629-AF","referenceNumber":"%s","invoiceFileName":"invoice-2.xml",
                     "acquisitionDate":"2026-05-09T12:00:00+00:00","invoicingDate":"2026-05-09T12:00:00+00:00",
                     "status":{"code":200,"description":"Accepted"}},
                    {"ordinalNumber":3,"invoiceNumber":"FV/3","ksefNumber":"5265877635-20250826-0100001AF629-AF","referenceNumber":"%s","invoiceFileName":"invoice-3.xml",
                     "acquisitionDate":"2026-05-09T12:00:00+00:00","invoicingDate":"2026-05-09T12:00:00+00:00",
                     "status":{"code":200,"description":"Accepted"}}
                  ]
                }""".formatted(INVOICE_REF_1, INVOICE_REF_2, INVOICE_REF_3);
    }

    private static String singleFailedInvoicePage() {
        return """
                {
                  "totalCount": 1,
                  "invoices": [
                    {"ordinalNumber":3,"invoiceNumber":"FV/3","referenceNumber":"%s","invoiceFileName":"invoice-3.xml",
                     "acquisitionDate":"2026-05-09T12:00:00+00:00","invoicingDate":"2026-05-09T12:00:00+00:00",
                     "status":{"code":21405,"description":"Validation failed","details":["nip checksum invalid"]}}
                  ]
                }""".formatted(INVOICE_REF_3);
    }

    private static String emptyInvoicesPage() {
        return """
                {"totalCount":0,"invoices":[]}""";
    }

    private static void stubBatchOpen(int httpPort) {
        String body = """
                {
                  "referenceNumber": "%s",
                  "partUploadRequests": [
                    {"ordinalNumber":1,"method":"PUT","url":"%s","headers":{}}
                  ]
                }""".formatted(BATCH_REF, UPLOAD_URL.formatted(httpPort));
        stubFor(post(urlEqualTo(BATCH_PATH))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(body)));
    }

    private static void stubPartUploadOk() {
        stubFor(put(urlMatching("/upload/.*"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
    }

    private static void stubBatchClose() {
        stubFor(post(urlEqualTo(CLOSE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
    }

    private static void stubStatusOkAfterClose() {
        stubFor(get(urlEqualTo(SESSIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-05-09T12:00:00+00:00",
                                 "dateUpdated":"2026-05-09T12:01:00+00:00"}""")));
    }

    private static void stubInvoicesPage(String body) {
        stubFor(get(urlMatching(SESSION_INVOICES_PATH + "\\?.*"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(body)));
    }

    private static void stubFailedInvoicesPage(String body) {
        stubFor(get(urlMatching(SESSION_FAILED_PATH + "\\?.*"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(body)));
    }

    private static void stubUpoFetch(String invoiceRef, byte[] upoBytes) {
        String path = SESSION_INVOICES_PATH + "/" + invoiceRef + "/upo";
        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(upoBytes)));
    }

    private static void stubSymmetricKeyEncryptionCert() {
        try {
            io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates testCerts =
                    io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates.generateRsa();
            String certBase64 = java.util.Base64.getEncoder()
                    .encodeToString(testCerts.certificate().getEncoded());
            String validFrom = java.time.OffsetDateTime.now().toString();
            String validTo = java.time.OffsetDateTime.now().plusYears(1).toString();
            stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                            .withBody("""
                                    [{
                                      "certificate": "%s",
                                      "usage": ["KsefTokenEncryption"],
                                      "validFrom": "%s",
                                      "validTo": "%s"
                                    },{
                                      "certificate": "%s",
                                      "usage": ["SymmetricKeyEncryption"],
                                      "validFrom": "%s",
                                      "validTo": "%s"
                                    }]
                                    """.formatted(certBase64, validFrom, validTo,
                                            certBase64, validFrom, validTo))));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to stub SymmetricKeyEncryption cert", ex);
        }
    }
}
