/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.CertificateClient;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RevokeCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.mapping.CertificatesMappers;

/**
 * Client for KSeF certificate management — enrollment, retrieval, querying,
 * revocation, and limits.
 */
public final class CertificateClientImpl implements CertificateClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateClientImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_CERTIFICATES = ApiPaths.CERTIFICATES;
    private static final String PATH_LIMITS = ApiPaths.CERTIFICATES + "/limits";
    private static final String PATH_ENROLLMENT_DATA = ApiPaths.CERTIFICATES + "/enrollments/data";
    private static final String PATH_ENROLLMENTS = ApiPaths.CERTIFICATES + "/enrollments";
    private static final String PATH_RETRIEVE = ApiPaths.CERTIFICATES + "/retrieve";
    private static final String PATH_QUERY = ApiPaths.CERTIFICATES + "/query";
    /** Spec-defined max page size for certificate query endpoint. */
    private static final int CERTIFICATE_QUERY_MAX_PAGE_SIZE = 250;

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

    public CertificateClientImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Get certificate enrollment and revocation limits for the current subject.
     *
     * @return certificate limits
     */
    @Override
    public CertificateLimits getLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_LIMITS);
        String token = http.requireToken();
        CertificateLimitsResponseRaw rawValue = http.getAuthenticated(PATH_LIMITS, token,
                CertificateLimitsResponseRaw.class, OP_GET_LIMITS);
        return CertificatesMappers.toCertificateLimits(rawValue);
    }

    /**
     * Get data required for certificate enrollment (CSR template, allowed types).
     *
     * @return enrollment data with CSR requirements
     */
    @Override
    public CertificateEnrollmentData getEnrollmentData() {
        LOGGER.debug(LOG_CALL, OP_GET_ENROLLMENT_DATA);
        String token = http.requireToken();
        CertificateEnrollmentDataResponseRaw rawValue = http.getAuthenticated(PATH_ENROLLMENT_DATA, token,
                CertificateEnrollmentDataResponseRaw.class, OP_GET_ENROLLMENT_DATA);
        return CertificatesMappers.toCertificateEnrollmentData(rawValue);
    }

    /**
     * Enroll (register) a new certificate.
     *
     * @param builder enrollment builder with certificate name, type, and CSR
     * @return response with enrollment reference number
     */
    @Override
    public EnrollCertificateResult enroll(CertificateEnrollBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_ENROLL);
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        EnrollCertificateRequestRaw request = CertificatesMappers.toEnrollCertificateRequestRaw(builder.build());
        String token = http.requireToken();
        EnrollCertificateResponseRaw rawValue = http.postJsonAuthenticated(PATH_ENROLLMENTS, request, token,
                EnrollCertificateResponseRaw.class, OP_ENROLL);
        return CertificatesMappers.toEnrollCertificateResult(rawValue);
    }

    /**
     * Get the status of a certificate enrollment.
     *
     * @param referenceNumber the enrollment reference number
     * @return enrollment status
     */
    @Override
    public CertificateEnrollmentStatus getEnrollmentStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_ENROLLMENT_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        CertificateEnrollmentStatusResponseRaw rawValue = http.getAuthenticated(ApiPaths.subPath(PATH_ENROLLMENTS, referenceNumber), token,
                CertificateEnrollmentStatusResponseRaw.class, OP_GET_ENROLLMENT_STATUS);
        return CertificatesMappers.toCertificateEnrollmentStatus(rawValue);
    }

    /**
     * Retrieve certificates by their serial numbers.
     *
     * @param certificateSerialNumbers serial numbers of certificates to retrieve
     * @return response with certificate details
     */
    @Override
    public RetrieveCertificatesResult retrieve(List<String> certificateSerialNumbers) {
        LOGGER.debug(LOG_CALL, OP_RETRIEVE);
        Objects.requireNonNull(certificateSerialNumbers, ERR_NULL_CERTIFICATE_SERIAL_NUMBERS);
        RetrieveCertificatesRequestRaw request = new RetrieveCertificatesRequestRaw();
        request.setCertificateSerialNumbers(certificateSerialNumbers);
        String token = http.requireToken();
        RetrieveCertificatesResponseRaw rawValue = http.postJsonAuthenticated(PATH_RETRIEVE, request, token,
                RetrieveCertificatesResponseRaw.class, OP_RETRIEVE);
        return CertificatesMappers.toRetrieveCertificatesResult(rawValue);
    }

    /**
     * Revoke a certificate by its serial number with no specific reason.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     */
    @Override
    public void revoke(String certificateSerialNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE, certificateSerialNumber);
        requireSafePathSegment(certificateSerialNumber);
        RevokeCertificateRequestRaw request = new RevokeCertificateRequestRaw();
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_CERTIFICATES, certificateSerialNumber) + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, request, token, OP_REVOKE);
    }

    /**
     * Revoke a certificate by its serial number with a specific reason.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     * @param revocationReason reason for revocation (Unspecified, Superseded, KeyCompromise)
     */
    @Override
    public void revoke(String certificateSerialNumber, CertificateRevocationReason revocationReason) {
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE, certificateSerialNumber);
        requireSafePathSegment(certificateSerialNumber);
        Objects.requireNonNull(revocationReason, ERR_NULL_REVOCATION_REASON);
        RevokeCertificateRequestRaw request = new RevokeCertificateRequestRaw();
        request.setRevocationReason(CertificatesMappers.toCertificateRevocationReasonRaw(revocationReason));
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_CERTIFICATES, certificateSerialNumber) + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, request, token, OP_REVOKE);
    }

    /**
     * Query certificates with optional filters.
     *
     * @param builder query builder with optional filters
     * @return matching certificates
     */
    @Override
    public CertificateQueryResult query(CertificateQueryBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_QUERY);
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        String token = http.requireToken();
        QueryCertificatesResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY, CertificatesMappers.toQueryCertificatesRequestRaw(builder.build()), token,
                QueryCertificatesResponseRaw.class, OP_QUERY);
        return CertificatesMappers.toCertificateQueryResult(rawValue);
    }

    @Override
    public java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            queryAll(CertificateQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem> all =
                new java.util.ArrayList<>();
        int pageOffset = 0;
        while (true) {
            String token = http.requireToken();
            String pagedPath = PATH_QUERY + "?pageOffset=" + pageOffset
                    + "&pageSize=" + CERTIFICATE_QUERY_MAX_PAGE_SIZE;
            QueryCertificatesResponseRaw raw = http.postJsonAuthenticated(pagedPath,
                    CertificatesMappers.toQueryCertificatesRequestRaw(builder.build()),
                    token, QueryCertificatesResponseRaw.class, OP_QUERY);
            CertificateQueryResult page = CertificatesMappers.toCertificateQueryResult(raw);
            all.addAll(page.certificates());
            if (!page.hasMore()) {
                return java.util.List.copyOf(all);
            }
            pageOffset++;
        }
    }
}
