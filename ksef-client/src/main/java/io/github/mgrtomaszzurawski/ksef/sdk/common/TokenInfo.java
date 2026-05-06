/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import java.time.OffsetDateTime;

/**
 * Authentication or access token with validity period.
 *
 * @param token the token string
 * @param validUntil expiration timestamp
 *
 * @since 1.0.0
 */
public record TokenInfo(String token, OffsetDateTime validUntil) {

}
