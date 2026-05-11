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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class KsefSessionTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String TEST_NIP = "1111111111";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";

    private static final String SEND_INVOICE_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String SESSION_STATUS_OK_RESPONSE = """
            {
              "status": {"code": 200, "description": "Completed"},
              "dateCreated": "2026-04-18T12:00:00+02:00"
            }
            """;

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

    private static final int TERMINAL_FAILURE_CODE = 415;
    private static final String TERMINAL_FAILURE_DESCRIPTION = "Schema validation rejected";
    private static final String SESSION_STATUS_TERMINAL_FAILURE_RESPONSE = """
            {
              "status": {"code": 415, "description": "Schema validation rejected", "details": ["bad faktura"]},
              "dateCreated": "2026-04-18T12:00:00+02:00"
            }
            """;

    private static final String SESSION_BUSY_BODY = """
            {"exception":{"exceptionCode":21405,"description":"Session busy (415)"}}
            """;

    private static final byte[] TEST_UPO_CONTENT = "<UPO>receipt</UPO>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TEST_INVOICE_XML = "<Invoice>test</Invoice>".getBytes(StandardCharsets.UTF_8);
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final String SCENARIO_BUSY_THEN_OK = "busy-then-ok";
    private static final String STATE_STARTED = "Started";
    private static final String STATE_RETRY = "retry";
    private static final String OCTET_STREAM = "application/octet-stream";
    /** Distinct from {@link FormCode#FA3} so {@code InvoiceValidationGate} skips the XSD check — these tests gate session lifecycle, not invoice content. */
    private static final FormCode TEST_FORM_CODE = FormCode.custom("FA (TEST)", "test", "FA");

    @Test
    void sendInvoice_whenSessionOpen_returnsSubmittedInvoice(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_INVOICE_RESPONSE)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices/" + TEST_INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(INVOICE_STATUS_ACCEPTED_RESPONSE)));
        stubCloseAndStatusOk();

        try (OnlineSession session = createSession(wmInfo)) {
            // when
            var result = session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, TEST_INVOICE_XML));

            // then
            assertNotNull(result);
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
        }
    }

    @Test
    void send_whenSessionClosed_throwsIllegalStateException(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (OnlineSession session = createSession(wmInfo)) {
            session.close();

            // when / then
            assertThrows(IllegalStateException.class, () -> session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, TEST_INVOICE_XML)));
        }
    }

    @Test
    void close_whenSessionBusy415_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first close returns 415 (busy), second succeeds
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs(STATE_STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SESSION_BUSY)
                        .withBody(SESSION_BUSY_BODY))
                .willSetStateTo(STATE_RETRY));

        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs(STATE_RETRY)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        stubStatusOk();

        try (OnlineSession session = createSession(wmInfo)) {
            // when — should not throw despite the first 415
            session.close();

            // then — session is closed, send should fail
            assertThrows(IllegalStateException.class, () -> session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, TEST_INVOICE_XML)));
        }
    }

    @Test
    void close_whenAlreadyClosed_isNoOp(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (OnlineSession session = createSession(wmInfo)) {
            session.close();

            // when — second close should be a no-op, no error
            session.close();

            // then — still closed
            assertThrows(IllegalStateException.class, () -> session.sendInvoice(Invoice.fromXml(TEST_FORM_CODE, TEST_INVOICE_XML)));
        }
    }

    @Test
    void referenceNumber_returnsSessionRef(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (OnlineSession session = createSession(wmInfo)) {
            // when
            String referenceNumber = session.referenceNumber();

            // then
            assertEquals(TEST_SESSION_REF, referenceNumber);
        }
    }

    @Test
    void upo_afterClose_returnsUpoBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF
                + "/invoices/" + TEST_INVOICE_REF + "/upo"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, OCTET_STREAM)
                        .withBody(TEST_UPO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF
                + "/invoices/" + TEST_INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {
                                  "ordinalNumber": 1,
                                  "invoiceNumber": "FV/1",
                                  "referenceNumber": "%s",
                                  "acquisitionDate": "2026-04-18T12:00:00+02:00",
                                  "invoicingDate": "2026-04-18T12:00:00+02:00",
                                  "status": {"code": 200, "description": "Ok"}
                                }
                                """.formatted(TEST_INVOICE_REF))));

        try (OnlineSession session = createSession(wmInfo)) {
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession closed = session.archive();

            // when
            byte[] upoBytes = closed.cleared(TEST_INVOICE_REF).upo().xmlBytes();

            // then
            assertArrayEquals(TEST_UPO_CONTENT, upoBytes);
        }
    }

    @Test
    void close_whenTerminalStatusNot200_throwsTerminalFailureException(WireMockRuntimeInfo wmInfo) {
        // given — close itself succeeds, but the status poll surfaces a terminal failure
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSION_STATUS_TERMINAL_FAILURE_RESPONSE)));

        OnlineSession session = createSession(wmInfo);

        // when / then
        KsefSessionTerminalFailureException failure =
                assertThrows(KsefSessionTerminalFailureException.class, session::close);
        assertEquals(TERMINAL_FAILURE_CODE, failure.code());
        assertEquals(TERMINAL_FAILURE_DESCRIPTION, failure.description());
        assertEquals(TEST_SESSION_REF, failure.referenceNumber());
    }

    @Test
    void validUntil_returnsValueWhenConstructedWithDeadline() {
        // Codex 2026-05-05 F8a — validUntil should round-trip through the
        // package-private constructor and timeToExpiry should compute
        // duration against the supplied clock.
        java.time.OffsetDateTime deadline = java.time.OffsetDateTime.parse("2026-04-18T13:00:00+02:00");
        OnlineSession session = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor
                .newOnlineSession(null, TEST_SESSION_REF, new byte[0], new byte[0], deadline);

        assertEquals(deadline, session.validUntil().orElseThrow());
        java.time.Clock fixed = java.time.Clock.fixed(
                java.time.OffsetDateTime.parse("2026-04-18T12:00:00+02:00").toInstant(),
                java.time.ZoneOffset.UTC);
        assertEquals(java.time.Duration.ofHours(1),
                session.timeToExpiry(fixed).orElseThrow());
    }

    @Test
    void validUntil_emptyWhenLegacyConstructor() {
        // Legacy 4-arg ctor (no validUntil) → accessor returns empty.
        OnlineSession session = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor
                .newOnlineSession(null, TEST_SESSION_REF, new byte[0], new byte[0]);
        assertEquals(java.util.Optional.empty(), session.validUntil());
        assertEquals(java.util.Optional.empty(), session.timeToExpiry(java.time.Clock.systemUTC()));
    }

    // The byte[] offline-send wire-shape gate moved to
    // OnlineSessionImplOfflineSendTest after N-7 removed raw bytes
    // (sendOffline(byte[]) / send(SendInvoiceCommand)) from the
    // OnlineSession public interface. The typed sendOfflineInvoice(OfflineInvoice)
    // pipeline exercised there asserts the same offlineMode=true /
    // no-hashOfCorrectedInvoice wire invariants (REQ-OFFLINE-004).

    private static OnlineSession createSession(WireMockRuntimeInfo wmInfo) {
        io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime runtime =
                io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF,
                java.time.OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        // Verification-aware constructor — needed by sendInvoice(Invoice) to
        // render KOD I QR and drive the terminal-state poll.
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(
                sessionClient, TEST_SESSION_REF, aesKey, initVector,
                null,
                io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment.TEST,
                java.time.Duration.ofSeconds(2));
    }

    private static void stubCloseAndStatusOk() {
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubStatusOk();
    }

    private static void stubStatusOk() {
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSION_STATUS_OK_RESPONSE)));
    }
}
