/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

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

}
