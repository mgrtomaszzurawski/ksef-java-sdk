/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.Limits;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.mapping.LimitsMappers;

/**
 * Client for KSeF session and subject limit queries.
 *
 * @since 1.0.0
 */
public final class LimitsImpl implements Limits {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitsImpl.class);
    private static final String LOG_CALL = "→ {}";

    private static final String PATH_CONTEXT_LIMITS = ApiPaths.LIMITS + "/context";
    private static final String PATH_SUBJECT_LIMITS = ApiPaths.LIMITS + "/subject";
    private static final String PATH_RATE_LIMITS = ApiPaths.RATE_LIMITS;

    private static final String OP_GET_CONTEXT_LIMITS = "getContextLimits";
    private static final String OP_GET_SUBJECT_LIMITS = "getSubjectLimits";
    private static final String OP_GET_RATE_LIMITS = "getRateLimits";

    private final HttpSupport http;

    public LimitsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public ContextLimits getContextLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_CONTEXT_LIMITS);
        String token = http.requireToken();
        EffectiveContextLimitsRaw rawValue = http.getAuthenticated(PATH_CONTEXT_LIMITS, token,
                EffectiveContextLimitsRaw.class, OP_GET_CONTEXT_LIMITS);
        return LimitsMappers.toContextLimits(rawValue);
    }

    @Override
    public SubjectLimits getSubjectLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_SUBJECT_LIMITS);
        String token = http.requireToken();
        EffectiveSubjectLimitsRaw rawValue = http.getAuthenticated(PATH_SUBJECT_LIMITS, token,
                EffectiveSubjectLimitsRaw.class, OP_GET_SUBJECT_LIMITS);
        return LimitsMappers.toSubjectLimits(rawValue);
    }

    @Override
    public ApiRateLimits getRateLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_RATE_LIMITS);
        String token = http.requireToken();
        EffectiveApiRateLimitsRaw rawValue = http.getAuthenticated(PATH_RATE_LIMITS, token,
                EffectiveApiRateLimitsRaw.class, OP_GET_RATE_LIMITS);
        return LimitsMappers.toApiRateLimits(rawValue);
    }
}
