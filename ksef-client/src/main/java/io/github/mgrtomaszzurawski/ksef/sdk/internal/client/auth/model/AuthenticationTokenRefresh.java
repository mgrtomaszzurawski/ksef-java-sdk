/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenRefreshResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.mapping.AuthMappers;
import org.jspecify.annotations.Nullable;

/**
 * Result of refreshing an access token.
 *
 * @param accessToken the new access token (null on partial-refresh failure)
 *
 * @since 1.0.0
 */
public record AuthenticationTokenRefresh(@Nullable TokenInfo accessToken) {

    public static AuthenticationTokenRefresh from(AuthenticationTokenRefreshResponseRaw rawValue) {
        return new AuthenticationTokenRefresh(AuthMappers.toTokenInfo(rawValue.getAccessToken()));
    }
}
