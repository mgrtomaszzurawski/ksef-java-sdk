/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;
import io.github.mgrtomaszzurawski.ksef.sdk.permissions.model.PermissionIdentifier;

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
        PermissionIdentifier parentId = raw.getParentEntityIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getParentEntityIdentifier().getType() != null ? raw.getParentEntityIdentifier().getType().getValue() : null,
                        raw.getParentEntityIdentifier().getValue())
                : null;
        return new EntityRole(
                parentId,
                raw.getRole() != null ? raw.getRole().getValue() : null,
                raw.getDescription(), raw.getStartDate());
    }
}
