/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates;
import io.github.mgrtomaszzurawski.ksef.sdk.certificates.CertificateLimit;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;

/**
 * Certificate enrollment and certificate limits for the current subject.
 *
 * @param canRequest whether the subject can request new certificates
 * @param enrollment enrollment limits
 * @param certificate certificate limits
 */
public record CertificateLimits(boolean canRequest, CertificateLimit enrollment, CertificateLimit certificate) {

    public static CertificateLimits from(CertificateLimitsResponseRaw raw) {
        return new CertificateLimits(
                Boolean.TRUE.equals(raw.getCanRequest()),
                CertificateLimit.from(raw.getEnrollment()),
                CertificateLimit.from(raw.getCertificate()));
    }
}
