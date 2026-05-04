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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class KsefBatchSessionTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_BATCH_REF = "20260418-BA-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";
    private static final String UPLOAD_URL = "https://upload.example.com/part1";
    private static final String UPLOAD_METHOD = "PUT";
    private static final int PART_ORDINAL = 1;

    private static final String SCENARIO_BUSY_THEN_OK = "batch-busy-then-ok";
    private static final String STATE_RETRY = "retry";
    private static final String STATE_STARTED = "Started";
    private static final String BATCH_BASE = "/v2/sessions/batch";
    private static final String SESSIONS_BASE = "/v2/sessions";

    private static final String SESSION_STATUS_OK_RESPONSE = """
            {
              "status": {"code": 200, "description": "Completed"},
              "dateCreated": "2026-04-18T12:00:00+02:00"
            }
            """;

    private static final String SESSION_BUSY_BODY = """
            {"exception":{"exceptionCode":21405,"description":"Session busy (415)"}}
            """;

    @Test
    void referenceNumber_returnsBatchSessionRef(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefBatchSession session = createSession(wmInfo)) {
            // when
            String referenceNumber = session.referenceNumber();

            // then
            assertEquals(TEST_BATCH_REF, referenceNumber);
        }
    }

    @Test
    void partUploadRequests_returnsImmutableList(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefBatchSession session = createSession(wmInfo)) {
            // when
            List<PartUploadRequest> parts = session.partUploadRequests();

            // then
            assertEquals(1, parts.size());
            assertEquals(PART_ORDINAL, parts.get(0).ordinalNumber());
            assertEquals(UPLOAD_METHOD, parts.get(0).method());
            assertEquals(URI.create(UPLOAD_URL), parts.get(0).url());
            PartUploadRequest extraPart = samplePart();
            Executable mutationAttempt = () -> parts.add(extraPart);
            assertThrows(UnsupportedOperationException.class, mutationAttempt);
        }
    }

    @Test
    void status_returnsSessionStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefBatchSession session = createSession(wmInfo)) {
            // when
            SessionStatus status = session.status();

            // then
            assertNotNull(status);
            assertEquals(TestHttpConstants.HTTP_OK, status.status().code());
        }
    }

    @Test
    void close_whenSessionReady_callsCloseAndPolls(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefBatchSession session = createSession(wmInfo)) {
            // when — first close should not throw
            session.close();

            // then — second close is no-op (idempotent), no error
            assertDoesNotThrow(session::close);
        }
    }

    @Test
    void close_whenSessionBusy415_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first close returns 415 (busy), second succeeds
        stubFor(post(urlEqualTo(BATCH_BASE + "/" + TEST_BATCH_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs(STATE_STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SESSION_BUSY)
                        .withBody(SESSION_BUSY_BODY))
                .willSetStateTo(STATE_RETRY));

        stubFor(post(urlEqualTo(BATCH_BASE + "/" + TEST_BATCH_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs(STATE_RETRY)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        stubStatusOk();

        try (KsefBatchSession session = createSession(wmInfo)) {
            // when — should not throw despite the first 415
            assertDoesNotThrow(session::close);

            // then — second call confirms close completed successfully (idempotent)
            assertDoesNotThrow(session::close);
        }
    }

    @Test
    void close_whenAlreadyClosed_isNoOp(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        try (KsefBatchSession session = createSession(wmInfo)) {
            session.close();

            // when / then — second call is a no-op (no error, no extra HTTP call)
            assertDoesNotThrow(session::close);
        }
    }

    private static KsefBatchSession createSession(WireMockRuntimeInfo wmInfo) {
        // Direct internal-runtime construction for the unit test of the
        // batch session's part-upload flow. The session reference is
        // pre-fed; no auth round-trip is needed because the batch-session
        // class does not call /auth/* itself — KsefClient orchestrates
        // auth before constructing the session in production.
        io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime runtime =
                io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_BATCH_REF, java.time.OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        return io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSessionFactory.newBatchSession(sessionClient, TEST_BATCH_REF, List.of(samplePart()));
    }

    private static PartUploadRequest samplePart() {
        return new PartUploadRequest(PART_ORDINAL, UPLOAD_METHOD,
                URI.create(UPLOAD_URL), Map.of());
    }

    private static void stubCloseAndStatusOk() {
        stubFor(post(urlEqualTo(BATCH_BASE + "/" + TEST_BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubStatusOk();
    }

    private static void stubStatusOk() {
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(SESSION_STATUS_OK_RESPONSE)));
    }
}
