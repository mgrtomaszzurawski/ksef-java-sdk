/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.ThirdSubjectIdentifierTypeRaw;

/**
 * Type of third subject identifier.
 */
public enum ThirdSubjectIdentifierType {

    NIP,
    INTERNAL_ID,
    VAT_UE,
    OTHER,
    NONE;

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static ThirdSubjectIdentifierType from(ThirdSubjectIdentifierTypeRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case NIP -> NIP;
            case INTERNAL_ID -> INTERNAL_ID;
            case VAT_UE -> VAT_UE;
            case OTHER -> OTHER;
            case NONE -> NONE;
        };
    }
}
