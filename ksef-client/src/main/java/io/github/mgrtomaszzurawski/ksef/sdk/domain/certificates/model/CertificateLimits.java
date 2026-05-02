/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;

/**
 * Certificate enrollment and certificate limits for the current subject.
 *
 * @param canRequest whether the subject can request new certificates
 * @param enrollment enrollment limits
 * @param certificate certificate limits
 */
public record CertificateLimits(boolean canRequest, CertificateLimit enrollment, CertificateLimit certificate) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static CertificateLimits from(CertificateLimitsResponseRaw raw) {
        return new CertificateLimits(
                raw.getCanRequest(),
                CertificateLimit.from(raw.getEnrollment()),
                CertificateLimit.from(raw.getCertificate()));
    }
}
