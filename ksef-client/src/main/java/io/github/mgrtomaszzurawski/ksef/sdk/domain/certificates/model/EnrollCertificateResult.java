/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.time.OffsetDateTime;

/**
 * Result of enrolling a new certificate.
 *
 * @param referenceNumber operation reference number
 * @param timestamp server timestamp of the enrollment
 *
 * @since 1.0.0
 */
public record EnrollCertificateResult(String referenceNumber, OffsetDateTime timestamp) {

}
