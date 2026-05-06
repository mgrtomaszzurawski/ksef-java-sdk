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
 *
 * @since 1.0.0
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
     * Stream all certificates matching the filter. Pages are fetched
     * lazily via {@code pageOffset = 0, 1, 2, ...} until the server
     * reports {@code hasMore == false}. Caller controls memory by
     * limiting / collecting downstream.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            streamCertificates(CertificateQueryBuilder builder);

    /**
     * Codex 2026-05-05 #10 / F7 — enroll a certificate and poll
     * {@link #getEnrollmentStatus(String)} until terminal
     * (status.code() &gt;= 200). Returns the terminal status which
     * carries the assigned {@code certificateSerialNumber} on success.
     * Throws {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException}
     * on timeout.
     *
     * @since 1.0.0
     */
    default CertificateEnrollmentStatus enrollAndAwait(CertificateEnrollBuilder builder,
                                                        java.time.Duration timeout) {
        EnrollCertificateResult result = enroll(builder);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.AsyncOperationAwaiter.awaitTerminal(
                "enrollCertificate",
                () -> getEnrollmentStatus(result.referenceNumber()),
                status -> status.status() != null && status.status().code() >= 200,
                status -> status.status() == null ? null : status.status().code(),
                timeout,
                null);
    }
}
