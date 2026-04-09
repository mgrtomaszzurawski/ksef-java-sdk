/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRoleRaw;

import java.time.OffsetDateTime;

/**
 * A subordinate entity role entry from query results.
 */
public record SubordinateEntityRole(
        PermissionIdentifier subordinateEntityIdentifier,
        String role,
        String description,
        OffsetDateTime startDate) {

    public static SubordinateEntityRole from(SubordinateEntityRoleRaw raw) {
        PermissionIdentifier subId = raw.getSubordinateEntityIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getSubordinateEntityIdentifier().getType() != null ? raw.getSubordinateEntityIdentifier().getType().getValue() : null,
                        raw.getSubordinateEntityIdentifier().getValue())
                : null;
        return new SubordinateEntityRole(
                subId,
                raw.getRole() != null ? raw.getRole().getValue() : null,
                raw.getDescription(), raw.getStartDate());
    }
}
