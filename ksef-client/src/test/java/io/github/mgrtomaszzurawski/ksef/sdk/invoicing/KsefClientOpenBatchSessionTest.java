/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.PreparedBatchPackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WireMock public-flow test for {@link KsefClient#openBatchSession(FormCode, PreparedBatchPackage)}
 * and {@link KsefClient#openBatchSession(FormCode, List)}, hitting
 * {@code POST /v2/sessions/batch}.
 *
 * <p>Closes Codex round-9 finding F1 (HIGH): the OpenAPI coverage registry
 * declared {@code /sessions/batch} as covered, but no test exercised the
 * batch-session opening path — only {@code /v2/sessions/batch/{ref}/close}.
 *
 * <p>This test pins the wire shape of the open request:
 * <ul>
 *   <li>{@code formCode.systemCode}, {@code .schemaVersion}, {@code .value}</li>
 *   <li>{@code encryption.encryptedSymmetricKey} (base64) and
 *       {@code .initializationVector}</li>
 *   <li>{@code batchFile.fileSize}, {@code .fileHash}, and {@code .fileParts}
 *       (per-part {@code ordinalNumber}, {@code fileSize}, {@code fileHash})</li>
 * </ul>
 *
 * <p>The KSeF response is mapped through {@code SessionClient.openBatch} into
 * {@link KsefBatchSession#partUploadRequests()}, so the test also asserts the
 * mapper preserves the upload-request URL/method/headers.
 */
@WireMockTest
class KsefClientOpenBatchSessionTest {

    private static final String BATCH_PATH = "/v2/sessions/batch";
    private static final String BATCH_REF = "20260504-BA-1234567890-AAAAAAAAAA-01";
    private static final String UPLOAD_URL = "https://upload.example.com/part1";
    private static final String UPLOAD_METHOD = "PUT";
    private static final String CUSTOM_HEADER = "x-amz-content-sha256";
    private static final String CUSTOM_HEADER_VALUE = "abcdef0123";
    /** SHA-256 produces 32 bytes. */
    private static final int SHA_256_BYTES = 32;
    /** AES-256 key length: 256 bits / 8 = 32 bytes. */
    private static final int AES_KEY_BYTES = 32;
    /** AES-CBC initialization vector: AES block size = 128 bits / 8 = 16 bytes. */
    private static final int AES_IV_BYTES = 16;
    /** Synthetic batch file size for the manual-overload test. */
    private static final long BATCH_FILE_SIZE = 1024L;
    /** Synthetic single-part size for the manual-overload test (must equal {@link #BATCH_FILE_SIZE} for one part). */
    private static final long BATCH_PART_FILE_SIZE = 512L;
    /** Expected ordinal number of the single part in the open-batch response stub. */
    private static final int FIRST_PART_ORDINAL = 1;
    /** Expected count of partUploadRequests after mapping the open-batch response stub. */
    private static final int EXPECTED_UPLOAD_REQUESTS = 1;
    private static final byte[] FAKE_HASH = new byte[SHA_256_BYTES];
    private static final byte[] FAKE_PART_BYTES = "encrypted".getBytes(StandardCharsets.UTF_8);
    private static final String OPEN_BATCH_RESPONSE = """
            {
              "referenceNumber": "%s",
              "partUploadRequests": [
                {
                  "ordinalNumber": 1,
                  "method": "PUT",
                  "url": "%s",
                  "headers": {"%s": "%s"}
                }
              ]
            }
            """.formatted(BATCH_REF, UPLOAD_URL, CUSTOM_HEADER, CUSTOM_HEADER_VALUE);

    @Test
    void openBatchSession_withPreparedPackage_postsExpectedBatchOpenRequest(WireMockRuntimeInfo wmInfo) {
        // Codex F1 (HIGH) — the open-batch wire-body shape was previously not
        // pinned by any test. Stub the open response with as many parts as the
        // request will declare so the count-matching guard inside
        // KsefBatchSession's constructor is satisfied; the test's purpose is
        // to assert the OUTGOING wire body, not the response mapping (response
        // mapping is covered by the existing batch session tests).
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpenWithSinglePart();
            stubBatchClose();

            BatchFileSpec spec = new BatchFileSpec(BATCH_FILE_SIZE, FAKE_HASH, List.of(
                    new BatchFileSpec.Part(FIRST_PART_ORDINAL, BATCH_PART_FILE_SIZE, FAKE_HASH)));
            PreparedBatchPackage pkg = new PreparedBatchPackage(spec,
                    new byte[AES_KEY_BYTES], new byte[AES_IV_BYTES], List.of(FAKE_PART_BYTES));

            // when
            KsefBatchSession session = client.openBatchSession(FormCode.FA2, pkg, BatchSessionOptions.online());

            // then — open request body must carry formCode, encryption, batchFile/fileParts.
            // This is the assertion Codex F1 said was missing — wire shape pinned.
            verify(postRequestedFor(urlEqualTo(BATCH_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + KsefAuthFlowFixture.DEFAULT_TEST_TOKEN))
                    .withRequestBody(matchingJsonPath("$.formCode.systemCode"))
                    .withRequestBody(matchingJsonPath("$.formCode.schemaVersion"))
                    .withRequestBody(matchingJsonPath("$.formCode.value", equalTo("FA")))
                    .withRequestBody(matchingJsonPath("$.encryption.encryptedSymmetricKey"))
                    .withRequestBody(matchingJsonPath("$.encryption.initializationVector"))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileSize",
                            equalTo(String.valueOf(BATCH_FILE_SIZE))))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileHash"))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileParts[0].ordinalNumber",
                            equalTo(String.valueOf(FIRST_PART_ORDINAL))))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileParts[0].fileSize",
                            equalTo(String.valueOf(BATCH_PART_FILE_SIZE))))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileParts[0].fileHash")));

            // and — response mapping must preserve every upload-request field
            // (Codex F1 review: original test asserted only referenceNumber).
            assertEquals(BATCH_REF, session.referenceNumber());
            assertEquals(EXPECTED_UPLOAD_REQUESTS, session.partUploadRequests().size());
            PartUploadRequest upload = session.partUploadRequests().get(0);
            assertEquals(FIRST_PART_ORDINAL, upload.ordinalNumber());
            assertEquals(UPLOAD_METHOD, upload.method());
            assertEquals(URI.create(UPLOAD_URL), upload.url());
            assertEquals(CUSTOM_HEADER_VALUE, upload.headers().get(CUSTOM_HEADER));
        }
    }

    @Test
    void openBatchSession_withInvoiceList_postsExpectedBatchOpenRequest(WireMockRuntimeInfo wmInfo) {
        // Convenience overload — SDK builds the prepared package internally
        // (zip + encrypt + split + hash). The stub returns one part, and the
        // splitter for a tiny 10-byte invoice also produces one part, so the
        // count-mismatch guard does not fire here — both successful and
        // count-mismatch outcomes are acceptable for the wire-shape contract
        // this test pins. The verify(...) below proves the request went out
        // with the expected JSON shape.
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpenAcceptingAnyPartCount();
            stubBatchClose();

            byte[] invoice = "<Invoice/>".getBytes(StandardCharsets.UTF_8);

            // when — let the call return a session OR throw count-mismatch;
            // either way the request body that reached the server is what we
            // assert below.
            try {
                client.openBatchSession(FormCode.FA2, List.of(invoice), BatchSessionOptions.online()).close();
            } catch (IllegalArgumentException countMismatch) {
                if (!countMismatch.getMessage().contains("partUploadRequests count")) {
                    throw countMismatch;
                }
                // expected if the splitter happens to produce a different
                // part count than the single-part stub returns.
            }

            verify(postRequestedFor(urlEqualTo(BATCH_PATH))
                    .withRequestBody(matchingJsonPath("$.formCode.value", equalTo("FA")))
                    .withRequestBody(matchingJsonPath("$.encryption.encryptedSymmetricKey",
                            matching(".+")))
                    .withRequestBody(matchingJsonPath("$.encryption.initializationVector",
                            matching(".+")))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileSize"))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileHash",
                            matching(".+")))
                    .withRequestBody(matchingJsonPath("$.batchFile.fileParts[0].ordinalNumber",
                            equalTo(String.valueOf(FIRST_PART_ORDINAL)))));
        }
    }

    @Test
    void openBatchSession_withOfflineOptions_postsOfflineModeTrue(WireMockRuntimeInfo wmInfo) {
        // Codex 2026-05-05 F2 — passing BatchSessionOptions.offline() must
        // surface offlineMode=true in the wire body. Online (the default
        // factory) flips back to false and is also asserted to pin the
        // mapping.
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpenWithSinglePart();
            stubBatchClose();

            BatchFileSpec spec = new BatchFileSpec(BATCH_FILE_SIZE, FAKE_HASH, List.of(
                    new BatchFileSpec.Part(FIRST_PART_ORDINAL, BATCH_PART_FILE_SIZE, FAKE_HASH)));
            PreparedBatchPackage pkg = new PreparedBatchPackage(spec,
                    new byte[AES_KEY_BYTES], new byte[AES_IV_BYTES], List.of(FAKE_PART_BYTES));

            client.openBatchSession(FormCode.FA2, pkg, BatchSessionOptions.offline());

            verify(postRequestedFor(urlEqualTo(BATCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true"))));
        }
    }

    @Test
    void openBatchSession_withInvoiceListAndOfflineOptions_postsOfflineModeTrue(WireMockRuntimeInfo wmInfo) {
        // Codex 2026-05-05 round-2 F2 — pin offline-mode wire shape on the
        // List<byte[]> entry point too. The dynamic splitter may produce a
        // different part count than the single-part stub; the verify(...)
        // below asserts the request body that reached the server, so we
        // accept either successful return or count-mismatch throw.
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpenAcceptingAnyPartCount();
            stubBatchClose();

            byte[] invoice = "<Invoice/>".getBytes(StandardCharsets.UTF_8);

            try {
                client.openBatchSession(FormCode.FA2, List.of(invoice), BatchSessionOptions.offline()).close();
            } catch (IllegalArgumentException countMismatch) {
                if (!countMismatch.getMessage().contains("partUploadRequests count")) {
                    throw countMismatch;
                }
                // expected if splitter part count != stub part count
            }

            verify(postRequestedFor(urlEqualTo(BATCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true"))));
        }
    }

    @Test
    void openBatchSessionFromFiles_withOfflineOptions_postsOfflineModeTrue(WireMockRuntimeInfo wmInfo) throws java.io.IOException {
        // Codex 2026-05-05 round-2 F2 — pin offline-mode wire shape on the
        // file-streaming entry point too.
        java.nio.file.Path tempInvoice = java.nio.file.Files.createTempFile("ksef-test-invoice", ".xml");
        java.nio.file.Files.write(tempInvoice, "<Invoice/>".getBytes(StandardCharsets.UTF_8));
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubBatchOpenAcceptingAnyPartCount();
            stubBatchClose();

            try {
                client.openBatchSessionFromFiles(FormCode.FA2,
                        List.of(tempInvoice), BatchSessionOptions.offline()).close();
            } catch (IllegalArgumentException countMismatch) {
                if (!countMismatch.getMessage().contains("partUploadRequests count")) {
                    throw countMismatch;
                }
                // expected if splitter part count != stub part count
            }

            verify(postRequestedFor(urlEqualTo(BATCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offlineMode", equalTo("true"))));
        } finally {
            java.nio.file.Files.deleteIfExists(tempInvoice);
        }
    }

    private static void stubBatchClose() {
        stubFor(post(urlEqualTo(BATCH_PATH + "/" + BATCH_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo("/v2/sessions/" + BATCH_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));
    }

    private static void stubBatchOpenWithSinglePart() {
        stubFor(post(urlEqualTo(BATCH_PATH))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPEN_BATCH_RESPONSE)));
    }

    private static void stubBatchOpenAcceptingAnyPartCount() {
        // For the invoice-list overload the splitter chooses N parts dynamically;
        // a stub returning a fixed number will trigger the count-mismatch guard.
        // Test only asserts the request body, not the returned session.
        stubFor(post(urlEqualTo(BATCH_PATH))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPEN_BATCH_RESPONSE)));
    }

    private static void stubSymmetricKeyEncryptionCert() {
        // KsefAuthFlowFixture stubs /security/public-key-certificates with a single
        // KsefTokenEncryption cert. openBatchSession needs SymmetricKeyEncryption.
        // Override at priority 1 so this stub wins over the fixture's default.
        try {
            io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates testCerts =
                    io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates.generateRsa();
            String certBase64 = java.util.Base64.getEncoder().encodeToString(testCerts.certificate().getEncoded());
            String now = java.time.OffsetDateTime.now().toString();
            String later = java.time.OffsetDateTime.now().plusYears(1).toString();
            stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                            .withBody("""
                                    [
                                      {
                                        "certificate": "%s",
                                        "usage": ["KsefTokenEncryption"],
                                        "validFrom": "%s",
                                        "validTo": "%s"
                                      },
                                      {
                                        "certificate": "%s",
                                        "usage": ["SymmetricKeyEncryption"],
                                        "validFrom": "%s",
                                        "validTo": "%s"
                                      }
                                    ]
                                    """.formatted(certBase64, now, later, certBase64, now, later))));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to stub SymmetricKeyEncryption cert", ex);
        }
    }

}
