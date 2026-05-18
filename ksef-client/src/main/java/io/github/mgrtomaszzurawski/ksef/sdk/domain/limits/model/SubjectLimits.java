/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

/**
 * Certificate and enrollment quota for the authenticated taxpayer
 * (subject identified by NIP). Both fields are nullable — server omits
 * them when the limit is unenforced for the subject. Non-null values
 * are hard caps validated on the certificate-management flow.
 *
 * <p>Typical values on KSeF demo and production (May 2026):
 * {@code maxEnrollments = 12}, {@code maxCertificates = 6}. Crossing
 * either cap surfaces as a typed validation error on
 * {@code POST /certificates/enrollments} or activation.
 *
 * @param maxEnrollments rolling monthly quota of certificate-enrollment
 *     requests permitted for the subject NIP. Each call to
 *     {@code client.certificates().enroll(...)} consumes one slot
 *     regardless of whether the enrollment completes successfully.
 *     {@code null} when the server does not advertise a cap.
 * @param maxCertificates maximum number of simultaneously active
 *     (non-revoked, non-expired) certificates the subject NIP may hold.
 *     Issuing a new certificate beyond this cap requires revoking an
 *     existing one first. {@code null} when the server does not
 *     advertise a cap.
 *
 * @since 1.0.0
 */
public record SubjectLimits(@Nullable Integer maxEnrollments, @Nullable Integer maxCertificates) {

}
