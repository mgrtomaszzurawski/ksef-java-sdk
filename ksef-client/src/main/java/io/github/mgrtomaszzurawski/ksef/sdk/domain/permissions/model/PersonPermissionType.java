/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Permission types that can be granted to a person.
 */
public enum PersonPermissionType {
    INVOICE_READ,
    INVOICE_WRITE,
    CREDENTIALS_READ,
    CREDENTIALS_MANAGE,
    SUBUNIT_MANAGE,
    ENFORCEMENT_OPERATIONS,
    INTROSPECTION
}
