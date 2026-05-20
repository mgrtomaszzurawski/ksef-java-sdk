/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Type of invoice in KSeF.
 *
 * @since 0.1.0
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

    /**
     * Whether this type denotes a correction invoice (any of the {@code KOR_*}
     * variants or the bare {@code KOR}). Lets consumers branch on correction
     * vs. original without enumerating every {@code KOR_*} constant — the
     * enum may grow new correction variants in future KSeF schema revisions.
     */
    public boolean isCorrection() {
        return this == KOR || this == KOR_ZAL || this == KOR_ROZ
                || this == KOR_PEF || this == KOR_VAT_RR;
    }
}
