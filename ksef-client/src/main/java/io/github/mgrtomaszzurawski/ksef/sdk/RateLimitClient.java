/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

/**
 * Client for KSeF API rate limit information.
 */
public final class RateLimitClient {

    private static final String PATH_RATE_LIMITS = "/api/v2/rate-limits";

    private static final String OP_GET_RATE_LIMITS = "getRateLimits";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public RateLimitClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Get the effective API rate limits for the current session.
     *
     * @return effective rate limits including request counts and time windows
     */
    public EffectiveApiRateLimitsRaw getRateLimits() {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_RATE_LIMITS, token,
                EffectiveApiRateLimitsRaw.class, OP_GET_RATE_LIMITS);
    }
}
