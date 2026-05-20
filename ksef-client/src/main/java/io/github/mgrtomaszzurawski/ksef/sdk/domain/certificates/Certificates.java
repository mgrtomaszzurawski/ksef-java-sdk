/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate;
import java.security.KeyPair;
import java.time.Duration;
import java.util.List;

/**
 * KSeF certificate management — enrollment via a single workflow call,
 * by-serial retrieval, revocation, and querying.
 *
 * <p>Reached via {@code KsefClient.certificates()}.
 *
 * <p><strong>Authentication requirement</strong>: all endpoints in this
 * client require certificate-based authentication (XAdES, via
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefCertificateCredentials}
 * or
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefPkcs12Credentials}).
 * Token-authenticated callers will receive HTTP 403 from the server.
 *
 * <p><strong>Typical flow</strong>:
 *
 * <ol>
 *   <li>Optional pre-flight: {@link #getLimits()} to check the monthly
 *       enrollment quota (spec cap 12/month per subject; max 6 concurrent
 *       active). The SDK does not auto-block enrollment when the quota
 *       is exhausted — the server returns a typed error and the
 *       consumer decides whether to surface it.</li>
 *   <li>{@link #requestNewCertificate(KeyPair)} — single workflow call.
 *       Internally the SDK pulls the required CSR subject from the
 *       current auth session, builds the PKCS#10 CSR locally from the
 *       supplied {@link KeyPair}, submits enroll, polls until terminal,
 *       and retrieves the DER bytes — returning a fully-typed
 *       {@link RetrievedCertificate}. Use the {@code (KeyPair, Duration)}
 *       overload to override the default 5-minute polling timeout
 *       (ADR-032).</li>
 *   <li>{@link #retrieve(java.util.List)} — fetch DER bytes for
 *       previously enrolled certificates by serial number (e.g. when
 *       you persisted the serial across a process restart).</li>
 *   <li>{@link #revoke(CertificateSerialNumber, CertificateRevocationReason)}
 *       — revoke a certificate with a documented reason
 *       (audit / compliance requirement).</li>
 *   <li>{@link #queryCertificates(CertificateQueryRequest)} /
 *       {@link #streamCertificates(CertificateQueryRequest)} — list
 *       certificates with filters; query returns a single page with
 *       paging metadata, stream walks pages lazily.</li>
 * </ol>
 *
 * <p>Per ADR-032 the raw async endpoints (submit-CSR-and-poll
 * separately, fetch enrollment status, fetch CSR subject) are not on
 * the public surface — they were dead surface pre-1.0 and consumers
 * who wrote the poll loop themselves only duplicated SDK logic.
 *
 * @since 0.1.0
 */
public interface Certificates {

    /**
     * Pre-flight quota check for new-certificate requests on the current
     * subject (monthly cap of 12 per spec; max 6 concurrent active).
     *
     * @return current quota information
     */
    CertificateLimits getLimits();

    /**
     * Sync workflow: enroll a new KSeF certificate and return the typed
     * result. Uses a sensible default polling timeout (5 minutes).
     *
     * <p>Composition under the hood:
     * <ol>
     *   <li>Pull required CSR subject fields from the current auth
     *       session (server-derived from bearer token).</li>
     *   <li>Build a PKCS#10 CSR (SHA-256/RSA) from the supplied
     *       {@link KeyPair} and the subject from step 1.</li>
     *   <li>Submit the CSR via {@code POST /certificates/enrollments}.</li>
     *   <li>Poll the enrollment status endpoint until a terminal status
     *       is reached or the timeout elapses.</li>
     *   <li>Fetch the DER bytes via {@code POST /certificates/retrieve}
     *       and assemble a {@link RetrievedCertificate}.</li>
     * </ol>
     *
     * @param keyPair RSA key pair backing the new certificate; private
     *     key never leaves the consumer's JVM
     * @return a fully-typed {@link RetrievedCertificate}
     */
    RetrievedCertificate requestNewCertificate(KeyPair keyPair);

    /**
     * As {@link #requestNewCertificate(KeyPair)} but with an explicit
     * polling timeout. Useful for batch workflows that want bounded
     * latency per certificate request.
     *
     * @param keyPair RSA key pair backing the new certificate
     * @param timeout overall wall-clock budget for the poll loop
     * @return a fully-typed {@link RetrievedCertificate}
     */
    RetrievedCertificate requestNewCertificate(KeyPair keyPair, Duration timeout);

    /**
     * Fetch DER bytes for previously enrolled certificates by their
     * serial numbers.
     *
     * @param certificateSerialNumbers serial numbers to retrieve
     * @return raw certificate payloads + minimal metadata
     */
    RetrieveCertificatesResult retrieve(List<CertificateSerialNumber> certificateSerialNumbers);

    /**
     * Revoke a certificate. The reason is required — every revocation
     * must carry a documented cause for audit / compliance. Use
     * {@link CertificateRevocationReason#UNSPECIFIED} only when no
     * specific reason applies and the audit log entry "unspecified" is
     * acceptable.
     */
    void revoke(CertificateSerialNumber certificateSerialNumber, CertificateRevocationReason revocationReason);

    /**
     * Single-page certificate query with explicit paging controls.
     *
     * @param request filter + paging (use {@code pageOffset} +
     *     {@code pageSize} for snapshots at a specific position;
     *     {@code pageOffset} is honoured by this method but ignored by
     *     {@link #streamCertificates})
     */
    CertificateQueryResult queryCertificates(CertificateQueryRequest request);

    /**
     * Stream all certificates matching the filter. Pages are fetched
     * lazily via {@code pageOffset = 0, 1, 2, ...} until the server
     * reports {@code hasMore == false}. Caller controls memory by
     * limiting / collecting downstream.
     *
     * <p>{@code request.pageOffset()} is <em>ignored</em> — the
     * paginator always starts from page 0. Use
     * {@link #queryCertificates(CertificateQueryRequest)} for a snapshot
     * at a specific offset.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem>
            streamCertificates(CertificateQueryRequest request);
}
