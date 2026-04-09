/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RevokeCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.RetrieveCertificatesResult;

import static io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF certificate management — enrollment, retrieval, querying,
 * revocation, and limits.
 */
public final class CertificateClient {

    private static final String PATH_CERTIFICATES = "/api/v2/certificates";
    private static final String PATH_LIMITS = "/api/v2/certificates/limits";
    private static final String PATH_ENROLLMENT_DATA = "/api/v2/certificates/enrollments/data";
    private static final String PATH_ENROLLMENTS = "/api/v2/certificates/enrollments";
    private static final String PATH_RETRIEVE = "/api/v2/certificates/retrieve";
    private static final String PATH_QUERY = "/api/v2/certificates/query";

    private static final String SEGMENT_REVOKE = "/revoke";

    private static final String OP_GET_LIMITS = "getCertificateLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_ENROLL = "enrollCertificate";
    private static final String OP_GET_ENROLLMENT_STATUS = "getEnrollmentStatus";
    private static final String OP_RETRIEVE = "retrieveCertificates";
    private static final String OP_REVOKE = "revokeCertificate";
    private static final String OP_QUERY = "queryCertificates";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public CertificateClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Get certificate enrollment and revocation limits for the current subject.
     *
     * @return certificate limits
     */
    public CertificateLimits getLimits() {
        String token = sessionContext.token();
        CertificateLimitsResponseRaw raw = http.getAuthenticated(PATH_LIMITS, token,
                CertificateLimitsResponseRaw.class, OP_GET_LIMITS);
        return CertificateLimits.from(raw);
    }

    /**
     * Get data required for certificate enrollment (CSR template, allowed types).
     *
     * @return enrollment data with CSR requirements
     */
    public CertificateEnrollmentData getEnrollmentData() {
        String token = sessionContext.token();
        CertificateEnrollmentDataResponseRaw raw = http.getAuthenticated(PATH_ENROLLMENT_DATA, token,
                CertificateEnrollmentDataResponseRaw.class, OP_GET_ENROLLMENT_DATA);
        return CertificateEnrollmentData.from(raw);
    }

    /**
     * Enroll (register) a new certificate.
     *
     * @param request enrollment request with certificate name, type, and CSR
     * @return response with enrollment reference number
     */
    public EnrollCertificateResult enroll(EnrollCertificateRequestRaw request) {
        String token = sessionContext.token();
        EnrollCertificateResponseRaw raw = http.postJsonAuthenticated(PATH_ENROLLMENTS, request, token,
                EnrollCertificateResponseRaw.class, OP_ENROLL);
        return EnrollCertificateResult.from(raw);
    }

    /**
     * Get the status of a certificate enrollment.
     *
     * @param referenceNumber the enrollment reference number
     * @return enrollment status
     */
    public CertificateEnrollmentStatus getEnrollmentStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        CertificateEnrollmentStatusResponseRaw raw = http.getAuthenticated(PATH_ENROLLMENTS + "/" + referenceNumber, token,
                CertificateEnrollmentStatusResponseRaw.class, OP_GET_ENROLLMENT_STATUS);
        return CertificateEnrollmentStatus.from(raw);
    }

    /**
     * Retrieve certificates by their serial numbers.
     *
     * @param request request containing certificate serial numbers
     * @return response with certificate details
     */
    public RetrieveCertificatesResult retrieve(RetrieveCertificatesRequestRaw request) {
        String token = sessionContext.token();
        RetrieveCertificatesResponseRaw raw = http.postJsonAuthenticated(PATH_RETRIEVE, request, token,
                RetrieveCertificatesResponseRaw.class, OP_RETRIEVE);
        return RetrieveCertificatesResult.from(raw);
    }

    /**
     * Revoke a certificate by its serial number.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     * @param request revocation request with reason
     */
    public void revoke(String certificateSerialNumber, RevokeCertificateRequestRaw request) {
        requireSafePathSegment(certificateSerialNumber);
        String token = sessionContext.token();
        String path = PATH_CERTIFICATES + "/" + certificateSerialNumber + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, request, token, OP_REVOKE);
    }

    /**
     * Query certificates with optional filters.
     *
     * @param request query filters
     * @return matching certificates
     */
    public CertificateQueryResult query(QueryCertificatesRequestRaw request) {
        String token = sessionContext.token();
        QueryCertificatesResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY, request, token,
                QueryCertificatesResponseRaw.class, OP_QUERY);
        return CertificateQueryResult.from(raw);
    }
}
