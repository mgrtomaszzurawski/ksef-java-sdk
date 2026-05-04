/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.session;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;

@WireMockTest
class SessionClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260404-IN-1234567890-ABCDEF1234-02";
    private static final String TEST_UPO_REF = "20260404-UP-1234567890-ABCDEF1234-03";
    private static final String TEST_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final int KSEF_STATUS_OK = 200;
    private static final String OPEN_ONLINE_RESPONSE = """
            {
              "referenceNumber": "%s",
              "validUntil": "2026-04-04T15:00:00+02:00"
            }
            """.formatted(TEST_SESSION_REF);

    private static final String SEND_INVOICE_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String SESSION_STATUS_RESPONSE = """
            {
              "status": {"code": 200, "description": "Active"},
              "dateCreated": "2026-04-04T12:00:00+02:00"
            }
            """;

    private static final String SESSION_INVOICES_RESPONSE = """
            {
              "invoices": [
                {
                  "referenceNumber": "%s",
                  "ordinalNumber": 1,
                  "invoicingDate": "2026-04-04T12:00:00+02:00",
                  "invoiceHash": "dGVzdA==",
                  "status": {"code": 200, "description": "Processed"}
                }
              ]
            }
            """.formatted(TEST_INVOICE_REF);

    private static final String INVOICE_STATUS_RESPONSE = """
            {
              "ordinalNumber": 1,
              "status": {"code": 200, "description": "Processed"}
            }
            """;

    private static final byte[] TEST_UPO_CONTENT = "<UPO>receipt</UPO>".getBytes(StandardCharsets.UTF_8);
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final String OCTET_STREAM = "application/octet-stream";

    @Test
    void openOnline_whenAuthenticated_returnsSessionReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPEN_ONLINE_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        OnlineSession response = new SessionClient(runtime).openOnline(new OpenOnlineSessionRequestRaw());

        // then
        assertEquals(TEST_SESSION_REF, response.referenceNumber());
        assertNotNull(response.validUntil());
    }

    @Test
    void sendInvoice_whenSessionOpen_returnsInvoiceReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_INVOICE_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        SendInvoiceResult response = new SessionClient(runtime).sendInvoice(
                TEST_SESSION_REF, new SendInvoiceRequest(new byte[]{}, 0L, new byte[]{}, 0L, new byte[]{}, false));

        // then
        assertEquals(TEST_INVOICE_REF, response.referenceNumber());
    }

    @Test
    void closeOnline_whenSessionOpen_sendsPostAndExpectsNoContent(WireMockRuntimeInfo wmInfo) {
        // given
        String closePath = ONLINE_BASE + "/" + TEST_SESSION_REF + "/close";
        stubFor(post(urlEqualTo(closePath))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        new SessionClient(runtime).closeOnline(TEST_SESSION_REF);

        // then
        verify(postRequestedFor(urlEqualTo(closePath))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void getStatus_whenSessionExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSION_STATUS_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        SessionStatus response = new SessionClient(runtime).getStatus(TEST_SESSION_REF);

        // then
        assertEquals(KSEF_STATUS_OK, response.status().code());
        assertNotNull(response.dateCreated());
    }

    @Test
    void getInvoices_whenSessionHasInvoices_returnsInvoiceList(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSION_INVOICES_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        SessionInvoices response = new SessionClient(runtime).getInvoices(TEST_SESSION_REF);

        // then
        assertEquals(1, response.invoices().size());
        assertEquals(TEST_INVOICE_REF, response.invoices().get(0).referenceNumber());
    }

    @Test
    void getInvoiceStatus_whenInvoiceExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/invoices/" + TEST_INVOICE_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(INVOICE_STATUS_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        SessionInvoiceStatus response = new SessionClient(runtime)
                .getInvoiceStatus(TEST_SESSION_REF, TEST_INVOICE_REF);

        // then
        assertEquals(KSEF_STATUS_OK, response.status().code());
    }

    @Test
    void getUpoByReference_whenAvailable_returnsUpoBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF + "/upo/" + TEST_UPO_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, OCTET_STREAM)
                        .withBody(TEST_UPO_CONTENT)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        byte[] upoBytes = new SessionClient(runtime).getUpoByReference(TEST_SESSION_REF, TEST_UPO_REF);

        // then
        assertArrayEquals(TEST_UPO_CONTENT, upoBytes);
    }

    @Test
    void getUpoByKsefNumber_whenAvailable_returnsUpoBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF
                + "/invoices/ksef/" + TEST_KSEF_NUMBER + "/upo"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TEST_UPO_CONTENT)));

        HttpRuntime runtime = activatedRuntime(wmInfo);

        // when
        byte[] upoBytes = new SessionClient(runtime).getUpoByKsefNumber(TEST_SESSION_REF, TEST_KSEF_NUMBER);

        // then
        assertArrayEquals(TEST_UPO_CONTENT, upoBytes);
    }

    @Test
    void openOnline_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given — both the target endpoint and the reauth security endpoint return 401,
        // so after the SDK retries once on 401 the auth exception propagates.
        stubFor(post(urlEqualTo(ONLINE_BASE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));

        HttpRuntime runtime = activatedRuntime(wmInfo);
        SessionClient sessions = new SessionClient(runtime);
        OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw();

        // then
        assertThrows(KsefAuthException.class, () -> sessions.openOnline(request));
    }

    @Test
    void sendInvoice_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody("{}")));

        HttpRuntime runtime = activatedRuntime(wmInfo);
        SessionClient sessions = new SessionClient(runtime);
        SendInvoiceRequest request = new SendInvoiceRequest(new byte[]{}, 0L, new byte[]{}, 0L, new byte[]{}, false);

        // then
        assertThrows(KsefServerException.class,
                () -> sessions.sendInvoice(TEST_SESSION_REF, request));
    }

    private static HttpRuntime activatedRuntime(WireMockRuntimeInfo wmInfo) {
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        return runtime;
    }
}
