/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts the REQ-SESS-41 cap of 10,000 invoices per session.
 *
 * <p>Reflection is used to seed the in-memory counter to the boundary
 * value, so the test does not need to issue 10,000 real WireMock calls.
 * The seam is intentional: the 10k cap is enforced before any HTTP call,
 * so seeding the counter is equivalent to having sent 10k invoices.
 *
 * <p>Covers TC-SESS-007.
 */
@WireMockTest
class KsefSessionInvoiceCapTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1234567890-ABCDEF1234-02";
    private static final byte[] TEST_INVOICE_XML = "<Invoice>test</Invoice>".getBytes(StandardCharsets.UTF_8);
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";
    private static final int MAX_INVOICES = 10_000;

    private static final String SEND_INVOICE_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_INVOICE_REF);

    @Test
    void send_when10kInvoicesAlreadySent_throwsBeforeHttpCall(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — counter pre-seeded to the cap (i.e. 10,000 already sent)
        stubInvoiceEndpoint();
        try (OnlineSession session = createSession(wmInfo)) {
            seedCounter(session, MAX_INVOICES);

            // when / then — the 10001st send is rejected before HTTP
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> session.send(TEST_INVOICE_XML));
            assertTrue(failure.getMessage().contains("10000"),
                    "Error message should reference the cap; got: " + failure.getMessage());
            assertTrue(failure.getMessage().contains("REQ-SESS-41"),
                    "Error message should cite the spec requirement; got: " + failure.getMessage());
        }
    }

    @Test
    void send_when9999InvoicesAlreadySent_acceptsThe10000thAndRejectsThe10001st(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — one short of the cap
        stubInvoiceEndpoint();
        try (OnlineSession session = createSession(wmInfo)) {
            seedCounter(session, MAX_INVOICES - 1);

            // when — 10,000th send succeeds
            assertDoesNotThrow(() -> session.send(TEST_INVOICE_XML));

            // then — 10,001st send fails fast
            assertThrows(IllegalStateException.class,
                    () -> session.send(TEST_INVOICE_XML));
        }
    }

    @Test
    void send_whenRejected_doesNotIncrementBeyondCap(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubInvoiceEndpoint();
        try (OnlineSession session = createSession(wmInfo)) {
            seedCounter(session, MAX_INVOICES);

            // when — invoke many rejected sends
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThrows(IllegalStateException.class, () -> session.send(TEST_INVOICE_XML));
            }

            // then — counter stays at the cap (rollback on rejection per implementation)
            assertEquals(MAX_INVOICES, readCounter(session),
                    "Counter must not drift past cap on rejected sends");
        }
    }

    private static void stubInvoiceEndpoint() {
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/invoices"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SEND_INVOICE_RESPONSE)));
        // try-with-resources triggers session.close() which posts to /close + polls status.
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
        // so close completes cleanly even when the test only exercises send().
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));

        var runtime = io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(sessionClient, TEST_SESSION_REF,
                CryptoService.generateAesKey(), CryptoService.generateIv());
    }

    private static void seedCounter(OnlineSession session, int value) throws Exception {
        AtomicInteger counter = readCounterField(session);
        counter.set(value);
    }

    private static int readCounter(OnlineSession session) throws Exception {
        return readCounterField(session).get();
    }

    private static AtomicInteger readCounterField(OnlineSession session) throws Exception {
        // OnlineSessionImpl is package-private — reach the field via the
        // runtime class rather than a static class literal.
        Field field = session.getClass().getDeclaredField("sentInvoiceCount");
        field.setAccessible(true);
        return (AtomicInteger) field.get(session);
    }
}
