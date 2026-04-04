/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveSubjectLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

/**
 * Client for KSeF session and subject limit queries.
 */
public final class LimitsClient {

    private static final String PATH_CONTEXT_LIMITS = "/api/v2/limits/context";
    private static final String PATH_SUBJECT_LIMITS = "/api/v2/limits/subject";

    private static final String OP_GET_CONTEXT_LIMITS = "getContextLimits";
    private static final String OP_GET_SUBJECT_LIMITS = "getSubjectLimits";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public LimitsClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Get the effective context limits (online and batch session limits).
     *
     * @return context limits with online and batch session constraints
     */
    public EffectiveContextLimitsRaw getContextLimits() {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_CONTEXT_LIMITS, token,
                EffectiveContextLimitsRaw.class, OP_GET_CONTEXT_LIMITS);
    }

    /**
     * Get the effective subject limits (certificate and enrollment limits).
     *
     * @return subject limits with certificate and enrollment constraints
     */
    public EffectiveSubjectLimitsRaw getSubjectLimits() {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_SUBJECT_LIMITS, token,
                EffectiveSubjectLimitsRaw.class, OP_GET_SUBJECT_LIMITS);
    }
}
