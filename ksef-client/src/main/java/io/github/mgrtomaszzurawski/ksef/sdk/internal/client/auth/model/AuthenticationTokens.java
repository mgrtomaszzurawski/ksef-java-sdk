/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.TokenInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;

/**
 * Access and refresh tokens obtained by redeeming the operation token.
 *
 * @param accessToken the access token for API calls
 * @param refreshToken the refresh token for obtaining new access tokens
 *
 * @since 1.0.0
 */
public record AuthenticationTokens(TokenInfo accessToken, TokenInfo refreshToken) {

    public static AuthenticationTokens from(AuthenticationTokensResponseRaw rawValue) {
        return new AuthenticationTokens(
                CommonMappers.toTokenInfo(rawValue.getAccessToken()),
                CommonMappers.toTokenInfo(rawValue.getRefreshToken()));
    }
}
