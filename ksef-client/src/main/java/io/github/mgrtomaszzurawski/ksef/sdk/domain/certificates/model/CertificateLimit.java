/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Certificate limit counters.
 *
 * @param remaining remaining certificates available
 * @param limit maximum certificates allowed
 *
 * @since 1.0.0
 */
public record CertificateLimit(Integer remaining, Integer limit) {

}
