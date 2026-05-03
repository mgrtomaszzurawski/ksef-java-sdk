/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemStatusRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateRevocationReasonRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesListItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimit;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrieveCertificatesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;
import java.util.List;

/**
 * Internal mappers from generated {@code *Raw} types to public certificates
 * domain records. Lives in a non-exported package; consumers can't reach it.
 */
public final class CertificatesMappers {

    private CertificatesMappers() { }

    public static EnrollCertificateRequestRaw toEnrollCertificateRequestRaw(CertificateEnrollRequest request) {
        EnrollCertificateRequestRaw raw = new EnrollCertificateRequestRaw();
        raw.setCertificateName(request.certificateName());
        raw.setCertificateType(toKsefCertificateTypeRaw(request.certificateType()));
        raw.setCsr(request.csr());
        if (request.validFrom() != null) {
            raw.setValidFrom(request.validFrom());
        }
        return raw;
    }

    public static QueryCertificatesRequestRaw toQueryCertificatesRequestRaw(CertificateQueryRequest request) {
        QueryCertificatesRequestRaw raw = new QueryCertificatesRequestRaw();
        if (request.serialNumber() != null) {
            raw.setCertificateSerialNumber(request.serialNumber());
        }
        if (request.name() != null) {
            raw.setName(request.name());
        }
        if (request.type() != null) {
            raw.setType(toKsefCertificateTypeRaw(request.type()));
        }
        if (request.status() != null) {
            raw.setStatus(toCertificateListItemStatusRaw(request.status()));
        }
        if (request.expiresAfter() != null) {
            raw.setExpiresAfter(request.expiresAfter());
        }
        return raw;
    }

    public static CertificateEnrollmentData toCertificateEnrollmentData(CertificateEnrollmentDataResponseRaw raw) {
        return new CertificateEnrollmentData(
                raw.getCommonName(),
                raw.getCountryName(),
                raw.getGivenName(),
                raw.getSurname(),
                raw.getSerialNumber(),
                raw.getUniqueIdentifier(),
                raw.getOrganizationName(),
                raw.getOrganizationIdentifier());
    }

    public static CertificateEnrollmentStatus toCertificateEnrollmentStatus(CertificateEnrollmentStatusResponseRaw raw) {
        return new CertificateEnrollmentStatus(
                raw.getRequestDate(),
                CommonMappers.toStatusInfo(raw.getStatus()),
                raw.getCertificateSerialNumber());
    }

    public static CertificateLimit toCertificateLimit(CertificateLimitRaw raw) {
        if (raw == null) {
            return null;
        }
        return new CertificateLimit(raw.getRemaining(), raw.getLimit());
    }

    public static CertificateLimits toCertificateLimits(CertificateLimitsResponseRaw raw) {
        return new CertificateLimits(
                raw.getCanRequest(),
                CertificatesMappers.toCertificateLimit(raw.getEnrollment()),
                CertificatesMappers.toCertificateLimit(raw.getCertificate()));
    }

    public static CertificateListItem toCertificateListItem(CertificateListItemRaw raw) {
        String subIdType = null;
        String subIdValue = null;
        if (raw.getSubjectIdentifier() != null) {
            subIdType = raw.getSubjectIdentifier().getType() != null
                    ? raw.getSubjectIdentifier().getType().getValue() : null;
            subIdValue = raw.getSubjectIdentifier().getValue();
        }
        return new CertificateListItem(
                raw.getCertificateSerialNumber(),
                raw.getName(),
                raw.getType().getValue(),
                raw.getCommonName(),
                raw.getStatus().getValue(),
                subIdType,
                subIdValue,
                raw.getValidFrom(),
                raw.getValidTo(),
                raw.getLastUseDate(),
                raw.getRequestDate());
    }

    public static CertificateQueryResult toCertificateQueryResult(QueryCertificatesResponseRaw raw) {
        List<CertificateListItem> mapped = raw.getCertificates().stream().map(CertificatesMappers::toCertificateListItem).toList();
        return new CertificateQueryResult(mapped, raw.getHasMore());
    }

    public static EnrollCertificateResult toEnrollCertificateResult(EnrollCertificateResponseRaw raw) {
        return new EnrollCertificateResult(raw.getReferenceNumber(), raw.getTimestamp());
    }

    public static RetrieveCertificatesResult toRetrieveCertificatesResult(RetrieveCertificatesResponseRaw raw) {
        List<RetrievedCertificate> mapped = raw.getCertificates().stream().map(CertificatesMappers::toRetrievedCertificate).toList();
        return new RetrieveCertificatesResult(mapped);
    }

    public static RetrievedCertificate toRetrievedCertificate(RetrieveCertificatesListItemRaw raw) {
        return new RetrievedCertificate(
                raw.getCertificate(),
                raw.getCertificateName(),
                raw.getCertificateSerialNumber(),
                raw.getCertificateType().getValue());
    }

    public static CertificateRevocationReasonRaw toCertificateRevocationReasonRaw(CertificateRevocationReason value) {
        return switch (value) {
            case UNSPECIFIED -> CertificateRevocationReasonRaw.UNSPECIFIED;
            case SUPERSEDED -> CertificateRevocationReasonRaw.SUPERSEDED;
            case KEY_COMPROMISE -> CertificateRevocationReasonRaw.KEY_COMPROMISE;
        };
    }

    public static CertificateListItemStatusRaw toCertificateListItemStatusRaw(CertificateStatus value) {
        return switch (value) {
            case ACTIVE -> CertificateListItemStatusRaw.ACTIVE;
            case BLOCKED -> CertificateListItemStatusRaw.BLOCKED;
            case REVOKED -> CertificateListItemStatusRaw.REVOKED;
            case EXPIRED -> CertificateListItemStatusRaw.EXPIRED;
        };
    }

    public static KsefCertificateTypeRaw toKsefCertificateTypeRaw(KsefCertificateType value) {
        return switch (value) {
            case AUTHENTICATION -> KsefCertificateTypeRaw.AUTHENTICATION;
            case OFFLINE -> KsefCertificateTypeRaw.OFFLINE;
        };
    }

}
