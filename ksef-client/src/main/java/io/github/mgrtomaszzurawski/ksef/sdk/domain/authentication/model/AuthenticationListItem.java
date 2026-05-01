/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationListItemRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;

/**
 * An authentication session in the session list.
 *
 * @param referenceNumber session reference number
 * @param startDate when the session was started
 * @param authenticationMethod legacy authentication method (deprecated in KSeF API)
 * @param authenticationMethodInfo detailed authentication method information
 * @param status current session status
 * @param tokenRedeemed whether the operation token has been redeemed
 * @param lastTokenRefreshDate when the token was last refreshed
 * @param refreshTokenValidUntil when the refresh token expires
 * @param current whether this is the current session
 */
public record AuthenticationListItem(
        String referenceNumber,
        OffsetDateTime startDate,
        AuthenticationMethod authenticationMethod,
        AuthenticationMethodInfo authenticationMethodInfo,
        StatusInfo status,
        Boolean tokenRedeemed,
        OffsetDateTime lastTokenRefreshDate,
        OffsetDateTime refreshTokenValidUntil,
        Boolean current) {

    public static AuthenticationListItem from(AuthenticationListItemRaw raw) {
        return new AuthenticationListItem(
                raw.getReferenceNumber(),
                raw.getStartDate(),
                AuthenticationMethod.from(raw.getAuthenticationMethod()),
                AuthenticationMethodInfo.from(raw.getAuthenticationMethodInfo()),
                StatusInfo.from(raw.getStatus()),
                raw.getIsTokenRedeemed(),
                raw.getLastTokenRefreshDate(),
                raw.getRefreshTokenValidUntil(),
                raw.getIsCurrent());
    }
}
