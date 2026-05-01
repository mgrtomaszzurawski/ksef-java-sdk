/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationMethodInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.AuthenticationMethod;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationOperationStatusResponseRaw;

import java.time.OffsetDateTime;

/**
 * Status of an authentication operation.
 *
 * @param startDate when the authentication was initiated
 * @param authenticationMethod legacy authentication method (deprecated in KSeF API)
 * @param authenticationMethodInfo detailed authentication method information
 * @param status current operation status
 * @param tokenRedeemed whether the operation token has been redeemed
 * @param lastTokenRefreshDate when the token was last refreshed
 * @param refreshTokenValidUntil when the refresh token expires
 */
public record AuthenticationStatus(
        OffsetDateTime startDate,
        AuthenticationMethod authenticationMethod,
        AuthenticationMethodInfo authenticationMethodInfo,
        StatusInfo status,
        Boolean tokenRedeemed,
        OffsetDateTime lastTokenRefreshDate,
        OffsetDateTime refreshTokenValidUntil) {

    public static AuthenticationStatus from(AuthenticationOperationStatusResponseRaw raw) {
        return new AuthenticationStatus(
                raw.getStartDate(),
                AuthenticationMethod.from(raw.getAuthenticationMethod()),
                AuthenticationMethodInfo.from(raw.getAuthenticationMethodInfo()),
                StatusInfo.from(raw.getStatus()),
                raw.getIsTokenRedeemed(),
                raw.getLastTokenRefreshDate(),
                raw.getRefreshTokenValidUntil());
    }
}
