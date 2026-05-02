/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

/**
 * Rate limit values for a specific operation type.
 *
 * @param perSecond maximum requests per second
 * @param perMinute maximum requests per minute
 * @param perHour maximum requests per hour
 */
public record RateLimitValues(Integer perSecond, Integer perMinute, Integer perHour) {

}
