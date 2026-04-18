/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectIdentifierTypeRaw;

/**
 * Type of subject identifier for test data limits in KSeF.
 */
public enum TestSubjectIdentifierType {

    NIP,
    PESEL,
    FINGERPRINT;

    public SubjectIdentifierTypeRaw toRaw() {
        return switch (this) {
            case NIP -> SubjectIdentifierTypeRaw.NIP;
            case PESEL -> SubjectIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> SubjectIdentifierTypeRaw.FINGERPRINT;
        };
    }
}
