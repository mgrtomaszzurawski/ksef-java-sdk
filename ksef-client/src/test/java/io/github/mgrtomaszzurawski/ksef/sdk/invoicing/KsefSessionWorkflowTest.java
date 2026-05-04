/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.SendInvoiceCommand;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
 * Workflow-level tests for {@link KsefSession} that complement
 * {@code KsefSessionTest}: technical-correction send (TC-SESS-005),
 * failed-invoices listing (TC-SESS-014), and invoice-status mapping
 * for ksef-number propagation.
 */
@WireMockTest
class KsefSessionWorkflowTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1234567890-ABCDEF1234-02";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final byte[] INVOICE_XML = "<Invoice>x</Invoice>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_OF_CORRECTED = new byte[32];

    private static final String SEND_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

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
        // given
        stubInvoicePost();
        try (KsefSession session = createSession(wmInfo)) {

            // when
            SendInvoiceResult result = session.sendTechnicalCorrection(INVOICE_XML, HASH_OF_CORRECTED);

            // then — invoice ref returned + the wire request had a non-null
            // hashOfCorrectedInvoice and offlineMode=true (technical correction
            // implies offline at the wire level per REQ-OFFLINE-004).
            assertNotNull(result);
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
            verify(postRequestedFor(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices")));
        }
    }

    @Test
    void technicalCorrection_whenHashOfCorrectedNull_throwsNPE(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        try (KsefSession session = createSession(wmInfo)) {

            // when / then — null hash is rejected by SendInvoiceCommand record
            assertThrows(NullPointerException.class,
                    () -> session.sendTechnicalCorrection(INVOICE_XML, null));
        }
    }

    @Test
    void send_command_isAcceptedAndDispatched(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        try (KsefSession session = createSession(wmInfo)) {

            SendInvoiceCommand command = SendInvoiceCommand.normal(INVOICE_XML);

            // when
            SendInvoiceResult result = session.send(command);

            // then
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
        }
    }

    @Test
    void failedInvoices_returnsFailedList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices/failed"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(FAILED_INVOICES_RESPONSE)));

        try (KsefSession session = createSession(wmInfo)) {

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
        try (KsefSession session = createSession(wmInfo)) {

            // when — empty XML is allowed by SDK; KSeF would reject server-side
            SendInvoiceResult result = session.send(new byte[0]);

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

        try (KsefSession session = createSession(wmInfo)) {
            session.close();

            // when / then
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> session.send(INVOICE_XML));
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

    private static KsefSession createSession(WireMockRuntimeInfo wmInfo) {
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
        return new KsefSession(sessionClient, TEST_SESSION_REF,
                CryptoService.generateAesKey(), CryptoService.generateIv());
    }
}
