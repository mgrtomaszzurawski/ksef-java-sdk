/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Certificate limit counters.
 *
 * @param remaining remaining certificates available
 * @param limit maximum certificates allowed
 */
public record CertificateLimit(Integer remaining, Integer limit) {

}
