/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Builder for KSeF certificate query requests. All fields are optional filters.
 *
 * @since 1.0.0
 */
public final class CertificateQueryBuilder {

    private @Nullable CertificateSerialNumber serialNumber;
    private @Nullable String name;
    private @Nullable KsefCertificateType type;
    private @Nullable CertificateStatus status;
    private @Nullable OffsetDateTime expiresAfter;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private CertificateQueryBuilder() { }

    public static CertificateQueryBuilder create() {
        return new CertificateQueryBuilder();
    }

    public CertificateQueryBuilder serialNumber(CertificateSerialNumber serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }
    public CertificateQueryBuilder name(String name) { this.name = name; return this; }
    public CertificateQueryBuilder type(KsefCertificateType type) { this.type = type; return this; }
    public CertificateQueryBuilder status(CertificateStatus status) { this.status = status; return this; }
    public CertificateQueryBuilder expiresAfter(OffsetDateTime expiresAfter) { this.expiresAfter = expiresAfter; return this; }

    /**
     * Set the page offset (0-based) for the snapshot {@code query()}
     * call. Ignored by {@code streamCertificates(...)} which manages
     * paging internally.
     */
    public CertificateQueryBuilder pageOffset(int pageOffset) { this.pageOffset = pageOffset; return this; }

    /**
     * Set the page size for both {@code query()} and the per-page fetch
     * inside {@code streamCertificates(...)}.
     */
    public CertificateQueryBuilder pageSize(int pageSize) { this.pageSize = pageSize; return this; }

    public @Nullable Integer pageOffsetValue() { return pageOffset; }

    public @Nullable Integer pageSizeValue() { return pageSize; }

    public CertificateQueryBuilder toBuilder() {
        CertificateQueryBuilder copy = new CertificateQueryBuilder();
        copy.serialNumber = this.serialNumber;
        copy.name = this.name;
        copy.type = this.type;
        copy.status = this.status;
        copy.expiresAfter = this.expiresAfter;
        copy.pageOffset = this.pageOffset;
        copy.pageSize = this.pageSize;
        return copy;
    }

    public CertificateQueryRequest build() {
        return new CertificateQueryRequest(serialNumber, name, type, status, expiresAfter, pageOffset, pageSize);
    }
}
