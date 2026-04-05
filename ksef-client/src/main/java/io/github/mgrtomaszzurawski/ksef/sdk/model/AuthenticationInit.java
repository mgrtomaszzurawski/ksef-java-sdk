/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;

/**
 * Result of initiating authentication (XAdES or token flow).
 *
 * @param referenceNumber operation reference number for status polling
 * @param authenticationToken the operation token with validity period
 */
public record AuthenticationInit(String referenceNumber, TokenInfo authenticationToken) {

    public static AuthenticationInit from(AuthenticationInitResponseRaw raw) {
        return new AuthenticationInit(
                raw.getReferenceNumber(),
                TokenInfo.from(raw.getAuthenticationToken()));
    }
}
