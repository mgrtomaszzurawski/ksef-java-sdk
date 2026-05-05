/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;

/**
 * Status of an authentication operation.
 *
 * @param startDate when the authentication was initiated
 * @param authenticationMethodInfo detailed authentication method information
 * @param status current operation status
 * @param tokenRedeemed whether the operation token has been redeemed
 * @param lastTokenRefreshDate when the token was last refreshed
 * @param refreshTokenValidUntil when the refresh token expires
 *
 * @since 1.0.0
 */
public record AuthenticationStatus(
        OffsetDateTime startDate,
        AuthenticationMethodInfo authenticationMethodInfo,
        StatusInfo status,
        Boolean tokenRedeemed,
        OffsetDateTime lastTokenRefreshDate,
        OffsetDateTime refreshTokenValidUntil) {

    public static AuthenticationStatus from(AuthenticationOperationStatusResponseRaw rawValue) {
        return new AuthenticationStatus(
                rawValue.getStartDate(),
                AuthenticationMethodInfo.from(rawValue.getAuthenticationMethodInfo()),
                CommonMappers.toStatusInfo(rawValue.getStatus()),
                rawValue.getIsTokenRedeemed(),
                rawValue.getLastTokenRefreshDate(),
                rawValue.getRefreshTokenValidUntil());
    }
}
