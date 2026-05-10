/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the synchronous {@code OnlineSession.sendInvoice(Invoice)}
 * pipeline introduced by PR10:
 *
 * <ul>
 *   <li>Submit → poll {@code invoiceStatus} → terminal Accepted →
 *       {@link SubmittedInvoice} carries non-empty {@code ksefNumber}
 *       and {@code kodIQr}; {@code errorDetails} is empty;
 *       {@code invoice} is the original DTO.</li>
 *   <li>Submit → poll → terminal Rejected → {@code errorDetails} is
 *       non-empty; {@code ksefNumber} and {@code kodIQr} are empty.</li>
 * </ul>
 */
@WireMockTest
class SendInvoiceSubmittedTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    /** Spec example KSeF number — passes the CRC-8 checksum validation in {@code KsefNumber}. */
    private static final String TEST_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final byte[] INVOICE_XML = "<Invoice>x</Invoice>".getBytes(StandardCharsets.UTF_8);

    private static final String SEND_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String INVOICE_STATUS_ACCEPTED = """
            {
              "ordinalNumber": 1,
              "invoiceNumber": "FA/001",
              "ksefNumber": "%s",
              "referenceNumber": "%s",
              "invoicingDate": "2026-04-18T12:00:00+02:00",
              "acquisitionDate": "2026-04-18T12:00:01+02:00",
              "status": {"code": 200, "description": "Accepted"}
            }
            """.formatted(TEST_KSEF_NUMBER, TEST_INVOICE_REF);

    private static final String INVOICE_STATUS_REJECTED = """
            {
              "ordinalNumber": 1,
              "invoiceNumber": "FA/001",
              "referenceNumber": "%s",
              "invoicingDate": "2026-04-18T12:00:00+02:00",
              "acquisitionDate": "2026-04-18T12:00:01+02:00",
              "status": {
                "code": 400,
                "description": "Schema rejected",
                "details": ["bad faktura element", "missing required field"]
              }
            }
            """.formatted(TEST_INVOICE_REF);

    @Test
    void sendInvoice_whenAccepted_returnsSubmittedWithKsefNumberAndKodIQr(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        stubInvoiceStatus(INVOICE_STATUS_ACCEPTED);
        stubCloseAndStatusOk();
        Invoice invoice = Invoice.fromXml(FormCode.custom("FA (TEST)", "test", "FA"), INVOICE_XML);

        try (OnlineSession session = createSession(wmInfo)) {

            // when
            SubmittedInvoice submitted = session.sendInvoice(invoice);

            // then — terminal accepted state populates ksefNumber + kodIQr
            assertSame(invoice, submitted.invoice(),
                    "SubmittedInvoice must embed the original Invoice instance");
            assertEquals(TEST_INVOICE_REF, submitted.referenceNumber());
            assertTrue(submitted.ksefNumber().isPresent(),
                    "Accepted invoice must populate ksefNumber");
            assertEquals(TEST_KSEF_NUMBER, submitted.ksefNumber().orElseThrow().value());
            assertTrue(submitted.kodIQr().isPresent(),
                    "Accepted invoice must populate KOD I QR PNG bytes");
            assertTrue(submitted.kodIQr().orElseThrow().length > 0,
                    "QR PNG must be non-empty");
            assertTrue(submitted.errorDetails().isEmpty(),
                    "Accepted invoice must have no error details");
        }
    }

    @Test
    void sendInvoice_whenRejected_returnsSubmittedWithErrorDetails(WireMockRuntimeInfo wmInfo) {
        // given
        stubInvoicePost();
        stubInvoiceStatus(INVOICE_STATUS_REJECTED);
        stubCloseAndStatusOk();
        Invoice invoice = Invoice.fromXml(FormCode.custom("FA (TEST)", "test", "FA"), INVOICE_XML);

        try (OnlineSession session = createSession(wmInfo)) {

            // when
            SubmittedInvoice submitted = session.sendInvoice(invoice);

            // then — terminal rejected state populates errorDetails, no QR / KSeF number
            assertSame(invoice, submitted.invoice());
            assertEquals(TEST_INVOICE_REF, submitted.referenceNumber());
            assertFalse(submitted.ksefNumber().isPresent(),
                    "Rejected invoice must have empty ksefNumber");
            assertFalse(submitted.kodIQr().isPresent(),
                    "Rejected invoice must have empty kodIQr");
            assertEquals(2, submitted.errorDetails().size(),
                    "Rejected invoice must surface server-supplied details");
            assertTrue(submitted.errorDetails().contains("bad faktura element"));
        }
    }

    private static void stubInvoicePost() {
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_RESPONSE)));
    }

    private static void stubInvoiceStatus(String body) {
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF
                + "/invoices/" + TEST_INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(body)));
    }

    private static void stubCloseAndStatusOk() {
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
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        return SessionHandleConstructor.newOnlineSession(
                sessionClient,
                TEST_SESSION_REF,
                CryptoService.generateAesKey(),
                CryptoService.generateIv(),
                null,
                KsefEnvironment.TEST,
                Duration.ofSeconds(2));
    }
}
