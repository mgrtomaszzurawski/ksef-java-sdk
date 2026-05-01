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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.KsefTokenCredentials;
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

@WireMockTest
class CertificateClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_ENROLLMENT_REF = "20260404-CE-1234567890-ABCDEF1234-08";
    private static final String TEST_CERT_SERIAL = "ABC123DEF456";
    private static final String TEST_CERT_NAME = "Test Auth Certificate";
    private static final byte[] TEST_CSR = new byte[]{0x30, 0x42};

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_SERVER_ERROR = 500;
    private static final int KSEF_STATUS_OK = 200;

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

    @Test
    void getLimits_whenAuthenticated_returnsLimits(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/certificates/limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(LIMITS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        CertificateLimits response = ksef.certificates().getLimits();

        // then
        assertTrue(response.canRequest());
        assertNotNull(response.enrollment());
        assertNotNull(response.certificate());
    }

    @Test
    void getEnrollmentData_whenAuthenticated_returnsData(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/certificates/enrollments/data"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ENROLLMENT_DATA_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        CertificateEnrollmentData response = ksef.certificates().getEnrollmentData();

        // then
        assertEquals("KSeF Certificate", response.commonName());
        assertEquals("PL", response.countryName());
    }

    @Test
    void enroll_whenAuthenticated_returnsReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/certificates/enrollments"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ENROLL_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        EnrollCertificateResult response = ksef.certificates().enroll(
                CertificateEnrollBuilder.create(TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR));

        // then
        assertEquals(TEST_ENROLLMENT_REF, response.referenceNumber());
        assertNotNull(response.timestamp());
    }

    @Test
    void getEnrollmentStatus_whenExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo("/api/v2/certificates/enrollments/" + TEST_ENROLLMENT_REF))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ENROLLMENT_STATUS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        CertificateEnrollmentStatus response =
                ksef.certificates().getEnrollmentStatus(TEST_ENROLLMENT_REF);

        // then
        assertEquals(KSEF_STATUS_OK, response.status().code());
        assertNotNull(response.requestDate());
    }

    @Test
    void retrieve_whenAuthenticated_returnsCertificates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/certificates/retrieve"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RETRIEVE_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        RetrieveCertificatesResult response =
                ksef.certificates().retrieve(List.of(TEST_CERT_SERIAL));

        // then
        assertNotNull(response.certificates());
    }

    @Test
    void revoke_whenAuthenticated_sendsRevokeRequest(WireMockRuntimeInfo wmInfo) {
        // given
        String revokePath = "/api/v2/certificates/" + TEST_CERT_SERIAL + "/revoke";
        stubFor(post(urlEqualTo(revokePath))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.certificates().revoke(TEST_CERT_SERIAL, CertificateRevocationReason.UNSPECIFIED);

        // then
        verify(postRequestedFor(urlEqualTo(revokePath))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void query_whenAuthenticated_returnsCertificates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/certificates/query"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(QUERY_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        CertificateQueryResult response =
                ksef.certificates().query(CertificateQueryBuilder.create());

        // then
        assertNotNull(response.certificates());
        assertFalse(response.hasMore());
    }

    @Test
    void enroll_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/certificates/enrollments"))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // then
        var certs = ksef.certificates();
        var builder = CertificateEnrollBuilder.create(
                TEST_CERT_NAME, KsefCertificateType.AUTHENTICATION, TEST_CSR);
        assertThrows(KsefServerException.class, () -> certs.enroll(builder));
    }

    @Test
    void getEnrollmentStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);
        var certs = ksef.certificates();

        assertThrows(IllegalArgumentException.class,
                () -> certs.getEnrollmentStatus("../../../etc/passwd"));
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
