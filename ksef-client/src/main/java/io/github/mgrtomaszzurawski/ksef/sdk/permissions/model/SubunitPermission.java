/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;
import io.github.mgrtomaszzurawski.ksef.sdk.permissions.model.PermissionIdentifier;

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
        PermissionIdentifier authzId = raw.getAuthorizedIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorizedIdentifier().getType() != null ? raw.getAuthorizedIdentifier().getType().getValue() : null,
                        raw.getAuthorizedIdentifier().getValue())
                : null;
        PermissionIdentifier subunitId = raw.getSubunitIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getSubunitIdentifier().getType() != null ? raw.getSubunitIdentifier().getType().getValue() : null,
                        raw.getSubunitIdentifier().getValue())
                : null;
        PermissionIdentifier authorId = raw.getAuthorIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorIdentifier().getType() != null ? raw.getAuthorIdentifier().getType().getValue() : null,
                        raw.getAuthorIdentifier().getValue())
                : null;
        return new SubunitPermission(
                raw.getId(), authzId, subunitId, authorId,
                raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null,
                raw.getDescription(), raw.getSubunitName(), raw.getStartDate());
    }
}
