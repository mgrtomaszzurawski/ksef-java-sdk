/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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

    public static ApiRateLimits toApiRateLimits(EffectiveApiRateLimitsRaw raw) {
        return new ApiRateLimits(
                LimitsMappers.toRateLimitValues(raw.getOnlineSession()),
                LimitsMappers.toRateLimitValues(raw.getBatchSession()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceSend()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceStatus()),
                LimitsMappers.toRateLimitValues(raw.getSessionList()),
                LimitsMappers.toRateLimitValues(raw.getSessionInvoiceList()),
                LimitsMappers.toRateLimitValues(raw.getSessionMisc()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceMetadata()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceExport()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceExportStatus()),
                LimitsMappers.toRateLimitValues(raw.getInvoiceDownload()),
                LimitsMappers.toRateLimitValues(raw.getOther()));
    }

    public static ContextLimits toContextLimits(EffectiveContextLimitsRaw raw) {
        return new ContextLimits(
                InvoicingMappers.toOnlineSessionLimits(raw.getOnlineSession()),
                InvoicingMappers.toBatchSessionLimits(raw.getBatchSession()));
    }

    public static RateLimitValues toRateLimitValues(EffectiveApiRateLimitValuesRaw raw) {
        if (raw == null) {
            return null;
        }
        return new RateLimitValues(raw.getPerSecond(), raw.getPerMinute(), raw.getPerHour());
    }

    public static SubjectLimits toSubjectLimits(EffectiveSubjectLimitsRaw raw) {
        EnrollmentEffectiveSubjectLimitsRaw enrollment = raw.getEnrollment();
        CertificateEffectiveSubjectLimitsRaw certificate = raw.getCertificate();
        return new SubjectLimits(
                enrollment != null ? enrollment.getMaxEnrollments() : null,
                certificate != null ? certificate.getMaxCertificates() : null);
    }

}
