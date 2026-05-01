/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.testdata.model;

import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthenticationContextIdentifierTypeRaw;

/**
 * Identifier type for test data context authentication operations.
 */
public enum TestDataIdentifierType {

    NIP,
    INTERNAL_ID,
    NIP_VAT_UE,
    PEPPOL_ID;

    public TestDataAuthenticationContextIdentifierTypeRaw toRaw() {
        return switch (this) {
            case NIP -> TestDataAuthenticationContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.INTERNAL_ID;
            case NIP_VAT_UE -> TestDataAuthenticationContextIdentifierTypeRaw.NIP_VAT_UE;
            case PEPPOL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.PEPPOL_ID;
        };
    }
}
