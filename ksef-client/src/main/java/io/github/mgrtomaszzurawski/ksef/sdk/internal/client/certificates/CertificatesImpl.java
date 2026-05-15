/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.Certificates;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RevokeCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber;
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
 *
 * @since 1.0.0
 */
public final class CertificatesImpl implements Certificates {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificatesImpl.class);
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

    private static final String ERR_NULL_REQUEST = "request is required";
    private static final String ERR_NULL_CERTIFICATE_SERIAL_NUMBERS = "certificateSerialNumbers is required";
    private static final String ERR_NULL_CERTIFICATE_SERIAL_NUMBER = "certificateSerialNumber must not be null";
    private static final String ERR_NULL_REVOCATION_REASON = "revocationReason is required";

    private static final String PARAM_PAGE_OFFSET_PREFIX = "?pageOffset=";
    private static final String PARAM_PAGE_SIZE_PREFIX = "?pageSize=";
    private static final String PARAM_PAGE_SIZE_AFTER_FIRST = "&pageSize=";
    private static final String WARN_TOKEN_AUTH_FOR_CERT_OP =
            "Calling certificate operation {} on a token-authenticated session — KSeF restricts "
                    + "/certificates/enrollments and /certificates/enrollments/data to certificate-based auth "
                    + "(KsefCertificateCredentials / KsefPkcs12Credentials). The server will return a typed error.";

    private final HttpSupport http;
    private final HttpRuntime runtime;

    public CertificatesImpl(HttpRuntime runtime) {
        this.runtime = runtime;
        this.http = new HttpSupport(runtime);
    }

    private void warnIfNotCertificateAuth(String operation) {
        if (!runtime.isAuthenticatedViaCertificate()) {
            LOGGER.warn(WARN_TOKEN_AUTH_FOR_CERT_OP, operation);
        }
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
        warnIfNotCertificateAuth(OP_GET_ENROLLMENT_DATA);
        String token = http.requireToken();
        CertificateEnrollmentDataResponseRaw rawValue = http.getAuthenticated(PATH_ENROLLMENT_DATA, token,
                CertificateEnrollmentDataResponseRaw.class, OP_GET_ENROLLMENT_DATA);
        return CertificatesMappers.toCertificateEnrollmentData(rawValue);
    }

    /**
     * Enroll (register) a new certificate.
     *
     * @param request enrollment request with certificate name, type, and CSR
     * @return response with enrollment reference number
     */
    @Override
    public EnrollCertificateResult enroll(CertificateEnrollRequest request) {
        LOGGER.debug(LOG_CALL, OP_ENROLL);
        warnIfNotCertificateAuth(OP_ENROLL);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        EnrollCertificateRequestRaw rawRequest = CertificatesMappers.toEnrollCertificateRequestRaw(request);
        String token = http.requireToken();
        EnrollCertificateResponseRaw rawValue = http.postJsonAuthenticated(PATH_ENROLLMENTS, rawRequest, token,
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
    public RetrieveCertificatesResult retrieve(List<CertificateSerialNumber> certificateSerialNumbers) {
        LOGGER.debug(LOG_CALL, OP_RETRIEVE);
        Objects.requireNonNull(certificateSerialNumbers, ERR_NULL_CERTIFICATE_SERIAL_NUMBERS);
        RetrieveCertificatesRequestRaw rawRequest = new RetrieveCertificatesRequestRaw();
        rawRequest.setCertificateSerialNumbers(certificateSerialNumbers.stream()
                .map(CertificateSerialNumber::value).toList());
        String token = http.requireToken();
        RetrieveCertificatesResponseRaw rawValue = http.postJsonAuthenticated(PATH_RETRIEVE, rawRequest, token,
                RetrieveCertificatesResponseRaw.class, OP_RETRIEVE);
        return CertificatesMappers.toRetrieveCertificatesResult(rawValue);
    }

    /**
     * Revoke a certificate by its serial number with a specific reason.
     *
     * @param certificateSerialNumber the serial number of the certificate to revoke
     * @param revocationReason reason for revocation (Unspecified, Superseded, KeyCompromise)
     */
    @Override
    public void revoke(CertificateSerialNumber certificateSerialNumber, CertificateRevocationReason revocationReason) {
        Objects.requireNonNull(certificateSerialNumber, ERR_NULL_CERTIFICATE_SERIAL_NUMBER);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(LOG_CALL_REF, OP_REVOKE, certificateSerialNumber.value());
        }
        Objects.requireNonNull(revocationReason, ERR_NULL_REVOCATION_REASON);
        RevokeCertificateRequestRaw rawRequest = new RevokeCertificateRequestRaw();
        rawRequest.setRevocationReason(CertificatesMappers.toCertificateRevocationReasonRaw(revocationReason));
        String token = http.requireToken();
        String path = ApiPaths.subPath(PATH_CERTIFICATES, certificateSerialNumber.value()) + SEGMENT_REVOKE;
        http.postJsonAuthenticatedNoContent(path, rawRequest, token, OP_REVOKE);
    }

    /**
     * Query certificates with optional filters.
     *
     * @param request query request with optional filters and paging
     * @return matching certificates
     */
    @Override
    public CertificateQueryResult queryCertificates(CertificateQueryRequest request) {
        LOGGER.debug(LOG_CALL, OP_QUERY);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY, request.pageOffset(), request.pageSize());
        QueryCertificatesResponseRaw rawValue = http.postJsonAuthenticated(path,
                CertificatesMappers.toQueryCertificatesRequestRaw(request), token,
                QueryCertificatesResponseRaw.class, OP_QUERY);
        return CertificatesMappers.toCertificateQueryResult(rawValue);
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            streamCertificates(CertificateQueryRequest request) {
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        int effectivePageSize = request.pageSize() == null
                ? CERTIFICATE_QUERY_MAX_PAGE_SIZE : request.pageSize();
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            String pagedPath = PATH_QUERY + PARAM_PAGE_OFFSET_PREFIX + pageOffset
                    + PARAM_PAGE_SIZE_AFTER_FIRST + effectivePageSize;
            QueryCertificatesResponseRaw raw = http.postJsonAuthenticated(pagedPath,
                    CertificatesMappers.toQueryCertificatesRequestRaw(request),
                    token, QueryCertificatesResponseRaw.class, OP_QUERY);
            CertificateQueryResult page = CertificatesMappers.toCertificateQueryResult(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.certificates(), page.hasMore());
        });
    }

    /** Build {@code ?pageOffset=...&pageSize=...} query string fragments only when present. */
    private static String appendPaging(String basePath,
                                       @org.jspecify.annotations.Nullable Integer pageOffset,
                                       @org.jspecify.annotations.Nullable Integer pageSize) {
        if (pageOffset == null && pageSize == null) {
            return basePath;
        }
        StringBuilder query = new StringBuilder(basePath);
        boolean first = true;
        if (pageOffset != null) {
            query.append(PARAM_PAGE_OFFSET_PREFIX).append(pageOffset);
            first = false;
        }
        if (pageSize != null) {
            query.append(first ? PARAM_PAGE_SIZE_PREFIX : PARAM_PAGE_SIZE_AFTER_FIRST).append(pageSize);
        }
        return query.toString();
    }
}
