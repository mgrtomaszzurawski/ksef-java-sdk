/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitsRaw;

/**
 * Effective API rate limits for all operation types.
 *
 * @param onlineSession online session operations
 * @param batchSession batch session operations
 * @param invoiceSend invoice send operations
 * @param invoiceStatus invoice status checks
 * @param sessionList session list queries
 * @param sessionInvoiceList session invoice list queries
 * @param sessionMisc miscellaneous session operations
 * @param invoiceMetadata invoice metadata queries
 * @param invoiceExport invoice export operations
 * @param invoiceExportStatus export status checks
 * @param invoiceDownload invoice download operations
 * @param other other operations
 */
public record ApiRateLimits(
        RateLimitValues onlineSession,
        RateLimitValues batchSession,
        RateLimitValues invoiceSend,
        RateLimitValues invoiceStatus,
        RateLimitValues sessionList,
        RateLimitValues sessionInvoiceList,
        RateLimitValues sessionMisc,
        RateLimitValues invoiceMetadata,
        RateLimitValues invoiceExport,
        RateLimitValues invoiceExportStatus,
        RateLimitValues invoiceDownload,
        RateLimitValues other) {

    public static ApiRateLimits from(EffectiveApiRateLimitsRaw raw) {
        return new ApiRateLimits(
                RateLimitValues.from(raw.getOnlineSession()),
                RateLimitValues.from(raw.getBatchSession()),
                RateLimitValues.from(raw.getInvoiceSend()),
                RateLimitValues.from(raw.getInvoiceStatus()),
                RateLimitValues.from(raw.getSessionList()),
                RateLimitValues.from(raw.getSessionInvoiceList()),
                RateLimitValues.from(raw.getSessionMisc()),
                RateLimitValues.from(raw.getInvoiceMetadata()),
                RateLimitValues.from(raw.getInvoiceExport()),
                RateLimitValues.from(raw.getInvoiceExportStatus()),
                RateLimitValues.from(raw.getInvoiceDownload()),
                RateLimitValues.from(raw.getOther()));
    }
}
