/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workflow-level tests for {@link OnlineSession} that complement
 * {@code KsefSessionTest}: technical-correction send (TC-SESS-005),
 * failed-invoices listing (TC-SESS-014), and invoice-status mapping
 * for ksef-number propagation.
 */
@WireMockTest
class KsefSessionWorkflowTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final byte[] INVOICE_XML = "<Invoice>x</Invoice>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_OF_CORRECTED = new byte[32];

    /** Distinct from {@link FormCode#FA3} so InvoiceValidationGate skips XSD validation. */
    private static final FormCode TEST_FORM_CODE = FormCode.custom("FA (TEST)", "test", "FA");

    private static final String SEND_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String TEST_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final String INVOICE_STATUS_ACCEPTED_RESPONSE = """
            {
              "ordinalNumber": 1,
              "invoiceNumber": "FA/001",
              "ksefNumber": "%s",
              "referenceNumber": "%s",
              "invoiceHash": "ZHVtbXktaGFzaA==",
              "invoicingDate": "2026-04-18T12:00:00+02:00",
              "acquisitionDate": "2026-04-18T12:00:01+02:00",
              "status": {"code": 200, "description": "Accepted"}
            }
            """.formatted(TEST_KSEF_NUMBER, TEST_INVOICE_REF);

    private static final String FAILED_INVOICES_RESPONSE = """
            {
              "invoices": [
                {
                  "referenceNumber": "20260418-IN-FAIL-AAAAAAAAAA-FF",
                  "ordinalNumber": 1,
                  "invoicingDate": "2026-04-18T12:00:00+02:00",
                  "invoiceHash": "ZmFpbGhhc2g=",
                  "status": {"code": 415, "description": "Schema rejected"}
                }
              ]
            }
            """;

    @Test
    void technicalCorrection_postsCorrectedInvoiceFlag(WireMockRuntimeInfo wmInfo) {
        // given — distinct hash bytes so we can verify the exact base64 payload
        byte[] hash = new byte[32];
        for (int idx = 0; idx < hash.length; idx++) {
            hash[idx] = (byte) (idx + 1);
        }
        String expectedHashBase64 = java.util.Base64.getEncoder().encodeToString(hash);
        stubInvoicePost();
        try (OnlineSession session = createSession(wmInfo)) {

            // when
            SubmittedInvoice result = session.sendTechnicalCorrection(
                    Invoice.fromXml(TEST_FORM_CODE, INVOICE_XML), hash);

            // then — invoice ref returned and the wire body has the right shape:
            //   * offlineMode == true (REQ-OFFLINE-004 — technical correction
            //     implies offline at the wire level),
            //   * hashOfCorrectedInvoice present and equal to the supplied bytes,
            //     base64-encoded.
            assertNotNull(result);
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
            verify(postRequestedFor(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                    .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.hashOfCorrectedInvoice", equalTo(expectedHashBase64))));
        }
    }

    @Test
    void normalSend_omitsHashOfCorrectedInvoice(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        try (OnlineSession session = createSession(wmInfo)) {

            // when
            session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, INVOICE_XML));

            // then — normal send must NOT carry a hashOfCorrectedInvoice key in the body
            verify(postRequestedFor(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                    .withRequestBody(notMatching(".*hashOfCorrectedInvoice.*")));
        }
    }

    @Test
    void technicalCorrection_whenHashOfCorrectedNull_throwsNPE(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        try (OnlineSession session = createSession(wmInfo)) {
            Invoice invoice = Invoice.fromXml(TEST_FORM_CODE, INVOICE_XML);

            // when / then — null hash is rejected by SendInvoiceCommand record
            assertThrows(NullPointerException.class,
                    () -> session.sendTechnicalCorrection(invoice, null));
        }
    }

    @Test
    void technicalCorrection_whenHashOfCorrectedWrongLength_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — SHA-256 must be exactly 32 bytes
        byte[] tooShort = new byte[31];
        byte[] tooLong = new byte[33];
        stubInvoicePost();
        try (OnlineSession session = createSession(wmInfo)) {
            Invoice invoice = Invoice.fromXml(TEST_FORM_CODE, INVOICE_XML);

            // when / then
            assertThrows(IllegalArgumentException.class,
                    () -> session.sendTechnicalCorrection(invoice, tooShort));
            assertThrows(IllegalArgumentException.class,
                    () -> session.sendTechnicalCorrection(invoice, tooLong));
        }
    }

    @Test
    void failedInvoices_returnsFailedList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices/failed?pageSize=250"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(FAILED_INVOICES_RESPONSE)));

        try (OnlineSession session = createSession(wmInfo)) {

            // when
            SessionInvoices failed = session.failedInvoices();

            // then
            assertNotNull(failed);
            assertEquals(1, failed.invoices().size());
            assertEquals(415, failed.invoices().get(0).status().code());
        }
    }

    @Test
    void send_whenInvoiceXmlIsEmpty_stillProducesAValidRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        try (OnlineSession session = createSession(wmInfo)) {

            // when — empty XML is allowed by SDK; KSeF would reject server-side
            SubmittedInvoice result = session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, new byte[0]));

            // then
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
        }
    }

    @Test
    void send_whenSessionAlreadyClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given — close the session via a successful close+status flow
        stubInvoicePost();
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));

        try (OnlineSession session = createSession(wmInfo)) {
            session.close();

            // when / then
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, INVOICE_XML)));
            assertTrue(failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains("closed"),
                    "Expected closed-session error; got: " + failure.getMessage());
        }
    }

    private static void stubInvoicePost() {
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_RESPONSE)));
        // sendInvoice(Invoice) polls /sessions/{ref}/invoices/{invRef} for terminal status.
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices/" + TEST_INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(INVOICE_STATUS_ACCEPTED_RESPONSE)));
        // try-with-resources triggers close() which posts /close and polls status.
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));
    }

    private static OnlineSession createSession(WireMockRuntimeInfo wmInfo) {
        // try-with-resources will eventually call close() — stub /close + status poll
        // so close completes cleanly and tests focus on their assertions.
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(
                sessionClient, TEST_SESSION_REF,
                CryptoService.generateAesKey(), CryptoService.generateIv(),
                null,
                io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment.TEST,
                java.time.Duration.ofSeconds(2));
    }
}
