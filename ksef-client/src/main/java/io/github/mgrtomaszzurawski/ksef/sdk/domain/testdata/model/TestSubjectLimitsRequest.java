/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code TestDataClient.setSubjectLimits(...)}.
 *
 * @param subjectIdentifierType type of subject identifier
 * @param maxEnrollments override for enrollment count limit (null = leave default)
 * @param maxCertificates override for active certificate limit (null = leave default)
 */
public record TestSubjectLimitsRequest(
        TestSubjectIdentifierType subjectIdentifierType,
        @Nullable Integer maxEnrollments,
        @Nullable Integer maxCertificates) {
}
