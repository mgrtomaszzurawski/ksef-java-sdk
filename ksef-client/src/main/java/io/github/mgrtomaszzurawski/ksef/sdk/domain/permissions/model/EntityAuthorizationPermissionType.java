/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Authorization permission types granted between entities.
 *
 * @since 0.1.0
 */
public enum EntityAuthorizationPermissionType {
    SELF_INVOICING,
    RR_INVOICING,
    TAX_REPRESENTATIVE,
    PEF_INVOICING
}
