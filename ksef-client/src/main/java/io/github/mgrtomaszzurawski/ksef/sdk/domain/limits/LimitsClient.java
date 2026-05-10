/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits;

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
 * rate-limit responses through {@code RetryPolicy} (HTTP 429 with
 * {@code Retry-After}, all three RFC 7231 date formats supported) but
 * does <em>not</em> proactively pace calls to stay below the
 * server-side budget. Treat {@link #getRateLimits()} as a read-only
 * diagnostic, not a budgeting primitive.
 *
 * @since 1.0.0
 */
public interface LimitsClient {

    ContextLimits getContextLimits();

    SubjectLimits getSubjectLimits();

    /**
     * Get the effective API rate limits for the current session.
     *
     * @return effective rate limits including request counts and time windows
     */
    ApiRateLimits getRateLimits();
}
