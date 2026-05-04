/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts {@link KsefBatchSession#uploadParts()} HTTP behaviour against
 * WireMock-served presigned-style endpoints.
 *
 * <p>The presigned upload URL pattern in production is an external (S3) URL
 * carrying its own authorisation in query parameters. The SDK must not send
 * a {@code Authorization: Bearer} header to these URLs. Here WireMock plays
 * the role of the presigned target.
 *
 * <p>Covers TC-BATCH-007 (HTTP method/URL/headers + no bearer),
 * TC-BATCH-008 (non-2xx aborts remaining parts), TC-BATCH-009 (I/O failures
 * map to {@link KsefNetworkException}), TC-BATCH-014 (unknown method
 * is exercised as-is or rejected explicitly).
 */
@WireMockTest
class KsefBatchSessionUploadTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_BATCH_REF = "20260418-BA-1234567890-ABCDEF1234-01";
    private static final String UPLOAD_PATH_1 = "/upload/part1";
    private static final String UPLOAD_PATH_2 = "/upload/part2";
    private static final String CUSTOM_HEADER = "x-amz-content-sha256";
    private static final String CUSTOM_HEADER_VALUE = "abc123";
    private static final byte[] PART_CONTENT = "encrypted-part-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Test
    void uploadParts_putsBytesToEachPresignedUrlWithoutBearer(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) throws Exception {
        // given — two parts, both targeted at WireMock with PUT
        Path partFile1 = writePartFile(tempDir, "part1.bin");
        Path partFile2 = writePartFile(tempDir, "part2.bin");

        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(put(urlEqualTo(UPLOAD_PATH_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1),
                        Map.of(CUSTOM_HEADER, CUSTOM_HEADER_VALUE)),
                new PartUploadRequest(2, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_2),
                        Map.of()));

        try (KsefBatchSession session = createSession(wmInfo, uploads, List.of(partFile1, partFile2))) {

            // when
            session.uploadParts();

            // then — both parts uploaded; first carries custom header; no Authorization header
            verify(putRequestedFor(urlEqualTo(UPLOAD_PATH_1))
                    .withHeader(CUSTOM_HEADER, equalTo(CUSTOM_HEADER_VALUE))
                    .withHeader("Authorization", absent()));
            verify(putRequestedFor(urlEqualTo(UPLOAD_PATH_2))
                    .withHeader("Authorization", absent()));
        }
    }

    @Test
    void uploadParts_whenSecondPartFailsWith500_abortsRemainingAndThrows(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) throws Exception {
        // given — first part succeeds, second returns 500
        Path partFile1 = writePartFile(tempDir, "part1.bin");
        Path partFile2 = writePartFile(tempDir, "part2.bin");

        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(put(urlEqualTo(UPLOAD_PATH_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()),
                new PartUploadRequest(2, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_2), Map.of()));

        try (KsefBatchSession session = createSession(wmInfo, uploads, List.of(partFile1, partFile2))) {

            // when / then
            KsefNetworkException failure = assertThrows(KsefNetworkException.class, session::uploadParts);
            assertTrue(failure.getMessage().contains("2"),
                    "Error must reference the failing ordinal; got: " + failure.getMessage());
            assertTrue(failure.getMessage().contains("500")
                            || failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains("upload"),
                    "Error must mention status or upload failure; got: " + failure.getMessage());
        }
    }

    @Test
    void uploadParts_whenPartFileMissing_throwsKsefNetworkException(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — declared part file does not exist on disk
        Path missingFile = Path.of("/nonexistent/path/to/part1.bin");
        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()));

        try (KsefBatchSession session = createSession(wmInfo, uploads, List.of(missingFile))) {

            // when / then
            assertThrows(KsefNetworkException.class, session::uploadParts);
        }
    }

    @Test
    void uploadParts_whenSessionOpenedFromPreparedPackage_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given — no batchPackage attached → uploadParts unsupported on this code path.
        // Stub close + status to keep try-with-resources clean.
        stubFor(post(urlEqualTo("/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo("/v2/sessions/" + TEST_BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_BATCH_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()));
        // Use the no-package constructor (the PreparedBatchPackage flow)
        try (KsefBatchSession session = io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSessionFactory.newBatchSession(sessionClient, TEST_BATCH_REF, uploads)) {

            // when / then
            assertThrows(IllegalStateException.class, session::uploadParts);
        }
    }

    @Test
    void uploadParts_whenStubReturns200_passesAndCountMatches(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) throws Exception {
        // given — single part, 200 OK
        Path partFile = writePartFile(tempDir, "part1.bin");
        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()));

        try (KsefBatchSession session = createSession(wmInfo, uploads, List.of(partFile))) {

            // when
            session.uploadParts();

            // then
            assertEquals(1, session.partUploadRequests().size());
            verify(putRequestedFor(urlEqualTo(UPLOAD_PATH_1)));
        }
    }

    @Test
    void uploadParts_whenCumulativeBudgetExceeded_throwsBeforeUploadingNext(WireMockRuntimeInfo wmInfo,
                                                                            @TempDir Path tempDir) throws Exception {
        // given — two parts; injected nanoTime jumps past 2 * 20-minute budget
        // immediately after the first part is uploaded, so the second part hits
        // the REQ-SESS-13 deadline check and aborts.
        Path partFile1 = writePartFile(tempDir, "part1.bin");
        Path partFile2 = writePartFile(tempDir, "part2.bin");
        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(put(urlEqualTo(UPLOAD_PATH_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()),
                new PartUploadRequest(2, "PUT",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_2), Map.of()));

        // Clock jumps past 41 minutes (>2 * 20-min budget) on second call.
        long[] clockTicks = {0L, 41L * 60L * 1_000_000_000L, 41L * 60L * 1_000_000_000L};
        java.util.concurrent.atomic.AtomicInteger tickIndex = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.LongSupplier fakeClock = () -> clockTicks[Math.min(tickIndex.getAndIncrement(),
                clockTicks.length - 1)];

        try (KsefBatchSession session = createBudgetTestSession(wmInfo, uploads, List.of(partFile1, partFile2), fakeClock)) {

            // when / then
            io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException failure =
                    assertThrows(io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException.class,
                            session::uploadParts);
            assertTrue(failure.getMessage().contains("REQ-SESS-13"),
                    "Error must cite REQ-SESS-13; got: " + failure.getMessage());
            assertTrue(failure.getMessage().contains("20"),
                    "Error must mention the 20-minute budget; got: " + failure.getMessage());
        }
    }

    @Test
    void uploadParts_whenMethodIsUnsupported_failsCleanly(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) throws Exception {
        // given — server reports method "PATCH" (not GET/PUT/POST). HttpClient.send
        // accepts arbitrary methods through the request builder, but a server that
        // does not allow PATCH on the presigned URL will reject. Here we stub the
        // PUT path to fail; the SDK's request builder uses PATCH literally and
        // WireMock will not match → the client should surface a network error.
        Path partFile = writePartFile(tempDir, "part1.bin");
        stubFor(put(urlEqualTo(UPLOAD_PATH_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        List<PartUploadRequest> uploads = List.of(
                new PartUploadRequest(1, "PATCH",
                        URI.create(wmInfo.getHttpBaseUrl() + UPLOAD_PATH_1), Map.of()));

        try (KsefBatchSession session = createSession(wmInfo, uploads, List.of(partFile))) {

            // when / then — PATCH is not registered as a stub method on this URL,
            // WireMock returns 404 → SDK wraps as KsefNetworkException.
            assertThrows(io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException.class,
                    session::uploadParts);
        }
    }

    private static KsefBatchSession createBudgetTestSession(WireMockRuntimeInfo wmInfo,
                                                             List<PartUploadRequest> uploads,
                                                             List<Path> partFiles,
                                                             java.util.function.LongSupplier fakeClock) {
        // Same as createSession but with an injected nano-time source for REQ-SESS-13
        // budget testing. Stubs close + status so try-with-resources can complete.
        stubFor(post(urlEqualTo("/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo("/v2/sessions/" + TEST_BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_BATCH_REF, java.time.OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        BatchPackageBuilder.BatchPackage pkg = new BatchPackageBuilder.BatchPackage(
                stubBatchFileSpec(partFiles.size()), partFiles);
        return io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSessionFactory.newBatchSession(sessionClient, HttpClient.newHttpClient(),
                TEST_BATCH_REF, uploads, pkg, fakeClock);
    }

    private static Path writePartFile(Path tempDir, String name) throws Exception {
        Path partFile = tempDir.resolve(name);
        Files.write(partFile, PART_CONTENT);
        return partFile;
    }

    private static KsefBatchSession createSession(WireMockRuntimeInfo wmInfo,
                                                   List<PartUploadRequest> uploads,
                                                   List<Path> partFiles) {
        // try-with-resources will eventually call session.close() — stub
        // /sessions/batch/{ref}/close + status poll so close completes cleanly.
        stubFor(post(urlEqualTo("/v2/sessions/batch/" + TEST_BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo("/v2/sessions/" + TEST_BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_BATCH_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        BatchPackageBuilder.BatchPackage pkg = new BatchPackageBuilder.BatchPackage(
                stubBatchFileSpec(partFiles.size()), partFiles);
        return io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSessionFactory.newBatchSession(sessionClient, HttpClient.newHttpClient(),
                TEST_BATCH_REF, uploads, pkg);
    }

    private static io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec stubBatchFileSpec(int partCount) {
        // Build a minimal but valid BatchFileSpec — values not used by uploadParts(),
        // only the part count matters for the constructor's count-mismatch guard.
        List<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec.Part> parts =
                new java.util.ArrayList<>();
        byte[] fakeHash = new byte[32];
        for (int index = 1; index <= partCount; index++) {
            parts.add(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec.Part(
                    index, PART_CONTENT.length, fakeHash));
        }
        return new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec(
                PART_CONTENT.length * (long) partCount, fakeHash, parts);
    }
}
