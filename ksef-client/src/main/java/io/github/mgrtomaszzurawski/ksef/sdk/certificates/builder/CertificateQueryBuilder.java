/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import java.time.OffsetDateTime;

/**
 * Builder for KSeF certificate query requests.
 * <p>
 * All fields are optional filters. An empty builder queries all certificates.
 * <p>
 * Usage:
 * <pre>{@code
 * QueryCertificatesRequestRaw request = CertificateQueryBuilder.create()
 *     .status(CertificateStatus.ACTIVE)
 *     .type(KsefCertificateType.AUTHENTICATION)
 *     .build();
 * }</pre>
 */
public final class CertificateQueryBuilder {

    private String serialNumber;
    private String name;
    private KsefCertificateType type;
    private CertificateStatus status;
    private OffsetDateTime expiresAfter;

    private CertificateQueryBuilder() {
    }

    /**
     * Create a new query builder with no filters.
     */
    public static CertificateQueryBuilder create() {
        return new CertificateQueryBuilder();
    }

    /**
     * Filter by exact certificate serial number.
     *
     * @param serialNumber the serial number to match
     */
    public CertificateQueryBuilder serialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    /**
     * Filter by certificate name (partial match / contains).
     *
     * @param name the name substring to search for
     */
    public CertificateQueryBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Filter by certificate type.
     *
     * @param type Authentication or Offline
     */
    public CertificateQueryBuilder type(KsefCertificateType type) {
        this.type = type;
        return this;
    }

    /**
     * Filter by certificate status.
     *
     * @param status Active, Blocked, Revoked, or Expired
     */
    public CertificateQueryBuilder status(CertificateStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Filter certificates that expire after the given date.
     *
     * @param expiresAfter the cutoff date
     */
    public CertificateQueryBuilder expiresAfter(OffsetDateTime expiresAfter) {
        this.expiresAfter = expiresAfter;
        return this;
    }

    /**
     * Build the query request.
     *
     * @return the request ready to pass to {@code CertificateClient.query()}
     */
    public QueryCertificatesRequestRaw build() {
        QueryCertificatesRequestRaw request = new QueryCertificatesRequestRaw();
        if (serialNumber != null) {
            request.setCertificateSerialNumber(serialNumber);
        }
        if (name != null) {
            request.setName(name);
        }
        if (type != null) {
            request.setType(type.toRaw());
        }
        if (status != null) {
            request.setStatus(status.toRaw());
        }
        if (expiresAfter != null) {
            request.setExpiresAfter(expiresAfter);
        }
        return request;
    }
}
