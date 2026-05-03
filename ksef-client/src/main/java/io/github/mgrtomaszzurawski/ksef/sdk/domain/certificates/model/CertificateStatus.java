/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Status of a KSeF certificate.
 */
public enum CertificateStatus {

    ACTIVE,
    BLOCKED,
    REVOKED,
    EXPIRED;

}
