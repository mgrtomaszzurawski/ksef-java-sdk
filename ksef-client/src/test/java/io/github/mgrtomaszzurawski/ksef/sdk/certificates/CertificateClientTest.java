/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class CertificateClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_ENROLLMENT_REF = "20260404-CE-1111111111-ABCDEF1234-08";
    private static final String TEST_CERT_SERIAL = "ABC123DEF456";
    private static final String TEST_CERT_NAME = "Test AuthSessions Certificate";
    private static final byte[] TEST_CSR = new byte[]{0x30, 0x42};
    private static final int KSEF_STATUS_OK = 200;
    private static final String CREDENTIALS_TOKEN = "test-token";
    private static final String CREDENTIALS_NIP = "1111111111";
    private static final String PATH_ENROLLMENTS = "/v2/certificates/enrollments";
    private static final String COMMON_NAME = "KSeF Certificate";
    private static final String COUNTRY_NAME = "PL";

    private static final String LIMITS_RESPONSE = """
            {
              "canRequest": true,
              "enrollment": {"limit": 5, "used": 1},
              "certificate": {"limit": 10, "used": 2}
            }
            """;

    private static final String ENROLLMENT_DATA_RESPONSE = """
            {
              "commonName": "KSeF Certificate",
              "countryName": "PL"
            }
            """;

    private static final String ENROLL_RESPONSE = """
            {
              "referenceNumber": "%s",
              "timestamp": "2026-04-04T12:00:00+02:00"
            }
            """.formatted(TEST_ENROLLMENT_REF);

    private static final String ENROLLMENT_STATUS_RESPONSE = """
            {
              "requestDate": "2026-04-04T12:00:00+02:00",
              "status": {"code": 200, "description": "Completed"}
            }
            """;

    private static final String QUERY_RESPONSE = """
            {
              "certificates": [],
              "hasMore": false
            }
            """;

    private static final String RETRIEVE_RESPONSE = """
            {
              "certificates": []
            }
            """;

    /** Terminal-state status response with a populated serialNumber — drives the workflow's poll loop to success. */
    private static final String ENROLLMENT_STATUS_TERMINAL_RESPONSE = """
            {
              "requestDate": "2026-04-04T12:00:00+02:00",
              "status": {"code": 200, "description": "Completed"},
              "certificateSerialNumber": "%s"
            }
            """.formatted(TEST_CERT_SERIAL);

    /** Retrieve response template — actual DER bytes filled in per-test from a freshly-generated cert. */
    private static final String RETRIEVE_WITH_CERT_TEMPLATE = """
            {
              "certificates": [
                {
                  "certificate": "%s",
                  "certificateName": "%s",
                  "certificateSerialNumber": "%s",
                  "certificateType": "Authentication"
                }
              ]
            }
            """;

    /** Non-terminal status (polling) for timeout-path coverage. */
    private static final String ENROLLMENT_STATUS_PENDING_RESPONSE = """
            {
              "requestDate": "2026-04-04T12:00:00+02:00",
              "status": {"code": 100, "description": "Processing"}
            }
            """;

    private static final int RSA_TEST_KEY_SIZE = 2048;
    private static final String RSA_ALGORITHM = "RSA";
    private static final Duration WORKFLOW_QUICK_TIMEOUT = Duration.ofSeconds(5);
    /** Below the 100 ms KsefAsync minimum poll cadence — the workflow times out after one or two polls. */
    private static final Duration WORKFLOW_INSTANT_TIMEOUT = Duration.ofMillis(150);

    @Test
    void getLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/certificates/limits"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(LIMITS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            CertificateLimits response = ksef.certificates().getLimits();

            // then
            assertTrue(response.canRequest());
            assertNotNull(response.enrollment());
            assertNotNull(response.certificate());
        }
    }

    @Test
    void retrieve_whenAuthenticated_returnsCertificates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/certificates/retrieve"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(RETRIEVE_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            RetrieveCertificatesResult response =
                    ksef.certificates().retrieve(List.of(CertificateSerialNumber.parse(TEST_CERT_SERIAL)));

            // then
            assertNotNull(response.certificates());
        }
    }

    @Test
    void revoke_whenAuthenticated_sendsRevokeRequest(WireMockRuntimeInfo wmInfo) {
        // given
        String revokePath = "/v2/certificates/" + TEST_CERT_SERIAL + "/revoke";
        stubFor(post(urlEqualTo(revokePath))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.certificates().revoke(CertificateSerialNumber.parse(TEST_CERT_SERIAL), CertificateRevocationReason.UNSPECIFIED);

            // then
            verify(postRequestedFor(urlEqualTo(revokePath))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void query_whenAuthenticated_returnsCertificates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/certificates/query"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            CertificateQueryResult response =
                    ksef.certificates().queryCertificates(CertificateQueryBuilder.create().build());

            // then
            assertNotNull(response.certificates());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void requestNewCertificate_whenAuthenticatedAndPollSucceeds_returnsRetrievedCertificate(WireMockRuntimeInfo wmInfo) {
        // given — stub the 4-stage workflow: getEnrollmentData → enroll → poll terminal → retrieve
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/data"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_DATA_RESPONSE)));
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLL_RESPONSE)));
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_STATUS_TERMINAL_RESPONSE)));
        byte[] freshCertDer = freshSelfSignedCertDer();
        String retrieveBody = RETRIEVE_WITH_CERT_TEMPLATE.formatted(
                Base64.getEncoder().encodeToString(freshCertDer),
                TEST_CERT_NAME,
                TEST_CERT_SERIAL);
        stubFor(post(urlEqualTo("/v2/certificates/retrieve"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(retrieveBody)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when — full workflow happy path
            KeyPair keyPair = newTestKeyPair();
            io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate result =
                    ksef.certificates().requestNewCertificate(keyPair, WORKFLOW_QUICK_TIMEOUT);

            // then — the freshly-enrolled cert is surfaced with the polled serial
            assertNotNull(result);
            assertEquals(TEST_CERT_SERIAL, result.certificateSerialNumber().value());
            assertEquals(TEST_CERT_NAME, result.certificateName());
            // and — workflow exercised every endpoint exactly once
            verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlEqualTo(PATH_ENROLLMENTS + "/data")));
            verify(postRequestedFor(urlEqualTo(PATH_ENROLLMENTS)));
            verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF)));
            verify(postRequestedFor(urlEqualTo("/v2/certificates/retrieve")));
        }
    }

    @Test
    void requestNewCertificate_whenPollNeverReachesTerminal_throwsTimeout(WireMockRuntimeInfo wmInfo) {
        // given — enrollment endpoints respond but the polled status stays non-terminal
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/data"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_DATA_RESPONSE)));
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLL_RESPONSE)));
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_STATUS_PENDING_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when / then — polling exceeds the workflow timeout
            KeyPair keyPair = newTestKeyPair();
            assertThrows(KsefAsyncTimeoutException.class,
                    () -> ksef.certificates().requestNewCertificate(keyPair, WORKFLOW_INSTANT_TIMEOUT));
        }
    }

    @Test
    void requestNewCertificate_whenEnrollServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — getEnrollmentData succeeds but the enroll POST returns 500
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/data"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_DATA_RESPONSE)));
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("{\"exceptionCode\":21405,\"message\":\"validation failed\"}")));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when / then — server error on enroll surfaces as KsefException (subclass)
            KeyPair keyPair = newTestKeyPair();
            assertThrows(KsefException.class,
                    () -> ksef.certificates().requestNewCertificate(keyPair, WORKFLOW_QUICK_TIMEOUT));
        }
    }

    /**
     * Build a fresh RSA-2048 key pair for workflow tests. Keys are not persisted —
     * the workflow only uses {@link KeyPair#getPublic()} for the CSR and
     * {@link KeyPair#getPrivate()} to sign the CSR; neither leaves the test.
     */
    private static KeyPair newTestKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(RSA_TEST_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new IllegalStateException("RSA not available on this JVM", missingAlgorithm);
        }
    }

    /**
     * Generate a fresh self-signed cert DER for the retrieve-stage stub.
     * {@link TestCertificates} returns a fully-parseable X.509 cert; the
     * workflow's {@code RetrievedCertificate.from} factory parses these
     * bytes through {@code CertificateFactory.getInstance("X.509")} so
     * the response body must carry a valid certificate.
     */
    private static byte[] freshSelfSignedCertDer() {
        try {
            return TestCertificates.generateRsa().certificate().getEncoded();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to generate self-signed cert for test fixture", failure);
        }
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1111111111");
    }
}
