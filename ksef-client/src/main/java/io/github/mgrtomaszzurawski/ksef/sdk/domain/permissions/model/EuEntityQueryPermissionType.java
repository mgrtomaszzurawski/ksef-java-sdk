/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Permission types accepted by EU entity permission queries.
 *
 * @since 0.1.0
 */
public enum EuEntityQueryPermissionType {
    VAT_UE_MANAGE,
    INVOICE_READ,
    INVOICE_WRITE,
    INTROSPECTION
}
