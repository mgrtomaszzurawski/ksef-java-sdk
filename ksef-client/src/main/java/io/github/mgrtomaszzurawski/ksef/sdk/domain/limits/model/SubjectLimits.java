/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

/**
 * Effective subject limits for enrollment and certificates.
 *
 * @param maxEnrollments maximum number of enrollments allowed (null if unlimited)
 * @param maxCertificates maximum number of certificates allowed (null if unlimited)
 *
 * @since 1.0.0
 */
public record SubjectLimits(@Nullable Integer maxEnrollments, @Nullable Integer maxCertificates) {

}
