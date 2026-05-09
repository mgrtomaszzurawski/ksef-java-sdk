/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code CertificateClient.query(...)}. All fields
 * are optional filters; an empty record queries all certificates.
 *
 * @since 1.0.0
 */
public record CertificateQueryRequest(
        @Nullable String serialNumber,
        @Nullable String name,
        @Nullable KsefCertificateType type,
        @Nullable CertificateStatus status,
        @Nullable OffsetDateTime expiresAfter,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {
}
