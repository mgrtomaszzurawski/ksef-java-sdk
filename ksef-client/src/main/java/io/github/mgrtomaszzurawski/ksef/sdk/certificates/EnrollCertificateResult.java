/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates;

import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;

import java.time.OffsetDateTime;

/**
 * Result of enrolling a new certificate.
 *
 * @param referenceNumber operation reference number
 * @param timestamp server timestamp of the enrollment
 */
public record EnrollCertificateResult(String referenceNumber, OffsetDateTime timestamp) {

    public static EnrollCertificateResult from(EnrollCertificateResponseRaw raw) {
        return new EnrollCertificateResult(raw.getReferenceNumber(), raw.getTimestamp());
    }
}
