/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationMethodInfoRaw;

/**
 * Detailed information about the authentication method.
 *
 * @param category authentication method category
 * @param code authentication method code string
 * @param displayName human-readable display name
 */
public record AuthenticationMethodInfo(
        AuthenticationMethodCategory category,
        String code,
        String displayName) {

    public static AuthenticationMethodInfo from(AuthenticationMethodInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new AuthenticationMethodInfo(
                AuthenticationMethodCategory.from(raw.getCategory()),
                raw.getCode(),
                raw.getDisplayName());
    }
}
