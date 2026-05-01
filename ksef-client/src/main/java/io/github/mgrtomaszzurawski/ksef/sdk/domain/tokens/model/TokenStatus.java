/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenStatusRaw;

/**
 * Lifecycle status of a KSeF API token.
 */
public enum TokenStatus {

    PENDING,
    ACTIVE,
    REVOKING,
    REVOKED,
    FAILED;

    public static TokenStatus from(AuthenticationTokenStatusRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case PENDING -> PENDING;
            case ACTIVE -> ACTIVE;
            case REVOKING -> REVOKING;
            case REVOKED -> REVOKED;
            case FAILED -> FAILED;
        };
    }
}
