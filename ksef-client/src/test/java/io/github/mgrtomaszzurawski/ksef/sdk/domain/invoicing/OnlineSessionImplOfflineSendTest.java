/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeAll;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class OnlineSessionImplOfflineSendTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String SELLER_NIP = "1111111111";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);

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

    private static KsefCertificate certificate;

    @BeforeAll
    static void initCertificate() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        certificate = new KsefCertificate(pair.certificate(), pair.privateKey());
    }

    @Test
    void sendOfflineInvoice_whenAccepted_postsOfflineModeTrueAndCarriesKodIIQr(WireMockRuntimeInfo wmInfo) {
        // given — wire stubs for invoice POST + status poll
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
        OfflineInvoice offline = OfflineInvoice.fromInvoice(invoice, certificate, OfflineMode.OFFLINE_24,
                new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext(
                        QrEnvironment.TEST, QrContextType.NIP, SELLER_NIP, SELLER_NIP, ISSUE_DATE));
        OnlineSession session = createSession(wmInfo);

        // when
        SubmittedInvoice result = session.sendOfflineInvoice(offline);

        // then — wire body carries offlineMode=true
        verify(postRequestedFor(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true"))));

        // and — KOD I + KOD II propagated through to the SubmittedInvoice
        assertEquals(TEST_INVOICE_REF, result.referenceNumber());
        assertTrue(result.kodIQr().isPresent());
        assertTrue(result.kodIIQr().isPresent());
        assertTrue(result.kodIQr().orElseThrow().length > 0);
        assertTrue(result.kodIIQr().orElseThrow().length > 0);
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
