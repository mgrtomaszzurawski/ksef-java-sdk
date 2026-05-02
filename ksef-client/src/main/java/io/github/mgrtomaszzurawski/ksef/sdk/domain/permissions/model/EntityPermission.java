/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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
        var ctxRaw = raw.getContextIdentifier();
        PermissionIdentifier ctxId = null;
        if (ctxRaw != null) {
            String type = ctxRaw.getType() != null ? ctxRaw.getType().getValue() : null;
            ctxId = new PermissionIdentifier(type, ctxRaw.getValue());
        }
        String scope = raw.getPermissionScope().getValue();
        return new EntityPermission(raw.getId(), ctxId, scope, raw.getDescription(),
                raw.getStartDate(), raw.getCanDelegate());
    }
}
