/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw;

/**
 * Type of buyer identifier.
 */
public enum BuyerIdentifierType {

    NIP,
    VAT_UE,
    OTHER,
    NONE;

    public static BuyerIdentifierType from(BuyerIdentifierTypeRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case NIP -> NIP;
            case VAT_UE -> VAT_UE;
            case OTHER -> OTHER;
            case NONE -> NONE;
        };
    }
}
