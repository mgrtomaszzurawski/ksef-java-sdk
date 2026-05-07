/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationMethodInfoRaw;
import org.jspecify.annotations.Nullable;

/**
 * Detailed information about the authentication method.
 *
 * @param category authentication method category
 * @param code authentication method code string
 * @param displayName human-readable display name
 *
 * @since 1.0.0
 */
public record AuthenticationMethodInfo(
        AuthenticationMethodCategory category,
        String code,
        @Nullable String displayName) {

    public static @Nullable AuthenticationMethodInfo from(@Nullable AuthenticationMethodInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new AuthenticationMethodInfo(
                AuthenticationMethodCategory.from(raw.getCategory()),
                raw.getCode(),
                raw.getDisplayName());
    }
}
