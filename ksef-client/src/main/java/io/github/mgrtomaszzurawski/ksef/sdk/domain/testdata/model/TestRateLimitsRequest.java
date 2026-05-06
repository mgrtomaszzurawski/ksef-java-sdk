/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code TestDataClient.setRateLimits(...)}. Each non-null
 * field overrides the corresponding category; null leaves the default.
 *
 * @since 1.0.0
 */
public record TestRateLimitsRequest(
        @Nullable TestRateLimitValues onlineSession,
        @Nullable TestRateLimitValues batchSession,
        @Nullable TestRateLimitValues invoiceSend,
        @Nullable TestRateLimitValues invoiceStatus,
        @Nullable TestRateLimitValues sessionList,
        @Nullable TestRateLimitValues sessionInvoiceList,
        @Nullable TestRateLimitValues sessionMisc,
        @Nullable TestRateLimitValues invoiceMetadata,
        @Nullable TestRateLimitValues invoiceExport,
        @Nullable TestRateLimitValues invoiceExportStatus,
        @Nullable TestRateLimitValues invoiceDownload,
        @Nullable TestRateLimitValues other) {
}
