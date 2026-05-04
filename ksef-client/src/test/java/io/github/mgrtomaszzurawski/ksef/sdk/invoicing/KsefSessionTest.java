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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
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
    private static final String TEST_SESSION_REF = "20260418-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1234567890-ABCDEF1234-02";
    private static final String TEST_NIP = "1234567890";
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

    @Test
    void send_whenSessionOpen_returnsInvoiceResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_INVOICE_RESPONSE)));
        stubCloseAndStatusOk();

        try (KsefSession session = createSession(wmInfo)) {
            // when
            var result = session.send(TEST_INVOICE_XML);

            // then
            assertNotNull(result);
            assertEquals(TEST_INVOICE_REF, result.referenceNumber());
        }
    }

    @Test
    void send_whenSessionClosed_throwsIllegalStateException(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefSession session = createSession(wmInfo)) {
            session.close();

            // when / then
            assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
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

        try (KsefSession session = createSession(wmInfo)) {
            // when — should not throw despite the first 415
            session.close();

            // then — session is closed, send should fail
            assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
        }
    }

    @Test
    void close_whenAlreadyClosed_isNoOp(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefSession session = createSession(wmInfo)) {
            session.close();

            // when — second close should be a no-op, no error
            session.close();

            // then — still closed
            assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
        }
    }

    @Test
    void referenceNumber_returnsSessionRef(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefSession session = createSession(wmInfo)) {
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

        try (KsefSession session = createSession(wmInfo)) {
            session.close();

            // when
            byte[] upoBytes = session.upo(TEST_INVOICE_REF);

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

        KsefSession session = createSession(wmInfo);

        // when / then
        KsefSessionTerminalFailureException failure =
                assertThrows(KsefSessionTerminalFailureException.class, session::close);
        assertEquals(TERMINAL_FAILURE_CODE, failure.code());
        assertEquals(TERMINAL_FAILURE_DESCRIPTION, failure.description());
        assertEquals(TEST_SESSION_REF, failure.referenceNumber());
    }

    private static KsefSession createSession(WireMockRuntimeInfo wmInfo) {
        io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime runtime =
                io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF,
                java.time.OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(sessionClient, TEST_SESSION_REF, aesKey, initVector);
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
