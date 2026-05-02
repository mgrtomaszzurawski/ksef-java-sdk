/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.client.model.TokenInfoRaw;
import java.time.OffsetDateTime;

/**
 * Authentication or access token with validity period.
 *
 * @param token the token string
 * @param validUntil expiration timestamp
 */
public record TokenInfo(String token, OffsetDateTime validUntil) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static TokenInfo from(TokenInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new TokenInfo(raw.getToken(), raw.getValidUntil());
    }
}
