/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenRefreshResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.TokenInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;

/**
 * Result of refreshing an access token.
 *
 * @param accessToken the new access token
 */
public record AuthenticationTokenRefresh(TokenInfo accessToken) {

    public static AuthenticationTokenRefresh from(AuthenticationTokenRefreshResponseRaw rawValue) {
        return new AuthenticationTokenRefresh(CommonMappers.toTokenInfo(rawValue.getAccessToken()));
    }
}
