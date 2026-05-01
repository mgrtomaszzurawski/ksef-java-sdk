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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
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

@WireMockTest
class KsefSessionTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1234567890-ABCDEF1234-02";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";

    private static final int HTTP_OK = 200;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_SESSION_BUSY = 415;
    private static final int KSEF_STATUS_OK = 200;

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

    private static final String SESSION_BUSY_BODY = """
            {"exception":{"exceptionCode":21405,"description":"Session busy (415)"}}
            """;

    private static final byte[] TEST_UPO_CONTENT = "<UPO>receipt</UPO>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TEST_INVOICE_XML = "<Invoice>test</Invoice>".getBytes(StandardCharsets.UTF_8);

    @Test
    void send_whenSessionOpen_returnsInvoiceResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/sessions/online/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(HTTP_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SEND_INVOICE_RESPONSE)));

        KsefSession session = createSession(wmInfo);

        // when
        var result = session.send(TEST_INVOICE_XML);

        // then
        assertNotNull(result);
        assertEquals(TEST_INVOICE_REF, result.referenceNumber());
    }

    @Test
    void send_whenSessionClosed_throwsIllegalStateException(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        KsefSession session = createSession(wmInfo);
        session.close();

        // when / then
        assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
    }

    @Test
    void close_whenSessionBusy415_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first close returns 415 (busy), second succeeds
        stubFor(post(urlEqualTo("/api/v2/sessions/online/" + TEST_SESSION_REF + "/close"))
                .inScenario("busy-then-ok")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(HTTP_SESSION_BUSY)
                        .withBody(SESSION_BUSY_BODY))
                .willSetStateTo("retry"));

        stubFor(post(urlEqualTo("/api/v2/sessions/online/" + TEST_SESSION_REF + "/close"))
                .inScenario("busy-then-ok")
                .whenScenarioStateIs("retry")
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        stubStatusOk();

        KsefSession session = createSession(wmInfo);

        // when — should not throw despite the first 415
        session.close();

        // then — session is closed, send should fail
        assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
    }

    @Test
    void close_whenAlreadyClosed_isNoOp(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        KsefSession session = createSession(wmInfo);
        session.close();

        // when — second close should be a no-op, no error
        session.close();

        // then — still closed
        assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
    }

    @Test
    void referenceNumber_returnsSessionRef(WireMockRuntimeInfo wmInfo) {
        // given
        KsefSession session = createSession(wmInfo);

        // when
        String ref = session.referenceNumber();

        // then
        assertEquals(TEST_SESSION_REF, ref);
    }

    @Test
    void upo_afterClose_returnsUpoBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        stubFor(get(urlEqualTo("/api/v2/sessions/" + TEST_SESSION_REF
                + "/invoices/" + TEST_INVOICE_REF + "/upo"))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(TEST_UPO_CONTENT)));

        KsefSession session = createSession(wmInfo);
        session.close();

        // when
        byte[] upo = session.upo(TEST_INVOICE_REF);

        // then
        assertArrayEquals(TEST_UPO_CONTENT, upo);
    }

    // --- Helper methods ---

    private static KsefSession createSession(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);

        SessionClient sessionClient = new SessionClient(ksef);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        return new KsefSession(sessionClient, TEST_SESSION_REF, aesKey, initVector);
    }

    private static void stubCloseAndStatusOk() {
        stubFor(post(urlEqualTo("/api/v2/sessions/online/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));
        stubStatusOk();
    }

    private static void stubStatusOk() {
        stubFor(get(urlEqualTo("/api/v2/sessions/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SESSION_STATUS_OK_RESPONSE)));
    }
}
