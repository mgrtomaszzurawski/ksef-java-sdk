/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionLimits;

/**
 * Effective context limits for online and batch sessions.
 *
 * @param onlineSession online session limits
 * @param batchSession batch session limits
 *
 * @since 1.0.0
 */
public record ContextLimits(OnlineSessionLimits onlineSession, BatchSessionLimits batchSession) {

}
