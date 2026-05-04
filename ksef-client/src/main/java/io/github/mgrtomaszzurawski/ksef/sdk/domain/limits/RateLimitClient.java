/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;

/**
 * Public API for KSeF rate-limit queries.
 *
 * <p>Obtain an instance via {@code KsefClient.rateLimits()}. The implementation
 * lives in {@code sdk.internal.client.limits} and is not exported via JPMS;
 * consumers cannot reference it directly (ADR-016).
 *
 * <p><b>Server-side semantics (Codex round-9 manual-validation A.4.3):</b>
 * KSeF rate limits are enforced as a sliding window per {@code (context, IP)}
 * tuple, server-side. The SDK reacts to rate-limit responses through
 * {@code RetryPolicy} (HTTP 429 with {@code Retry-After}, all three RFC 7231
 * date formats supported) but does <em>not</em> proactively pace calls to
 * stay below the server-side budget — that would require the SDK to
 * maintain per-IP/per-context counters and a synchronised clock against the
 * server, which the spec explicitly says is the server's responsibility
 * (per {@code ksef-docs/limity/limity-api.md}). Treat
 * {@link #getRateLimits()} as a read-only diagnostic, not a budgeting
 * primitive.
 */
public interface RateLimitClient {

    /**
     * Get the effective API rate limits for the current session.
     *
     * @return effective rate limits including request counts and time windows
     */
    ApiRateLimits getRateLimits();
}
