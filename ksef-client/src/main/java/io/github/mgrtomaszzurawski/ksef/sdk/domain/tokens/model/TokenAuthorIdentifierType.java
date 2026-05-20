/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Identifier type qualifier for the token-list filter
 * {@code authorIdentifierType} (per spec {@code GET /tokens}).
 *
 * @since 0.1.0
 */
public enum TokenAuthorIdentifierType {

    NIP("Nip"),
    PESEL("Pesel"),
    FINGERPRINT("Fingerprint");

    private final String wireValue;

    TokenAuthorIdentifierType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Wire value (PascalCase) expected by the KSeF API. */
    public String wireValue() {
        return wireValue;
    }
}
