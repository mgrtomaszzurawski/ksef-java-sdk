/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw;

/**
 * Type of invoice in KSeF.
 */
public enum InvoiceType {

    VAT,
    ZAL,
    KOR,
    ROZ,
    UPR,
    KOR_ZAL,
    KOR_ROZ,
    VAT_PEF,
    VAT_PEF_SP,
    KOR_PEF,
    VAT_RR,
    KOR_VAT_RR;

    public static InvoiceType from(InvoiceTypeRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case VAT -> VAT;
            case ZAL -> ZAL;
            case KOR -> KOR;
            case ROZ -> ROZ;
            case UPR -> UPR;
            case KOR_ZAL -> KOR_ZAL;
            case KOR_ROZ -> KOR_ROZ;
            case VAT_PEF -> VAT_PEF;
            case VAT_PEF_SP -> VAT_PEF_SP;
            case KOR_PEF -> KOR_PEF;
            case VAT_RR -> VAT_RR;
            case KOR_VAT_RR -> KOR_VAT_RR;
        };
    }
}
