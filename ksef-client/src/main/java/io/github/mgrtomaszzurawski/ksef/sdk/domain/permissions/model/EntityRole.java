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

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static EntityRole from(EntityRoleRaw raw) {
        var parentRaw = raw.getParentEntityIdentifier();
        PermissionIdentifier parentId = parentRaw != null
                ? new PermissionIdentifier(parentRaw.getType().getValue(), parentRaw.getValue())
                : null;
        String role = raw.getRole().getValue();
        return new EntityRole(parentId, role, raw.getDescription(), raw.getStartDate());
    }
}
