/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollmentEffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEffectiveSubjectLimitsRaw;

/**
 * Effective subject limits for enrollment and certificates.
 *
 * @param maxEnrollments maximum number of enrollments allowed (null if unlimited)
 * @param maxCertificates maximum number of certificates allowed (null if unlimited)
 */
public record SubjectLimits(Integer maxEnrollments, Integer maxCertificates) {

    public static SubjectLimits from(EffectiveSubjectLimitsRaw raw) {
        EnrollmentEffectiveSubjectLimitsRaw enrollment = raw.getEnrollment();
        CertificateEffectiveSubjectLimitsRaw certificate = raw.getCertificate();
        return new SubjectLimits(
                enrollment != null ? enrollment.getMaxEnrollments() : null,
                certificate != null ? certificate.getMaxCertificates() : null);
    }
}
