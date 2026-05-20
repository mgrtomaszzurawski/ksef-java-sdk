/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Status of a KSeF certificate.
 *
 * @since 0.1.0
 */
public enum CertificateStatus {

    ACTIVE,
    BLOCKED,
    REVOKED,
    EXPIRED;

}
