/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionItemRaw;

import java.time.OffsetDateTime;

/**
 * An entity permission entry from query results.
 */
public record EntityPermission(
        String id,
        PermissionIdentifier contextIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate,
        Boolean canDelegate) {

    public static EntityPermission from(EntityPermissionItemRaw raw) {
        PermissionIdentifier ctxId = raw.getContextIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getContextIdentifier().getType() != null ? raw.getContextIdentifier().getType().getValue() : null,
                        raw.getContextIdentifier().getValue())
                : null;
        return new EntityPermission(
                raw.getId(), ctxId,
                raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null,
                raw.getDescription(), raw.getStartDate(), raw.getCanDelegate());
    }
}
