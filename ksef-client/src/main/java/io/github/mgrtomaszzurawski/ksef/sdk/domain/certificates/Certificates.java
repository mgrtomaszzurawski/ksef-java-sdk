/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import java.util.List;

/**
 * Client for KSeF certificate management — enrollment, retrieval, querying,
 * revocation, and limits.
 *
 * <p><strong>Authentication requirement:</strong> all endpoints in this
 * client require certificate-based authentication (XAdES, via
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials}
 * or
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials}).
 * Token-authenticated callers will receive HTTP 403 from the server.
 *
 * @since 1.0.0
 */
public interface Certificates {

    CertificateLimits getLimits();
    CertificateEnrollmentData getEnrollmentData();
    EnrollCertificateResult enroll(CertificateEnrollRequest request);
    CertificateEnrollmentStatus getEnrollmentStatus(String referenceNumber);
    RetrieveCertificatesResult retrieve(List<String> certificateSerialNumbers);

    /**
     * Revoke a certificate. The reason is required — every revocation must
     * carry a documented cause for audit / compliance. Use
     * {@link CertificateRevocationReason#UNSPECIFIED} only when no specific
     * reason applies and the audit log entry "unspecified" is acceptable.
     */
    void revoke(String certificateSerialNumber, CertificateRevocationReason revocationReason);

    CertificateQueryResult queryCertificates(CertificateQueryRequest request);

    /**
     * Stream all certificates matching the filter. Pages are fetched
     * lazily via {@code pageOffset = 0, 1, 2, ...} until the server
     * reports {@code hasMore == false}. Caller controls memory by
     * limiting / collecting downstream.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            streamCertificates(CertificateQueryRequest request);
}
