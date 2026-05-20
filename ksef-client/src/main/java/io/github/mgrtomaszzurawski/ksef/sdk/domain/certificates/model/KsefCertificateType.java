/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

/**
 * Type of KSeF certificate.
 *
 * @since 0.1.0
 */
public enum KsefCertificateType {

    /**
     * Certificate used for KSeF online-session authentication (XAdES
     * signature on the auth challenge). Issued via
     * {@code certificates/enrollments}; valid for the auth flow only.
     */
    AUTHENTICATION,

    /**
     * Certificate used for the KOD II offline-invoice signing scheme
     * (offline invoice mode + verification QR code). Issued via the
     * same enrollment endpoint with this type code.
     */
    OFFLINE;

}
