/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.LimitsClient;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.mapping.LimitsMappers;

/**
 * Client for KSeF session and subject limit queries.
 */
public final class LimitsClientImpl implements LimitsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitsClientImpl.class);
    private static final String LOG_CALL = "→ {}";

    private static final String PATH_CONTEXT_LIMITS = ApiPaths.LIMITS + "/context";
    private static final String PATH_SUBJECT_LIMITS = ApiPaths.LIMITS + "/subject";

    private static final String OP_GET_CONTEXT_LIMITS = "getContextLimits";
    private static final String OP_GET_SUBJECT_LIMITS = "getSubjectLimits";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public LimitsClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef.runtime());
        this.sessionContext = ksef.runtime().sessionContext();
    }

    /**
     * Get the effective context limits (online and batch session limits).
     *
     * @return context limits with online and batch session constraints
     */
    @Override
    public ContextLimits getContextLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_CONTEXT_LIMITS);
        String token = sessionContext.token();
        EffectiveContextLimitsRaw raw = http.getAuthenticated(PATH_CONTEXT_LIMITS, token,
                EffectiveContextLimitsRaw.class, OP_GET_CONTEXT_LIMITS);
        return LimitsMappers.toContextLimits(raw);
    }

    /**
     * Get the effective subject limits (certificate and enrollment limits).
     *
     * @return subject limits with certificate and enrollment constraints
     */
    @Override
    public SubjectLimits getSubjectLimits() {
        LOGGER.debug(LOG_CALL, OP_GET_SUBJECT_LIMITS);
        String token = sessionContext.token();
        EffectiveSubjectLimitsRaw raw = http.getAuthenticated(PATH_SUBJECT_LIMITS, token,
                EffectiveSubjectLimitsRaw.class, OP_GET_SUBJECT_LIMITS);
        return LimitsMappers.toSubjectLimits(raw);
    }
}
