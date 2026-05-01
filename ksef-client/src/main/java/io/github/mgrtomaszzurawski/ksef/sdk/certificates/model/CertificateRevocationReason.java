/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateRevocationReasonRaw;

/**
 * Reason for revoking a KSeF certificate.
 */
public enum CertificateRevocationReason {

    UNSPECIFIED,
    SUPERSEDED,
    KEY_COMPROMISE;

    public CertificateRevocationReasonRaw toRaw() {
        return switch (this) {
            case UNSPECIFIED -> CertificateRevocationReasonRaw.UNSPECIFIED;
            case SUPERSEDED -> CertificateRevocationReasonRaw.SUPERSEDED;
            case KEY_COMPROMISE -> CertificateRevocationReasonRaw.KEY_COMPROMISE;
        };
    }
}
