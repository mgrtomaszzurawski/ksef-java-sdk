/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

/**
 * Three sliding-window caps the server enforces on a single operation
 * category. Each field is an independent budget — a caller may hit the
 * per-second cap without exhausting per-minute, and vice versa. All
 * three are required by the server (no nullable shape on the wire).
 *
 * <p>Exceeding any window returns HTTP 429 with a {@code Retry-After}
 * header. The SDK retries 429 responses through
 * {@code RetryPolicy} (RFC 7231 date formats supported) and surfaces
 * exhaustion as {@code KsefRateLimitException} when retry attempts
 * also fail.
 *
 * @param perSecond cap on the rolling 1-second window for the
 *     operation category. Typical demo values range from 100 (session
 *     open/close, invoice send) to 300 (status checks).
 * @param perMinute cap on the rolling 60-second window. Typical demo
 *     values range from 200 (batch open/close) to 1200 (status checks).
 * @param perHour cap on the rolling 3600-second window. Typical demo
 *     values range from 600 (batch open/close) to 12000 (status
 *     checks).
 *
 * @since 0.1.0
 */
public record RateLimitValues(Integer perSecond, Integer perMinute, Integer perHour) {

}
