/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationMethodRaw;

/**
 * Authentication method used to establish a KSeF session.
 */
public enum AuthenticationMethod {

    TOKEN,
    TRUSTED_PROFILE,
    INTERNAL_CERTIFICATE,
    QUALIFIED_SIGNATURE,
    QUALIFIED_SEAL,
    PERSONAL_SIGNATURE,
    PEPPOL_SIGNATURE;

    public static AuthenticationMethod from(AuthenticationMethodRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case TOKEN -> TOKEN;
            case TRUSTED_PROFILE -> TRUSTED_PROFILE;
            case INTERNAL_CERTIFICATE -> INTERNAL_CERTIFICATE;
            case QUALIFIED_SIGNATURE -> QUALIFIED_SIGNATURE;
            case QUALIFIED_SEAL -> QUALIFIED_SEAL;
            case PERSONAL_SIGNATURE -> PERSONAL_SIGNATURE;
            case PEPPOL_SIGNATURE -> PEPPOL_SIGNATURE;
        };
    }
}
