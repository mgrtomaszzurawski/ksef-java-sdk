/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Authorization permission types granted between entities.
 */
public enum EntityAuthorizationPermissionType {
    SELF_INVOICING,
    RR_INVOICING,
    TAX_REPRESENTATIVE,
    PEF_INVOICING
}
