/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.time.OffsetDateTime;

/**
 * Result of enrolling a new certificate.
 *
 * @param referenceNumber operation reference number
 * @param timestamp server timestamp of the enrollment
 */
public record EnrollCertificateResult(String referenceNumber, OffsetDateTime timestamp) {

}
