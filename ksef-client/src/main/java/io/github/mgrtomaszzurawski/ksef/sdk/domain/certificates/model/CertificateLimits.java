/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import org.jspecify.annotations.Nullable;

/**
 * Certificate enrollment and active-certificate limits for the current
 * subject. KSeF caps certificate management per natural person:
 * 12 enrollment requests per calendar month and 6 simultaneously
 * active certificates.
 *
 * @param canRequest whether the subject is currently allowed to request
 *     a new certificate (false when monthly enrollment quota or active
 *     cert cap is reached)
 * @param enrollment per-month enrollment-request quota (used vs limit)
 * @param certificate active-certificate cap (currently active vs limit)
 *
 * @since 0.1.0
 */
public record CertificateLimits(boolean canRequest, @Nullable CertificateLimit enrollment, @Nullable CertificateLimit certificate) {

}
