/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionRaw;
import java.time.OffsetDateTime;

/**
 * A subunit permission entry from query results.
 */
public record SubunitPermission(
        String id,
        PermissionIdentifier authorizedIdentifier,
        PermissionIdentifier subunitIdentifier,
        PermissionIdentifier authorIdentifier,
        String permissionScope,
        String description,
        String subunitName,
        OffsetDateTime startDate) {

    public static SubunitPermission from(SubunitPermissionRaw raw) {
        var authzRaw = raw.getAuthorizedIdentifier();
        PermissionIdentifier authzId = null;
        if (authzRaw != null) {
            String type = authzRaw.getType() != null ? authzRaw.getType().getValue() : null;
            authzId = new PermissionIdentifier(type, authzRaw.getValue());
        }
        var subunitRaw = raw.getSubunitIdentifier();
        PermissionIdentifier subunitId = null;
        if (subunitRaw != null) {
            String type = subunitRaw.getType() != null ? subunitRaw.getType().getValue() : null;
            subunitId = new PermissionIdentifier(type, subunitRaw.getValue());
        }
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = null;
        if (authorRaw != null) {
            String type = authorRaw.getType() != null ? authorRaw.getType().getValue() : null;
            authorId = new PermissionIdentifier(type, authorRaw.getValue());
        }
        String scope = raw.getPermissionScope().getValue();
        return new SubunitPermission(raw.getId(), authzId, subunitId, authorId,
                scope, raw.getDescription(), raw.getSubunitName(), raw.getStartDate());
    }
}
