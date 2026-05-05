/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.TokenInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;

/**
 * Result of initiating authentication (XAdES or token flow).
 *
 * @param referenceNumber operation reference number for status polling
 * @param authenticationToken the operation token with validity period
 *
 * @since 1.0.0
 */
public record AuthenticationInit(String referenceNumber, TokenInfo authenticationToken) {

    public static AuthenticationInit from(AuthenticationInitResponseRaw rawValue) {
        return new AuthenticationInit(
                rawValue.getReferenceNumber(),
                CommonMappers.toTokenInfo(rawValue.getAuthenticationToken()));
    }
}
