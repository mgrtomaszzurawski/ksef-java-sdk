/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.RateLimitClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RateLimitClient}. Constructed by {@code KsefClient};
 * lives in non-exported {@code sdk.internal.client.limits} (ADR-016).
 */
public final class RateLimitClientImpl implements RateLimitClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitClientImpl.class);
    private static final String LOG_CALL = "→ {}";

    private static final String PATH_RATE_LIMITS = ApiPaths.RATE_LIMITS;
    private static final String OP_GET_RATE_LIMITS = "getRateLimits";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public RateLimitClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef.runtime());
        this.sessionContext = ksef.runtime().sessionContext();
    }

    @Override
    public ApiRateLimits getRateLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_RATE_LIMITS);
        String token = sessionContext.token();
        EffectiveApiRateLimitsRaw raw = http.getAuthenticated(PATH_RATE_LIMITS, token,
                EffectiveApiRateLimitsRaw.class, OP_GET_RATE_LIMITS);
        return ApiRateLimits.from(raw);
    }
}
