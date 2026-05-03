/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
        EnrollCertificateRequestRaw rawValue = new EnrollCertificateRequestRaw();
        rawValue.setCertificateName(request.certificateName());
        rawValue.setCertificateType(toKsefCertificateTypeRaw(request.certificateType()));
        rawValue.setCsr(request.csr());
        if (request.validFrom() != null) {
            rawValue.setValidFrom(request.validFrom());
        }
        return rawValue;
    }

    public static QueryCertificatesRequestRaw toQueryCertificatesRequestRaw(CertificateQueryRequest request) {
        QueryCertificatesRequestRaw rawValue = new QueryCertificatesRequestRaw();
        if (request.serialNumber() != null) {
            rawValue.setCertificateSerialNumber(request.serialNumber());
        }
        if (request.name() != null) {
            rawValue.setName(request.name());
        }
        if (request.type() != null) {
            rawValue.setType(toKsefCertificateTypeRaw(request.type()));
        }
        if (request.status() != null) {
            rawValue.setStatus(toCertificateListItemStatusRaw(request.status()));
        }
        if (request.expiresAfter() != null) {
            rawValue.setExpiresAfter(request.expiresAfter());
        }
        return rawValue;
    }

    public static CertificateEnrollmentData toCertificateEnrollmentData(CertificateEnrollmentDataResponseRaw rawValue) {
        return new CertificateEnrollmentData(
                rawValue.getCommonName(),
                rawValue.getCountryName(),
                rawValue.getGivenName(),
                rawValue.getSurname(),
                rawValue.getSerialNumber(),
                rawValue.getUniqueIdentifier(),
                rawValue.getOrganizationName(),
                rawValue.getOrganizationIdentifier());
    }

    public static CertificateEnrollmentStatus toCertificateEnrollmentStatus(CertificateEnrollmentStatusResponseRaw rawValue) {
        return new CertificateEnrollmentStatus(
                rawValue.getRequestDate(),
                CommonMappers.toStatusInfo(rawValue.getStatus()),
                rawValue.getCertificateSerialNumber());
    }

    public static CertificateLimit toCertificateLimit(CertificateLimitRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new CertificateLimit(rawValue.getRemaining(), rawValue.getLimit());
    }

    public static CertificateLimits toCertificateLimits(CertificateLimitsResponseRaw rawValue) {
        return new CertificateLimits(
                rawValue.getCanRequest(),
                CertificatesMappers.toCertificateLimit(rawValue.getEnrollment()),
                CertificatesMappers.toCertificateLimit(rawValue.getCertificate()));
    }

    public static CertificateListItem toCertificateListItem(CertificateListItemRaw rawValue) {
        String subIdType = null;
        String subIdValue = null;
        if (rawValue.getSubjectIdentifier() != null) {
            subIdType = rawValue.getSubjectIdentifier().getType() != null
                    ? rawValue.getSubjectIdentifier().getType().getValue() : null;
            subIdValue = rawValue.getSubjectIdentifier().getValue();
        }
        return new CertificateListItem(
                rawValue.getCertificateSerialNumber(),
                rawValue.getName(),
                rawValue.getType().getValue(),
                rawValue.getCommonName(),
                rawValue.getStatus().getValue(),
                subIdType,
                subIdValue,
                rawValue.getValidFrom(),
                rawValue.getValidTo(),
                rawValue.getLastUseDate(),
                rawValue.getRequestDate());
    }

    public static CertificateQueryResult toCertificateQueryResult(QueryCertificatesResponseRaw rawValue) {
        List<CertificateListItem> mapped = rawValue.getCertificates().stream().map(CertificatesMappers::toCertificateListItem).toList();
        return new CertificateQueryResult(mapped, rawValue.getHasMore());
    }

    public static EnrollCertificateResult toEnrollCertificateResult(EnrollCertificateResponseRaw rawValue) {
        return new EnrollCertificateResult(rawValue.getReferenceNumber(), rawValue.getTimestamp());
    }

    public static RetrieveCertificatesResult toRetrieveCertificatesResult(RetrieveCertificatesResponseRaw rawValue) {
        List<RetrievedCertificate> mapped = rawValue.getCertificates().stream().map(CertificatesMappers::toRetrievedCertificate).toList();
        return new RetrieveCertificatesResult(mapped);
    }

    public static RetrievedCertificate toRetrievedCertificate(RetrieveCertificatesListItemRaw rawValue) {
        return new RetrievedCertificate(
                rawValue.getCertificate(),
                rawValue.getCertificateName(),
                rawValue.getCertificateSerialNumber(),
                rawValue.getCertificateType().getValue());
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
