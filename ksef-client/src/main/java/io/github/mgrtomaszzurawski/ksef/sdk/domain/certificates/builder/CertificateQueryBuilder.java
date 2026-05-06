/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.time.OffsetDateTime;

/**
 * Builder for KSeF certificate query requests. All fields are optional filters.
 *
 * @since 1.0.0
 */
public final class CertificateQueryBuilder {

    private String serialNumber;
    private String name;
    private KsefCertificateType type;
    private CertificateStatus status;
    private OffsetDateTime expiresAfter;

    private CertificateQueryBuilder() { }

    public static CertificateQueryBuilder create() {
        return new CertificateQueryBuilder();
    }

    public CertificateQueryBuilder serialNumber(String serialNumber) { this.serialNumber = serialNumber; return this; }
    public CertificateQueryBuilder name(String name) { this.name = name; return this; }
    public CertificateQueryBuilder type(KsefCertificateType type) { this.type = type; return this; }
    public CertificateQueryBuilder status(CertificateStatus status) { this.status = status; return this; }
    public CertificateQueryBuilder expiresAfter(OffsetDateTime expiresAfter) { this.expiresAfter = expiresAfter; return this; }

    public CertificateQueryBuilder toBuilder() {
        CertificateQueryBuilder copy = new CertificateQueryBuilder();
        copy.serialNumber = this.serialNumber;
        copy.name = this.name;
        copy.type = this.type;
        copy.status = this.status;
        copy.expiresAfter = this.expiresAfter;
        return copy;
    }

    public CertificateQueryRequest build() {
        return new CertificateQueryRequest(serialNumber, name, type, status, expiresAfter);
    }
}
