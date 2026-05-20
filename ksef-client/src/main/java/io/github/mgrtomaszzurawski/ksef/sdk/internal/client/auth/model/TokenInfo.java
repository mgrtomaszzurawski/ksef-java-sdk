/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import java.time.OffsetDateTime;

/**
 * Authentication or access token with validity period. Internal carrier
 * used by AuthClient and its model records; not exported via JPMS.
 *
 * @param token the token string
 * @param validUntil expiration timestamp
 *
 * @since 0.1.0
 */
public record TokenInfo(String token, OffsetDateTime validUntil) {

}
