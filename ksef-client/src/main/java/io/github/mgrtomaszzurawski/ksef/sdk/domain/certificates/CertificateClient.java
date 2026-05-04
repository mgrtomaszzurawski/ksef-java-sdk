/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import java.util.List;

/**
 * Client for KSeF certificate management — enrollment, retrieval, querying,
 * revocation, and limits.
 */
public interface CertificateClient {

    CertificateLimits getLimits();
    CertificateEnrollmentData getEnrollmentData();
    EnrollCertificateResult enroll(CertificateEnrollBuilder builder);
    CertificateEnrollmentStatus getEnrollmentStatus(String referenceNumber);
    RetrieveCertificatesResult retrieve(List<String> certificateSerialNumbers);
    void revoke(String certificateSerialNumber);

    void revoke(String certificateSerialNumber, CertificateRevocationReason revocationReason);

    CertificateQueryResult query(CertificateQueryBuilder builder);

    /**
     * Query all certificates matching the filter, walking pageOffset
     * internally with spec-max page size. Codex round-9 manual-validation
     * A.4.1 — saves consumers from composing pagination loops.
     */
    List<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            queryAll(CertificateQueryBuilder builder);
}
