/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;

/**
 * Result of generating a new KSeF API token.
 *
 * @param referenceNumber operation reference number
 * @param token the generated token string
 */
public record GenerateTokenResult(String referenceNumber, String token) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static GenerateTokenResult from(GenerateTokenResponseRaw raw) {
        return new GenerateTokenResult(raw.getReferenceNumber(), raw.getToken());
    }
}
