/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Status of a certificate enrollment operation.
 *
 * @param requestDate when the enrollment was requested
 * @param status current enrollment status
 * @param certificateSerialNumber serial number of the enrolled certificate (null if pending)
 *
 * @since 1.0.0
 */
public record CertificateEnrollmentStatus(
        OffsetDateTime requestDate,
        @Nullable StatusInfo status,
        @Nullable CertificateSerialNumber certificateSerialNumber) {

}
