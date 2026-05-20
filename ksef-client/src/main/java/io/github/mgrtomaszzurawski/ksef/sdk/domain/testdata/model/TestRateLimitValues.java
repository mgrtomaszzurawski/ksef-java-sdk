/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

/**
 * Rate-limit override values for one operation category in
 * {@link TestRateLimitsRequest}.
 *
 * @since 0.1.0
 */
public record TestRateLimitValues(int perSecond, int perMinute, int perHour) {
}
