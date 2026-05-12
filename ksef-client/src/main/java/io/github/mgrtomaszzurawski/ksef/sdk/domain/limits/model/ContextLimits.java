/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

/**
 * Effective context limits for online and batch sessions.
 *
 * @param onlineSession online session limits
 * @param batchSession batch session limits
 *
 * @since 1.0.0
 */
public record ContextLimits(@Nullable OnlineSessionLimits onlineSession, @Nullable BatchSessionLimits batchSession) {

}
