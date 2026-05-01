/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;
import io.github.mgrtomaszzurawski.ksef.sdk.permissions.model.PermissionIdentifier;

import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionRaw;

import java.time.OffsetDateTime;

/**
 * A person permission entry from query results.
 *
 * @param id permission ID
 * @param authorizedIdentifier authorized subject identifier
 * @param contextIdentifier context (NIP) identifier
 * @param targetIdentifier target subject identifier
 * @param authorIdentifier author (grantor) identifier
 * @param permissionScope permission scope
 * @param description permission description
 * @param permissionState Active or Inactive
 * @param startDate when the permission was granted
 * @param canDelegate whether the permission can be delegated
 */
public record PersonPermission(
        String id,
        PermissionIdentifier authorizedIdentifier,
        PermissionIdentifier contextIdentifier,
        PermissionIdentifier targetIdentifier,
        PermissionIdentifier authorIdentifier,
        String permissionScope,
        String description,
        String permissionState,
        OffsetDateTime startDate,
        Boolean canDelegate) {

    public static PersonPermission from(PersonPermissionRaw raw) {
        PermissionIdentifier authzId = raw.getAuthorizedIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorizedIdentifier().getType() != null ? raw.getAuthorizedIdentifier().getType().getValue() : null,
                        raw.getAuthorizedIdentifier().getValue())
                : null;
        PermissionIdentifier ctxId = raw.getContextIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getContextIdentifier().getType() != null ? raw.getContextIdentifier().getType().getValue() : null,
                        raw.getContextIdentifier().getValue())
                : null;
        PermissionIdentifier targetId = raw.getTargetIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getTargetIdentifier().getType() != null ? raw.getTargetIdentifier().getType().getValue() : null,
                        raw.getTargetIdentifier().getValue())
                : null;
        PermissionIdentifier authorId = raw.getAuthorIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorIdentifier().getType() != null ? raw.getAuthorIdentifier().getType().getValue() : null,
                        raw.getAuthorIdentifier().getValue())
                : null;
        return new PersonPermission(
                raw.getId(), authzId, ctxId, targetId, authorId,
                raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null,
                raw.getDescription(),
                raw.getPermissionState() != null ? raw.getPermissionState().getValue() : null,
                raw.getStartDate(), raw.getCanDelegate());
    }
}
