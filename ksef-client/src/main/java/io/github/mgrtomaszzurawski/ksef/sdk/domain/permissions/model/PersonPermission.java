/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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
        var authzRaw = raw.getAuthorizedIdentifier();
        PermissionIdentifier authzId = null;
        if (authzRaw != null) {
            String type = authzRaw.getType() != null ? authzRaw.getType().getValue() : null;
            authzId = new PermissionIdentifier(type, authzRaw.getValue());
        }
        var ctxRaw = raw.getContextIdentifier();
        PermissionIdentifier ctxId = null;
        if (ctxRaw != null) {
            String type = ctxRaw.getType() != null ? ctxRaw.getType().getValue() : null;
            ctxId = new PermissionIdentifier(type, ctxRaw.getValue());
        }
        var targetRaw = raw.getTargetIdentifier();
        PermissionIdentifier targetId = null;
        if (targetRaw != null) {
            String type = targetRaw.getType() != null ? targetRaw.getType().getValue() : null;
            targetId = new PermissionIdentifier(type, targetRaw.getValue());
        }
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = null;
        if (authorRaw != null) {
            String type = authorRaw.getType() != null ? authorRaw.getType().getValue() : null;
            authorId = new PermissionIdentifier(type, authorRaw.getValue());
        }
        String scope = raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null;
        String state = raw.getPermissionState() != null ? raw.getPermissionState().getValue() : null;
        return new PersonPermission(raw.getId(), authzId, ctxId, targetId, authorId,
                scope, raw.getDescription(), state, raw.getStartDate(), raw.getCanDelegate());
    }
}
