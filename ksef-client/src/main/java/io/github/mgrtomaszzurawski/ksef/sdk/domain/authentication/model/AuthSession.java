/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;

/**
 * Public auth-session DTO — single entry returned by
 * {@code KsefClient.listAuthSessions()}. Captures one row of the
 * KSeF {@code GET /auth/sessions} response.
 *
 * <p>The internal richer representation
 * ({@code sdk.internal.client.auth.model.AuthenticationListItem}) is
 * not exported via JPMS; this record is the public projection that
 * flattens the auth-method info into a single string and exposes only
 * the fields a consumer needs for session inspection / termination.
 *
 * @param referenceNumber session reference number (KSeF assigns these
 *     in the same {@code YYYYMMDD-XX-...} format as session reference
 *     numbers; not validated by SDK)
 * @param startDate when the session was started server-side
 * @param authenticationMethod human-readable method label
 *     (e.g. {@code "Token"}, {@code "XAdES"}); {@code null} when the
 *     server didn't include method info on the row
 * @param status current session status
 * @param tokenRedeemed whether the operation token has been redeemed
 * @param lastTokenRefreshDate when the access token was last refreshed
 *     ({@code null} when no refresh has occurred)
 * @param refreshTokenValidUntil when the refresh token expires
 *     ({@code null} when no refresh token)
 * @param current whether this is the session that issued the
 *     {@code GET /auth/sessions} call
 */
public record AuthSession(
        String referenceNumber,
        OffsetDateTime startDate,
        String authenticationMethod,
        StatusInfo status,
        boolean tokenRedeemed,
        OffsetDateTime lastTokenRefreshDate,
        OffsetDateTime refreshTokenValidUntil,
        boolean current) {
}
