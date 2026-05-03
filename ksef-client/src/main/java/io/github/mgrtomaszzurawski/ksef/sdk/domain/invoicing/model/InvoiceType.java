/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

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

}
