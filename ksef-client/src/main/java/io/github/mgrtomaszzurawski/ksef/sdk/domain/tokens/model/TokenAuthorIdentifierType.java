/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Identifier type qualifier for the token-list filter
 * {@code authorIdentifierType} (per spec {@code GET /tokens}).
 *
 * @since 1.0.0
 */
public enum TokenAuthorIdentifierType {

    NIP,
    PESEL,
    FINGERPRINT;

    /** Wire value (PascalCase) expected by the KSeF API. */
    public String wireValue() {
        return switch (this) {
            case NIP -> "Nip";
            case PESEL -> "Pesel";
            case FINGERPRINT -> "Fingerprint";
        };
    }
}
