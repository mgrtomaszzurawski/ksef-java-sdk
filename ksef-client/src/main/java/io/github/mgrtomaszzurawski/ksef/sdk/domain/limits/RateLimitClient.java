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
 */
public interface RateLimitClient {

    /**
     * Get the effective API rate limits for the current session.
     *
     * @return effective rate limits including request counts and time windows
     */
    ApiRateLimits getRateLimits();
}
