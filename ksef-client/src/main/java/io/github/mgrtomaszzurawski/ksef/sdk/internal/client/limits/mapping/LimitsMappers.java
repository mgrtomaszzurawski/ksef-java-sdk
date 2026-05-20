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
import org.jspecify.annotations.Nullable;

/**
 * Internal mappers from generated {@code *Raw} types to public limits
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 0.1.0
 */
public final class LimitsMappers {

    private LimitsMappers() { }

    public static ApiRateLimits toApiRateLimits(EffectiveApiRateLimitsRaw rawValue) {
        return new ApiRateLimits(
                toRateLimitValues(rawValue.getOnlineSession()),
                toRateLimitValues(rawValue.getBatchSession()),
                toRateLimitValues(rawValue.getInvoiceSend()),
                toRateLimitValues(rawValue.getInvoiceStatus()),
                toRateLimitValues(rawValue.getSessionList()),
                toRateLimitValues(rawValue.getSessionInvoiceList()),
                toRateLimitValues(rawValue.getSessionMisc()),
                toRateLimitValues(rawValue.getInvoiceMetadata()),
                toRateLimitValues(rawValue.getInvoiceExport()),
                toRateLimitValues(rawValue.getInvoiceExportStatus()),
                toRateLimitValues(rawValue.getInvoiceDownload()),
                toRateLimitValues(rawValue.getOther()));
    }

    public static ContextLimits toContextLimits(EffectiveContextLimitsRaw rawValue) {
        return new ContextLimits(
                InvoicingMappers.toOnlineSessionLimits(rawValue.getOnlineSession()),
                InvoicingMappers.toBatchSessionLimits(rawValue.getBatchSession()));
    }

    public static @Nullable RateLimitValues toRateLimitValues(@Nullable EffectiveApiRateLimitValuesRaw rawValue) {
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
