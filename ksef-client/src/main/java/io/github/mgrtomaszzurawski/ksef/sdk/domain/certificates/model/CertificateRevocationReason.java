/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Reason for revoking a KSeF certificate.
 */
public enum CertificateRevocationReason {

    UNSPECIFIED,
    SUPERSEDED,
    KEY_COMPROMISE;

}
