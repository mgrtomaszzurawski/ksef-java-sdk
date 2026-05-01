/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitRaw;

/**
 * Certificate limit counters.
 *
 * @param remaining remaining certificates available
 * @param limit maximum certificates allowed
 */
public record CertificateLimit(Integer remaining, Integer limit) {

    public static CertificateLimit from(CertificateLimitRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CertificateLimit(raw.getRemaining(), raw.getLimit());
    }
}
