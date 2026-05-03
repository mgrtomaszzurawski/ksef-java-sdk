/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitValuesRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollmentEffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.RateLimitValues;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;

/**
 * Internal mappers from generated {@code *Raw} types to public limits
 * domain records. Lives in a non-exported package; consumers can't reach it.
 */
public final class LimitsMappers {

    private LimitsMappers() { }

    public static ApiRateLimits toApiRateLimits(EffectiveApiRateLimitsRaw rawValue) {
        return new ApiRateLimits(
                LimitsMappers.toRateLimitValues(rawValue.getOnlineSession()),
                LimitsMappers.toRateLimitValues(rawValue.getBatchSession()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceSend()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceStatus()),
                LimitsMappers.toRateLimitValues(rawValue.getSessionList()),
                LimitsMappers.toRateLimitValues(rawValue.getSessionInvoiceList()),
                LimitsMappers.toRateLimitValues(rawValue.getSessionMisc()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceMetadata()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceExport()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceExportStatus()),
                LimitsMappers.toRateLimitValues(rawValue.getInvoiceDownload()),
                LimitsMappers.toRateLimitValues(rawValue.getOther()));
    }

    public static ContextLimits toContextLimits(EffectiveContextLimitsRaw rawValue) {
        return new ContextLimits(
                InvoicingMappers.toOnlineSessionLimits(rawValue.getOnlineSession()),
                InvoicingMappers.toBatchSessionLimits(rawValue.getBatchSession()));
    }

    public static RateLimitValues toRateLimitValues(EffectiveApiRateLimitValuesRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new RateLimitValues(rawValue.getPerSecond(), rawValue.getPerMinute(), rawValue.getPerHour());
    }

    public static SubjectLimits toSubjectLimits(EffectiveSubjectLimitsRaw rawValue) {
        EnrollmentEffectiveSubjectLimitsRaw enrollment = rawValue.getEnrollment();
        CertificateEffectiveSubjectLimitsRaw certificate = rawValue.getCertificate();
        return new SubjectLimits(
                enrollment != null ? enrollment.getMaxEnrollments() : null,
                certificate != null ? certificate.getMaxCertificates() : null);
    }

}
