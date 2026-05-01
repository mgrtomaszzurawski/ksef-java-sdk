/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.limits;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.OnlineSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.BatchSessionLimits;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveContextLimitsRaw;

/**
 * Effective context limits for online and batch sessions.
 *
 * @param onlineSession online session limits
 * @param batchSession batch session limits
 */
public record ContextLimits(OnlineSessionLimits onlineSession, BatchSessionLimits batchSession) {

    public static ContextLimits from(EffectiveContextLimitsRaw raw) {
        return new ContextLimits(
                OnlineSessionLimits.from(raw.getOnlineSession()),
                BatchSessionLimits.from(raw.getBatchSession()));
    }
}
