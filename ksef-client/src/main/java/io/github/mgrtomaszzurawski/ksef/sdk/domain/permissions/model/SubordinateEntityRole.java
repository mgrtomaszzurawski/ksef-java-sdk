/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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
        var subRaw = raw.getSubordinateEntityIdentifier();
        PermissionIdentifier subId = new PermissionIdentifier(subRaw.getType().getValue(), subRaw.getValue());
        String role = raw.getRole().getValue();
        return new SubordinateEntityRole(subId, role, raw.getDescription(), raw.getStartDate());
    }
}
