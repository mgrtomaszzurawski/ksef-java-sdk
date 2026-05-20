/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationListItemRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import java.time.OffsetDateTime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;
import org.jspecify.annotations.Nullable;

/**
 * An authentication session in the session list.
 *
 * @param referenceNumber session reference number
 * @param startDate when the session was started
 * @param authenticationMethodInfo detailed authentication method information
 * @param status current session status
 * @param tokenRedeemed whether the operation token has been redeemed
 * @param lastTokenRefreshDate when the token was last refreshed
 * @param refreshTokenValidUntil when the refresh token expires
 * @param current whether this is the current session
 *
 * @since 0.1.0
 */
public record AuthenticationListItem(
        String referenceNumber,
        OffsetDateTime startDate,
        @Nullable AuthenticationMethodInfo authenticationMethodInfo,
        @Nullable StatusInfo status,
        @Nullable Boolean tokenRedeemed,
        @Nullable OffsetDateTime lastTokenRefreshDate,
        @Nullable OffsetDateTime refreshTokenValidUntil,
        @Nullable Boolean current) {

    public static AuthenticationListItem from(AuthenticationListItemRaw rawValue) {
        return new AuthenticationListItem(
                rawValue.getReferenceNumber(),
                rawValue.getStartDate(),
                AuthenticationMethodInfo.from(rawValue.getAuthenticationMethodInfo()),
                CommonMappers.toStatusInfo(rawValue.getStatus()),
                rawValue.getIsTokenRedeemed(),
                rawValue.getLastTokenRefreshDate(),
                rawValue.getRefreshTokenValidUntil(),
                rawValue.getIsCurrent());
    }
}
