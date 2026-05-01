/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenRefreshResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.TokenInfo;

/**
 * Result of refreshing an access token.
 *
 * @param accessToken the new access token
 */
public record AuthenticationTokenRefresh(TokenInfo accessToken) {

    public static AuthenticationTokenRefresh from(AuthenticationTokenRefreshResponseRaw raw) {
        return new AuthenticationTokenRefresh(TokenInfo.from(raw.getAccessToken()));
    }
}
