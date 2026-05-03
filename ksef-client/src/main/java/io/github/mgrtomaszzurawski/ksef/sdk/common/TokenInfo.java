/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import java.time.OffsetDateTime;

/**
 * Authentication or access token with validity period.
 *
 * @param token the token string
 * @param validUntil expiration timestamp
 */
public record TokenInfo(String token, OffsetDateTime validUntil) {

}
