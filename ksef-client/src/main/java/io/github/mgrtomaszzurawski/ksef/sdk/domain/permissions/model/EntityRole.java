/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityRoleRaw;
import java.time.OffsetDateTime;

/**
 * An entity role entry from query results.
 */
public record EntityRole(
        PermissionIdentifier parentEntityIdentifier,
        String role,
        String description,
        OffsetDateTime startDate) {

    public static EntityRole from(EntityRoleRaw raw) {
        var parentRaw = raw.getParentEntityIdentifier();
        PermissionIdentifier parentId = null;
        if (parentRaw != null) {
            String type = parentRaw.getType() != null ? parentRaw.getType().getValue() : null;
            parentId = new PermissionIdentifier(type, parentRaw.getValue());
        }
        String role = raw.getRole() != null ? raw.getRole().getValue() : null;
        return new EntityRole(parentId, role, raw.getDescription(), raw.getStartDate());
    }
}
