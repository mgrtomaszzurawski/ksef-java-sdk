/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

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
 *
 * @since 1.0.0
 */
public record ApiRateLimits(
        @Nullable RateLimitValues onlineSession,
        @Nullable RateLimitValues batchSession,
        @Nullable RateLimitValues invoiceSend,
        @Nullable RateLimitValues invoiceStatus,
        @Nullable RateLimitValues sessionList,
        @Nullable RateLimitValues sessionInvoiceList,
        @Nullable RateLimitValues sessionMisc,
        @Nullable RateLimitValues invoiceMetadata,
        @Nullable RateLimitValues invoiceExport,
        @Nullable RateLimitValues invoiceExportStatus,
        @Nullable RateLimitValues invoiceDownload,
        @Nullable RateLimitValues other) {

}
