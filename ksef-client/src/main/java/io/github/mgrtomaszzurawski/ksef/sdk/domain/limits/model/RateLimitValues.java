/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EffectiveApiRateLimitValuesRaw;

/**
 * Rate limit values for a specific operation type.
 *
 * @param perSecond maximum requests per second
 * @param perMinute maximum requests per minute
 * @param perHour maximum requests per hour
 */
public record RateLimitValues(Integer perSecond, Integer perMinute, Integer perHour) {

    public static RateLimitValues from(EffectiveApiRateLimitValuesRaw raw) {
        if (raw == null) {
            return null;
        }
        return new RateLimitValues(raw.getPerSecond(), raw.getPerMinute(), raw.getPerHour());
    }
}
