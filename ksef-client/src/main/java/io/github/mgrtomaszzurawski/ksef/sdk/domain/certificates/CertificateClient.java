/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RevokeCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import java.util.Objects;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;

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

    private static final String ERR_NULL_BUILDER = "builder is required";
    private static final String ERR_NULL_CERTIFICATE_SERIAL_NUMBERS = "certificateSerialNumbers is required";
    private static final String ERR_NULL_REVOCATION_REASON = "revocationReason is required";

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
     * @param builder enrollment builder with certificate name, type, and CSR
     * @return response with enrollment reference number
     */
    public EnrollCertificateResult enroll(CertificateEnrollBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        EnrollCertificateRequestRaw request = builder.build();
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
     * @param certificateSerialNumbers serial numbers of certificates to retrieve
     * @return response with certificate details
     */
    public RetrieveCertificatesResult retrieve(List<String> certificateSerialNumbers) {
        Objects.requireNonNull(certificateSerialNumbers, ERR_NULL_CERTIFICATE_SERIAL_NUMBERS);
        RetrieveCertificatesRequestRaw request = new RetrieveCertificatesRequestRaw();
        request.setCertificateSerialNumbers(certificateSerialNumbers);
        String token = sessionContext.token();
        RetrieveCertificatesResponseRaw raw = http.postJsonAuthenticated(PATH_RETRIEVE, request, token,
                RetrieveCertificatesResponseRaw.class, OP_RETRIEVE);
        return RetrieveCertificatesResult.from(raw);
    }

    /**
     * Revoke a certificate by its serial number with no specific reason.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     */
    public void revoke(String certificateSerialNumber) {
        requireSafePathSegment(certificateSerialNumber);
        RevokeCertificateRequestRaw request = new RevokeCertificateRequestRaw();
        String token = sessionContext.token();
        String path = PATH_CERTIFICATES + "/" + certificateSerialNumber + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, request, token, OP_REVOKE);
    }

    /**
     * Revoke a certificate by its serial number with a specific reason.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     * @param revocationReason reason for revocation (Unspecified, Superseded, KeyCompromise)
     */
    public void revoke(String certificateSerialNumber, CertificateRevocationReason revocationReason) {
        requireSafePathSegment(certificateSerialNumber);
        Objects.requireNonNull(revocationReason, ERR_NULL_REVOCATION_REASON);
        RevokeCertificateRequestRaw request = new RevokeCertificateRequestRaw();
        request.setRevocationReason(revocationReason.toRaw());
        String token = sessionContext.token();
        String path = PATH_CERTIFICATES + "/" + certificateSerialNumber + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, request, token, OP_REVOKE);
    }

    /**
     * Query certificates with optional filters.
     *
     * @param builder query builder with optional filters
     * @return matching certificates
     */
    public CertificateQueryResult query(CertificateQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        String token = sessionContext.token();
        QueryCertificatesResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY, builder.build(), token,
                QueryCertificatesResponseRaw.class, OP_QUERY);
        return CertificateQueryResult.from(raw);
    }
}
