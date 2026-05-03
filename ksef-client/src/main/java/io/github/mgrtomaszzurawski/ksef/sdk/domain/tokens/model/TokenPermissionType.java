/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

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

}
