/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;

/**
 * Client for KSeF session and subject limit queries.
 */
public interface LimitsClient {

    ContextLimits getContextLimits();
    SubjectLimits getSubjectLimits();
}
