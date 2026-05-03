/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Certificate enrollment and certificate limits for the current subject.
 *
 * @param canRequest whether the subject can request new certificates
 * @param enrollment enrollment limits
 * @param certificate certificate limits
 */
public record CertificateLimits(boolean canRequest, CertificateLimit enrollment, CertificateLimit certificate) {

}
