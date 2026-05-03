/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

/**
 * Rate-limit override values for one operation category in
 * {@link TestRateLimitsRequest}.
 */
public record TestRateLimitValues(int perSecond, int perMinute, int perHour) {
}
