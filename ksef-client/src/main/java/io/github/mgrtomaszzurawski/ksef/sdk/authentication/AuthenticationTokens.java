/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication;
import io.github.mgrtomaszzurawski.ksef.sdk.common.TokenInfo;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokensResponseRaw;

/**
 * Access and refresh tokens obtained by redeeming the operation token.
 *
 * @param accessToken the access token for API calls
 * @param refreshToken the refresh token for obtaining new access tokens
 */
public record AuthenticationTokens(TokenInfo accessToken, TokenInfo refreshToken) {

    public static AuthenticationTokens from(AuthenticationTokensResponseRaw raw) {
        return new AuthenticationTokens(
                TokenInfo.from(raw.getAccessToken()),
                TokenInfo.from(raw.getRefreshToken()));
    }
}
