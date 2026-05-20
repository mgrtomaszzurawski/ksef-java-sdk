/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits;

import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;

/**
 * Client for KSeF limit queries — both business limits (max invoices
 * per online/batch session, retention windows) and HTTP rate limits
 * (sliding-window per (context, IP) tuple).
 *
 * <p>The rate-limit query previously lived on a dedicated
 * {@code RateLimitClient}; both clients have been merged because KSeF
 * docs describe both as "limity" / "limits" — splitting them was
 * over-decomposition (Review Section 11).
 *
 * <p><b>Server-side rate-limit semantics:</b>
 * KSeF rate limits are enforced as a sliding window per
 * {@code (context, IP)} tuple, server-side. The SDK reacts to
 * rate-limit responses through {@link RetryPolicy} (HTTP 429 with
 * {@code Retry-After}, all three RFC 7231 date formats supported) but
 * does <em>not</em> proactively pace calls to stay below the
 * server-side budget. Treat {@link #getRateLimits()} as a read-only
 * diagnostic, not a budgeting primitive.
 *
 * @since 1.0.0
 */
public interface Limits {

    /**
     * Retrieve the maximum-per-session limits the server enforces for
     * online and batch sessions opened under the current authentication
     * context. Each {@link ContextLimits#onlineSession()} and
     * {@link ContextLimits#batchSession()} carries three caps: invoice
     * payload size in MB, invoice-with-attachment size in MB, and total
     * invoices per session.
     *
     * <p>Typical values (KSeF demo, May 2026): 1 MB / 3 MB / 10000
     * invoices for both session types. Concrete numbers vary per
     * environment and per taxpayer — query at runtime rather than
     * hard-coding. Exceeding the size caps surfaces as a wire-level
     * validation error at {@code sendInvoice}; exceeding
     * {@code maxInvoices} blocks further sends in the same session.
     *
     * @return non-null snapshot with both session-type caps populated
     *     (server marks both fields required)
     */
    ContextLimits getContextLimits();

    /**
     * Retrieve the certificate and enrollment quota for the current
     * subject (taxpayer NIP). Both fields are nullable when the server
     * reports "unlimited" — non-null values are hard caps enforced on
     * the {@code POST /certificates/enrollments} and certificate
     * activation flows.
     *
     * <p>Typical values (KSeF demo + production):
     * {@link SubjectLimits#maxEnrollments()} = 12 enrollment requests
     * per calendar month per NIP;
     * {@link SubjectLimits#maxCertificates()} = 6 simultaneously active
     * certificates per NIP. Crossing either cap surfaces as a typed
     * validation error on the certificate flow.
     *
     * @return non-null snapshot; either field may be null if the server
     *     does not advertise the limit (treated as unlimited)
     */
    SubjectLimits getSubjectLimits();

    /**
     * Retrieve the per-operation HTTP rate limits enforced for the
     * current {@code (context, IP)} tuple. Returns 12 limit categories
     * matching KSeF endpoint groups (online-session open/close,
     * batch-session open/close, invoice send, status checks, list
     * queries, export, download, other). Each category carries three
     * sliding-window caps: per-second, per-minute, per-hour.
     *
     * <p>Sliding windows are independent: a caller may hit the per-second
     * cap (e.g. 100 req/s on {@code onlineSession}) without exhausting
     * the per-minute budget (300 req/min), and vice versa.
     *
     * <p>Typical values (KSeF demo, May 2026): {@code onlineSession} =
     * 100/300/1200, {@code batchSession} = 100/200/600,
     * {@code invoiceSend} = 100/300/1800, {@code invoiceStatus} =
     * 300/1200/12000. Concrete values vary per environment and per
     * taxpayer.
     *
     * <p>Exceeding any window returns HTTP 429 with a {@code Retry-After}
     * header. The SDK auto-retries 429 responses through
     * {@link RetryPolicy} (all three RFC 7231 date formats supported)
     * but does <em>not</em> proactively pace calls — treat this method
     * as a read-only diagnostic, not a budgeting primitive.
     *
     * @return non-null snapshot with all 12 categories populated
     *     (server marks every field required)
     */
    ApiRateLimits getRateLimits();
}
