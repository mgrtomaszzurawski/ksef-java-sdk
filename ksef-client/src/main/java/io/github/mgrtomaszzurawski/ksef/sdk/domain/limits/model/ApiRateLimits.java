/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

/**
 * Per-operation HTTP rate limits the server enforces on the current
 * {@code (context, IP)} tuple. Twelve categories cover the entire KSeF
 * endpoint surface; each carries three sliding-window caps (see
 * {@link RateLimitValues}).
 *
 * <p>All twelve fields are populated by the server (server marks each
 * required); the {@link Nullable} annotation guards against malformed
 * responses. Treat any null observation as a server contract drift,
 * not as "no limit on this category".
 *
 * <p>Exceeding any window returns HTTP 429 with {@code Retry-After};
 * the SDK retries through {@code RetryPolicy} and surfaces exhaustion
 * as {@code KsefRateLimitException}.
 *
 * @param onlineSession opening and closing online (interactive) sessions
 *     ({@code POST /sessions/online}, {@code POST /sessions/{ref}/close}).
 *     Typical demo: 100 / 300 / 1200.
 * @param batchSession opening and closing batch sessions
 *     ({@code POST /sessions/batch}, {@code POST /sessions/{ref}/close}).
 *     Typical demo: 100 / 200 / 600.
 * @param invoiceSend sending individual invoices into an open session
 *     ({@code POST /sessions/{ref}/invoices}). Separate budget from
 *     {@link #onlineSession()} — high-throughput senders are limited
 *     here, not on session lifecycle. Typical demo: 100 / 300 / 1800.
 * @param invoiceStatus polling status of a sent invoice or session
 *     ({@code GET /sessions/{ref}/invoices/{ref}}). Generous budget
 *     since polling drives the SDK's terminal-status loops. Typical
 *     demo: 300 / 1200 / 12000.
 * @param sessionList listing prior sessions via
 *     {@code GET /sessions}.
 * @param sessionInvoiceList listing invoices inside a session via
 *     {@code GET /sessions/{ref}/invoices}.
 * @param sessionMisc miscellaneous session endpoints not covered by
 *     the more specific categories (e.g. metadata queries on a session
 *     reference).
 * @param invoiceMetadata invoice metadata queries
 *     ({@code GET /invoices/query/metadata}, etc.).
 * @param invoiceExport export job creation
 *     ({@code POST /invoices/exports/async}).
 * @param invoiceExportStatus polling export job status
 *     ({@code GET /invoices/exports/async/{ref}}).
 * @param invoiceDownload downloading the export package payload
 *     ({@code GET /invoices/exports/{ref}/download}).
 * @param other catch-all for endpoint groups not assigned to a
 *     specific category above (auth, certificates, permissions,
 *     tokens, limits — all share this budget).
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
