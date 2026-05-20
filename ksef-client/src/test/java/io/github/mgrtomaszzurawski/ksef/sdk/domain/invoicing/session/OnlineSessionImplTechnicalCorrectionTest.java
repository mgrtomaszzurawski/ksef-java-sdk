/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class OnlineSessionImplTechnicalCorrectionTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final int SHA256_LENGTH = 32;

    private static final String SEND_INVOICE_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String INVOICE_STATUS_OK_RESPONSE = """
            {
              "status": {"code": 200, "description": "Accepted"},
              "invoiceNumber": "20260418-FA-1111111111-AAAAAA-FF",
              "ksefNumber": "5265877635-20250826-0100001AF629-AF"
            }
            """;

    @Test
    void sendTechnicalCorrection_whenInvoked_postsHashOfCorrectedInvoiceAndOfflineMode(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_INVOICE_RESPONSE)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF
                + "/invoices/" + TEST_INVOICE_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(INVOICE_STATUS_OK_RESPONSE)));

        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] originalHash = new byte[SHA256_LENGTH];
        for (int i = 0; i < originalHash.length; i++) {
            originalHash[i] = (byte) i;
        }
        OnlineSession session = createSession(wmInfo);

        // when
        SubmittedInvoice result = session.sendTechnicalCorrection(invoice, originalHash);

        // then — body carries offlineMode=true and a hashOfCorrectedInvoice field
        verify(postRequestedFor(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.hashOfCorrectedInvoice")));

        assertEquals(TEST_INVOICE_REF, result.referenceNumber());
    }

    @Test
    void sendTechnicalCorrection_whenHashWrongLength_throwsIllegalArgumentException(WireMockRuntimeInfo wmInfo) {
        // given
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] tooShortHash = new byte[SHA256_LENGTH - 1];
        OnlineSession session = createSession(wmInfo);

        // then
        assertThrows(IllegalArgumentException.class,
                () -> session.sendTechnicalCorrection(invoice, tooShortHash));
    }

    private static OnlineSession createSession(WireMockRuntimeInfo wmInfo) {
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF,
                OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        return SessionHandleConstructor.newOnlineSession(sessionClient, TEST_SESSION_REF,
                aesKey, initVector, null,
                KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"),
                Duration.ofSeconds(30));
    }
}
