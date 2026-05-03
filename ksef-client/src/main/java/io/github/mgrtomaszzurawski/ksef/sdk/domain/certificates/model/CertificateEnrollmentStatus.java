/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;

/**
 * Status of a certificate enrollment operation.
 *
 * @param requestDate when the enrollment was requested
 * @param status current enrollment status
 * @param certificateSerialNumber serial number of the enrolled certificate (null if pending)
 */
public record CertificateEnrollmentStatus(
        OffsetDateTime requestDate,
        StatusInfo status,
        String certificateSerialNumber) {

}
