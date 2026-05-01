/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemStatusRaw;

/**
 * Status of a KSeF certificate.
 */
public enum CertificateStatus {

    ACTIVE,
    BLOCKED,
    REVOKED,
    EXPIRED;

    public CertificateListItemStatusRaw toRaw() {
        return switch (this) {
            case ACTIVE -> CertificateListItemStatusRaw.ACTIVE;
            case BLOCKED -> CertificateListItemStatusRaw.BLOCKED;
            case REVOKED -> CertificateListItemStatusRaw.REVOKED;
            case EXPIRED -> CertificateListItemStatusRaw.EXPIRED;
        };
    }
}
