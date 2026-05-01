/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;

/**
 * Type of KSeF certificate.
 */
public enum KsefCertificateType {

    AUTHENTICATION,
    OFFLINE;

    public KsefCertificateTypeRaw toRaw() {
        return switch (this) {
            case AUTHENTICATION -> KsefCertificateTypeRaw.AUTHENTICATION;
            case OFFLINE -> KsefCertificateTypeRaw.OFFLINE;
        };
    }
}
