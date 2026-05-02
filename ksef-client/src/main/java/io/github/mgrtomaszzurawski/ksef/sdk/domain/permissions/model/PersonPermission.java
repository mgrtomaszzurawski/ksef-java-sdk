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

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static PersonPermission from(PersonPermissionRaw raw) {
        var authzRaw = raw.getAuthorizedIdentifier();
        PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var ctxRaw = raw.getContextIdentifier();
        PermissionIdentifier ctxId = ctxRaw != null
                ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                : null;
        var targetRaw = raw.getTargetIdentifier();
        PermissionIdentifier targetId = targetRaw != null
                ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                : null;
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = raw.getPermissionScope().getValue();
        String state = raw.getPermissionState().getValue();
        return new PersonPermission(raw.getId(), authzId, ctxId, targetId, authorId,
                scope, raw.getDescription(), state, raw.getStartDate(), raw.getCanDelegate());
    }
}
