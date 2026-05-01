/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens.model;

import io.github.mgrtomaszzurawski.ksef.client.model.TokenPermissionTypeRaw;

/**
 * Permission type that can be granted to a KSeF API token.
 */
public enum TokenPermissionType {

    INVOICE_READ,
    INVOICE_WRITE,
    CREDENTIALS_READ,
    CREDENTIALS_MANAGE,
    SUBUNIT_MANAGE,
    ENFORCEMENT_OPERATIONS,
    INTROSPECTION;

    public static TokenPermissionType from(TokenPermissionTypeRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case INVOICE_READ -> INVOICE_READ;
            case INVOICE_WRITE -> INVOICE_WRITE;
            case CREDENTIALS_READ -> CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> CREDENTIALS_MANAGE;
            case SUBUNIT_MANAGE -> SUBUNIT_MANAGE;
            case ENFORCEMENT_OPERATIONS -> ENFORCEMENT_OPERATIONS;
            case INTROSPECTION -> INTROSPECTION;
        };
    }
}
