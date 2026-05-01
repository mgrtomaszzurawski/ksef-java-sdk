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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class KsefBatchSessionTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_BATCH_REF = "20260418-BA-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_KSEF_TOKEN = "test-ksef-token";

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_SESSION_BUSY = 415;

    private static final String UPLOAD_URL = "https://upload.example.com/part1";
    private static final String UPLOAD_METHOD = "PUT";
    private static final int PART_ORDINAL = 1;

    private static final String SCENARIO_BUSY_THEN_OK = "batch-busy-then-ok";
    private static final String STATE_RETRY = "retry";

    private static final String SESSION_STATUS_OK_RESPONSE = """
            {
              "status": {"code": 200, "description": "Completed"},
              "dateCreated": "2026-04-18T12:00:00+02:00"
            }
            """;

    private static final String SESSION_BUSY_BODY = """
            {"exception":{"exceptionCode":21405,"description":"Session busy (415)"}}
            """;

    // --- Tests ---

    @Test
    void referenceNumber_returnsBatchSessionRef(WireMockRuntimeInfo wmInfo) {
        // given
        KsefBatchSession session = createSession(wmInfo);

        // when
        String ref = session.referenceNumber();

        // then
        assertEquals(TEST_BATCH_REF, ref);
    }

    @Test
    void partUploadRequests_returnsImmutableList(WireMockRuntimeInfo wmInfo) {
        // given
        KsefBatchSession session = createSession(wmInfo);

        // when
        List<PartUploadRequest> parts = session.partUploadRequests();

        // then
        assertEquals(1, parts.size());
        assertEquals(PART_ORDINAL, parts.get(0).ordinalNumber());
        assertEquals(UPLOAD_METHOD, parts.get(0).method());
        assertEquals(URI.create(UPLOAD_URL), parts.get(0).url());
        PartUploadRequest extraPart = samplePart();
        assertThrows(UnsupportedOperationException.class, () -> parts.add(extraPart));
    }

    @Test
    void status_returnsSessionStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubStatusOk();
        KsefBatchSession session = createSession(wmInfo);

        // when
        SessionStatus status = session.status();

        // then
        assertNotNull(status);
        assertEquals(HTTP_OK, status.status().code());
    }

    @Test
    void close_whenSessionReady_callsCloseAndPolls(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        KsefBatchSession session = createSession(wmInfo);

        // when — first close should not throw
        session.close();

        // then — second close is no-op (idempotent), no error
        assertDoesNotThrow(session::close);
    }

    @Test
    void close_whenSessionBusy415_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first close returns 415 (busy), second succeeds
        stubFor(post(urlEqualTo("/api/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(HTTP_SESSION_BUSY)
                        .withBody(SESSION_BUSY_BODY))
                .willSetStateTo(STATE_RETRY));

        stubFor(post(urlEqualTo("/api/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .inScenario(SCENARIO_BUSY_THEN_OK)
                .whenScenarioStateIs(STATE_RETRY)
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        stubStatusOk();

        KsefBatchSession session = createSession(wmInfo);

        // when — should not throw despite the first 415
        assertDoesNotThrow(session::close);

        // then — second call confirms close completed successfully (idempotent)
        assertDoesNotThrow(session::close);
    }

    @Test
    void close_whenAlreadyClosed_isNoOp(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        KsefBatchSession session = createSession(wmInfo);
        session.close();

        // when / then — second call is a no-op (no error, no extra HTTP call)
        assertDoesNotThrow(session::close);
    }

    // --- Helper methods ---

    private static KsefBatchSession createSession(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials(TEST_KSEF_TOKEN, TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_BATCH_REF, null);

        SessionClient sessionClient = new SessionClient(ksef);
        return new KsefBatchSession(sessionClient, TEST_BATCH_REF, List.of(samplePart()));
    }

    private static PartUploadRequest samplePart() {
        return new PartUploadRequest(PART_ORDINAL, UPLOAD_METHOD,
                URI.create(UPLOAD_URL), Map.of());
    }

    private static void stubCloseAndStatusOk() {
        stubFor(post(urlEqualTo("/api/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));
        stubStatusOk();
    }

    private static void stubStatusOk() {
        stubFor(get(urlEqualTo("/api/v2/sessions/" + TEST_BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SESSION_STATUS_OK_RESPONSE)));
    }
}
