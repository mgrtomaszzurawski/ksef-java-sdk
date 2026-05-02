/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionRaw;
import java.time.OffsetDateTime;

/**
 * A personal permission entry from query results.
 *
 * @param id permission ID
 * @param contextIdentifier context (NIP) identifier
 * @param authorizedIdentifier authorized subject identifier
 * @param targetIdentifier target subject identifier
 * @param permissionScope permission scope (e.g. InvoiceRead, CredentialsManage)
 * @param description permission description
 * @param subjectPersonDetails person details (if applicable)
 * @param subjectEntityDetails entity details (if applicable)
 * @param permissionState Active or Inactive
 * @param startDate when the permission was granted
 * @param canDelegate whether the permission can be delegated
 */
public record PersonalPermission(
        String id,
        PermissionIdentifier contextIdentifier,
        PermissionIdentifier authorizedIdentifier,
        PermissionIdentifier targetIdentifier,
        String permissionScope,
        String description,
        PermissionSubjectDetails subjectPersonDetails,
        PermissionSubjectDetails subjectEntityDetails,
        String permissionState,
        OffsetDateTime startDate,
        Boolean canDelegate) {

    public static PersonalPermission from(PersonalPermissionRaw raw) {
        var ctxRaw = raw.getContextIdentifier();
        PermissionIdentifier ctxId = ctxRaw != null
                ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                : null;
        var authzRaw = raw.getAuthorizedIdentifier();
        PermissionIdentifier authzId = authzRaw != null
                ? new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue())
                : null;
        var targetRaw = raw.getTargetIdentifier();
        PermissionIdentifier targetId = targetRaw != null
                ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                : null;
        PermissionSubjectDetails personDetails = raw.getSubjectPersonDetails() != null
                ? new PermissionSubjectDetails(
                        raw.getSubjectPersonDetails().getFirstName(),
                        raw.getSubjectPersonDetails().getLastName(), null)
                : null;
        PermissionSubjectDetails entityDetails = raw.getSubjectEntityDetails() != null
                ? new PermissionSubjectDetails(null, null,
                        raw.getSubjectEntityDetails().getFullName())
                : null;
        String scope = raw.getPermissionScope().getValue();
        String state = raw.getPermissionState().getValue();
        return new PersonalPermission(raw.getId(), ctxId, authzId, targetId, scope,
                raw.getDescription(), personDetails, entityDetails, state,
                raw.getStartDate(), raw.getCanDelegate());
    }
}
