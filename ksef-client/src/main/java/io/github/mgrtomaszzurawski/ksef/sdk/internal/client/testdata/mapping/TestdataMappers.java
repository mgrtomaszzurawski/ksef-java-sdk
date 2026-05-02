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

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw toSetRateLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw();
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
        raw.setRateLimits(rateLimits);
        return raw;
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitValuesOverrideRaw toApiRateLimitValuesOverrideRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitValues values) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitValuesOverrideRaw();
        raw.setPerSecond(values.perSecond());
        raw.setPerMinute(values.perMinute());
        raw.setPerHour(values.perHour());
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonCreateRequestRaw toPersonCreateRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.PersonCreateRequestRaw();
        raw.setNip(request.nip());
        raw.setPesel(request.pesel());
        raw.setIsBailiff(request.isBailiff());
        raw.setDescription(request.description());
        if (request.isDeceased() != null) {
            raw.setIsDeceased(request.isDeceased());
        }
        if (request.createdDate() != null) {
            raw.setCreatedDate(request.createdDate());
        }
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw toSetSessionLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw();
        var online = new io.github.mgrtomaszzurawski.ksef.client.model.OnlineSessionContextLimitsOverrideRaw();
        online.setMaxInvoiceSizeInMB(request.onlineSession().maxInvoiceSizeMb());
        online.setMaxInvoiceWithAttachmentSizeInMB(request.onlineSession().maxInvoiceWithAttachmentSizeMb());
        online.setMaxInvoices(request.onlineSession().maxInvoices());
        raw.setOnlineSession(online);
        var batch = new io.github.mgrtomaszzurawski.ksef.client.model.BatchSessionContextLimitsOverrideRaw();
        batch.setMaxInvoiceSizeInMB(request.batchSession().maxInvoiceSizeMb());
        batch.setMaxInvoiceWithAttachmentSizeInMB(request.batchSession().maxInvoiceWithAttachmentSizeMb());
        batch.setMaxInvoices(request.batchSession().maxInvoices());
        raw.setBatchSession(batch);
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw toSetSubjectLimitsRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw();
        raw.setSubjectIdentifierType(toSubjectIdentifierTypeRaw(request.subjectIdentifierType()));
        if (request.maxEnrollments() != null) {
            var enrollment = new io.github.mgrtomaszzurawski.ksef.client.model.EnrollmentSubjectLimitsOverrideRaw();
            enrollment.setMaxEnrollments(request.maxEnrollments());
            raw.setEnrollment(enrollment);
        }
        if (request.maxCertificates() != null) {
            var certificate = new io.github.mgrtomaszzurawski.ksef.client.model.CertificateSubjectLimitsOverrideRaw();
            certificate.setMaxCertificates(request.maxCertificates());
            raw.setCertificate(certificate);
        }
        return raw;
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
