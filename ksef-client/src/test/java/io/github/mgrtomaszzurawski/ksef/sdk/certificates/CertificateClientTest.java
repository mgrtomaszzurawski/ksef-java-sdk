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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
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
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_ENROLLMENT_REF = "20260404-CE-1234567890-ABCDEF1234-08";
    private static final String TEST_CERT_SERIAL = "ABC123DEF456";
    private static final String TEST_CERT_NAME = "Test Auth Certificate";
    private static final byte[] TEST_CSR = new byte[]{0x30, 0x42};
    private static final int KSEF_STATUS_OK = 200;
    private static final String CREDENTIALS_TOKEN = "test-token";
    private static final String CREDENTIALS_NIP = "1234567890";
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

    /** KSeF terminal status code per CertificateClient — 200 = Completed. */
    private static final int KSEF_STATUS_OK_CODE = 200;
    /**
     * Status body where {@code status.code = 100} (in-progress, below the
     * 200 terminal threshold). The {@code enrollAndAwait} timeout test
     * stubs this so the await loop never reaches a terminal state.
     */
    private static final String ENROLLMENT_IN_PROGRESS_RESPONSE = """
            {
              "requestDate": "2026-04-04T12:00:00+02:00",
              "status": {"code": 100, "description": "Pending"}
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
    void getEnrollmentData_whenAuthenticated_returnsData(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/v2/certificates/enrollments/data"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_DATA_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            CertificateEnrollmentData response = ksef.certificates().getEnrollmentData();

            // then
            assertEquals(COMMON_NAME, response.commonName());
            assertEquals(COUNTRY_NAME, response.countryName());
        }
    }

    @Test
    void enroll_whenAuthenticated_returnsReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLL_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            EnrollCertificateResult response = ksef.certificates().enroll(
                    CertificateEnrollBuilder.create(TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR));

            // then
            assertEquals(TEST_ENROLLMENT_REF, response.referenceNumber());
            assertNotNull(response.timestamp());
        }
    }

    @Test
    void enrollAndAwait_whenStatusReachesTerminal_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given — POST returns enrollment reference; GET status is already terminal (code 200).
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLL_RESPONSE)));
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_STATUS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            CertificateEnrollmentStatus terminal = ksef.certificates().enrollAndAwait(
                    CertificateEnrollBuilder.create(TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR),
                    java.time.Duration.ofSeconds(5));

            assertEquals(KSEF_STATUS_OK_CODE, terminal.status().code());
        }
    }

    @Test
    void enrollAndAwait_whenStatusNeverTerminal_throwsTimeoutWithLastCode(WireMockRuntimeInfo wmInfo) {
        // given — POST succeeds; GET status keeps reporting code 100 (in-progress).
        // The await loop must exhaust the timeout and throw KsefAsyncTimeoutException
        // carrying the last-seen status code (covers the statusCodeOf lambda).
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLL_RESPONSE)));
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_IN_PROGRESS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            CertificateEnrollBuilder builder =
                    CertificateEnrollBuilder.create(TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR);

            assertThrows(io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException.class,
                    () -> ksef.certificates().enrollAndAwait(builder, java.time.Duration.ofMillis(50)));
        }
    }

    @Test
    void getEnrollmentStatus_whenExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_ENROLLMENTS + "/" + TEST_ENROLLMENT_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ENROLLMENT_STATUS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            CertificateEnrollmentStatus response =
                    ksef.certificates().getEnrollmentStatus(TEST_ENROLLMENT_REF);

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertNotNull(response.requestDate());
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
                    ksef.certificates().retrieve(List.of(TEST_CERT_SERIAL));

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
            ksef.certificates().revoke(TEST_CERT_SERIAL, CertificateRevocationReason.UNSPECIFIED);

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
                    ksef.certificates().query(CertificateQueryBuilder.create());

            // then
            assertNotNull(response.certificates());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void enroll_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(PATH_ENROLLMENTS))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody("{}")));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // then
            var certs = ksef.certificates();
            var builder = CertificateEnrollBuilder.create(
                    TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR);
            assertThrows(KsefServerException.class, () -> certs.enroll(builder));
        }
    }

    @Test
    void getEnrollmentStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            var certs = ksef.certificates();

            assertThrows(IllegalArgumentException.class,
                    () -> certs.getEnrollmentStatus("../../../etc/passwd"));
        }
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1234567890");
    }
}
