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

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static SubunitPermission from(SubunitPermissionRaw raw) {
        var authzRaw = raw.getAuthorizedIdentifier();
        PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var subunitRaw = raw.getSubunitIdentifier();
        PermissionIdentifier subunitId = new PermissionIdentifier(subunitRaw.getType().getValue(), subunitRaw.getValue());
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = raw.getPermissionScope().getValue();
        return new SubunitPermission(raw.getId(), authzId, subunitId, authorId,
                scope, raw.getDescription(), raw.getSubunitName(), raw.getStartDate());
    }
}
