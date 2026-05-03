/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthenticationContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;

/**
 * Internal mappers for testdata domain types. Non-exported package.
 */
public final class TestdataMappers {

    private TestdataMappers() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsRevokeRequestRaw toTestDataPermissionsRevokeRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsRevokeRequestRaw();
        var context = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierRaw();
        context.setType(io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierTypeRaw.NIP);
        context.setValue(request.contextNip());
        rawValue.setContextIdentifier(context);
        var authorized = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierRaw();
        authorized.setType(toTestDataAuthorizedIdentifierTypeRaw(request.authorizedType()));
        authorized.setValue(request.authorizedValue());
        rawValue.setAuthorizedIdentifier(authorized);
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsGrantRequestRaw toTestDataPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsGrantRequestRaw();
        var context = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierRaw();
        context.setType(io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierTypeRaw.NIP);
        context.setValue(request.contextNip());
        rawValue.setContextIdentifier(context);

        var authorized = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierRaw();
        authorized.setType(toTestDataAuthorizedIdentifierTypeRaw(request.authorizedType()));
        authorized.setValue(request.authorizedValue());
        rawValue.setAuthorizedIdentifier(authorized);

        var perms = new java.util.ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionRaw>(request.permissions().size());
        for (var perm : request.permissions()) {
            var permRaw = new io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionRaw();
            permRaw.setPermissionType(toTestDataPermissionTypeRaw(perm.permissionType()));
            permRaw.setDescription(perm.description());
            perms.add(permRaw);
        }
        rawValue.setPermissions(perms);
        return rawValue;
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw toTestDataAuthorizedIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw.NIP;
            case PESEL -> io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SubjectCreateRequestRaw toSubjectCreateRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.SubjectCreateRequestRaw();
        rawValue.setSubjectNip(request.subjectNip());
        rawValue.setSubjectType(toSubjectTypeRaw(request.subjectType()));
        rawValue.setDescription(request.description());
        if (!request.subunits().isEmpty()) {
            var subunits = new java.util.ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.SubunitRaw>(request.subunits().size());
            for (var subunit : request.subunits()) {
                var subRaw = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitRaw();
                subRaw.setSubjectNip(subunit.subjectNip());
                subRaw.setDescription(subunit.description());
                subunits.add(subRaw);
            }
            rawValue.setSubunits(subunits);
        }
        if (request.createdDate() != null) {
            rawValue.setCreatedDate(request.createdDate());
        }
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw toSetRateLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw();
        var rateLimits = new io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitsOverrideRaw();
        if (request.onlineSession() != null) { rateLimits.setOnlineSession(toApiRateLimitValuesOverrideRaw(request.onlineSession())); }
        if (request.batchSession() != null) { rateLimits.setBatchSession(toApiRateLimitValuesOverrideRaw(request.batchSession())); }
        if (request.invoiceSend() != null) { rateLimits.setInvoiceSend(toApiRateLimitValuesOverrideRaw(request.invoiceSend())); }
        if (request.invoiceStatus() != null) { rateLimits.setInvoiceStatus(toApiRateLimitValuesOverrideRaw(request.invoiceStatus())); }
        if (request.sessionList() != null) { rateLimits.setSessionList(toApiRateLimitValuesOverrideRaw(request.sessionList())); }
        if (request.sessionInvoiceList() != null) { rateLimits.setSessionInvoiceList(toApiRateLimitValuesOverrideRaw(request.sessionInvoiceList())); }
        if (request.sessionMisc() != null) { rateLimits.setSessionMisc(toApiRateLimitValuesOverrideRaw(request.sessionMisc())); }
        if (request.invoiceMetadata() != null) { rateLimits.setInvoiceMetadata(toApiRateLimitValuesOverrideRaw(request.invoiceMetadata())); }
        if (request.invoiceExport() != null) { rateLimits.setInvoiceExport(toApiRateLimitValuesOverrideRaw(request.invoiceExport())); }
        if (request.invoiceExportStatus() != null) { rateLimits.setInvoiceExportStatus(toApiRateLimitValuesOverrideRaw(request.invoiceExportStatus())); }
        if (request.invoiceDownload() != null) { rateLimits.setInvoiceDownload(toApiRateLimitValuesOverrideRaw(request.invoiceDownload())); }
        if (request.other() != null) { rateLimits.setOther(toApiRateLimitValuesOverrideRaw(request.other())); }
        rawValue.setRateLimits(rateLimits);
        return rawValue;
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitValuesOverrideRaw toApiRateLimitValuesOverrideRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitValues values) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitValuesOverrideRaw();
        rawValue.setPerSecond(values.perSecond());
        rawValue.setPerMinute(values.perMinute());
        rawValue.setPerHour(values.perHour());
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonCreateRequestRaw toPersonCreateRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.PersonCreateRequestRaw();
        rawValue.setNip(request.nip());
        rawValue.setPesel(request.pesel());
        rawValue.setIsBailiff(request.isBailiff());
        rawValue.setDescription(request.description());
        if (request.isDeceased() != null) {
            rawValue.setIsDeceased(request.isDeceased());
        }
        if (request.createdDate() != null) {
            rawValue.setCreatedDate(request.createdDate());
        }
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw toSetSessionLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw();
        var online = new io.github.mgrtomaszzurawski.ksef.client.model.OnlineSessionContextLimitsOverrideRaw();
        online.setMaxInvoiceSizeInMB(request.onlineSession().maxInvoiceSizeMb());
        online.setMaxInvoiceWithAttachmentSizeInMB(request.onlineSession().maxInvoiceWithAttachmentSizeMb());
        online.setMaxInvoices(request.onlineSession().maxInvoices());
        rawValue.setOnlineSession(online);
        var batch = new io.github.mgrtomaszzurawski.ksef.client.model.BatchSessionContextLimitsOverrideRaw();
        batch.setMaxInvoiceSizeInMB(request.batchSession().maxInvoiceSizeMb());
        batch.setMaxInvoiceWithAttachmentSizeInMB(request.batchSession().maxInvoiceWithAttachmentSizeMb());
        batch.setMaxInvoices(request.batchSession().maxInvoices());
        rawValue.setBatchSession(batch);
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw toSetSubjectLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw();
        rawValue.setSubjectIdentifierType(toSubjectIdentifierTypeRaw(request.subjectIdentifierType()));
        if (request.maxEnrollments() != null) {
            var enrollment = new io.github.mgrtomaszzurawski.ksef.client.model.EnrollmentSubjectLimitsOverrideRaw();
            enrollment.setMaxEnrollments(request.maxEnrollments());
            rawValue.setEnrollment(enrollment);
        }
        if (request.maxCertificates() != null) {
            var certificate = new io.github.mgrtomaszzurawski.ksef.client.model.CertificateSubjectLimitsOverrideRaw();
            certificate.setMaxCertificates(request.maxCertificates());
            rawValue.setCertificate(certificate);
        }
        return rawValue;
    }

    public static TestDataAuthenticationContextIdentifierTypeRaw toTestDataAuthenticationContextIdentifierTypeRaw(TestDataIdentifierType value) {
        return switch (value) {
            case NIP -> TestDataAuthenticationContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.INTERNAL_ID;
            case NIP_VAT_UE -> TestDataAuthenticationContextIdentifierTypeRaw.NIP_VAT_UE;
            case PEPPOL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.PEPPOL_ID;
        };
    }

    public static TestDataPermissionTypeRaw toTestDataPermissionTypeRaw(TestDataPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> TestDataPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> TestDataPermissionTypeRaw.INVOICE_WRITE;
            case INTROSPECTION -> TestDataPermissionTypeRaw.INTROSPECTION;
            case CREDENTIALS_READ -> TestDataPermissionTypeRaw.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> TestDataPermissionTypeRaw.CREDENTIALS_MANAGE;
            case ENFORCEMENT_OPERATIONS -> TestDataPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
            case SUBUNIT_MANAGE -> TestDataPermissionTypeRaw.SUBUNIT_MANAGE;
        };
    }

    public static SubjectIdentifierTypeRaw toSubjectIdentifierTypeRaw(TestSubjectIdentifierType value) {
        return switch (value) {
            case NIP -> SubjectIdentifierTypeRaw.NIP;
            case PESEL -> SubjectIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> SubjectIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static SubjectTypeRaw toSubjectTypeRaw(TestSubjectType value) {
        return switch (value) {
            case ENFORCEMENT_AUTHORITY -> SubjectTypeRaw.ENFORCEMENT_AUTHORITY;
            case VAT_GROUP -> SubjectTypeRaw.VAT_GROUP;
            case JST -> SubjectTypeRaw.JST;
        };
    }

}
