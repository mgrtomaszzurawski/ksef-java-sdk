/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationMethodCategoryRaw;

/**
 * Category of authentication method used in KSeF.
 *
 * @since 1.0.0
 */
public enum AuthenticationMethodCategory {

    XADES_SIGNATURE,
    NATIONAL_NODE,
    TOKEN,
    OTHER;

    public static AuthenticationMethodCategory from(AuthenticationMethodCategoryRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case XADES_SIGNATURE -> XADES_SIGNATURE;
            case NATIONAL_NODE -> NATIONAL_NODE;
            case TOKEN -> TOKEN;
            case OTHER -> OTHER;
        };
    }
}
