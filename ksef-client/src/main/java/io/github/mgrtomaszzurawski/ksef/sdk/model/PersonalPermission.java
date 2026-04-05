/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

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
        PermissionIdentifier ctxId = raw.getContextIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getContextIdentifier().getType() != null ? raw.getContextIdentifier().getType().getValue() : null,
                        raw.getContextIdentifier().getValue())
                : null;
        PermissionIdentifier authzId = raw.getAuthorizedIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorizedIdentifier().getType() != null ? raw.getAuthorizedIdentifier().getType().getValue() : null,
                        raw.getAuthorizedIdentifier().getValue())
                : null;
        PermissionIdentifier targetId = raw.getTargetIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getTargetIdentifier().getType() != null ? raw.getTargetIdentifier().getType().getValue() : null,
                        raw.getTargetIdentifier().getValue())
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
        return new PersonalPermission(
                raw.getId(), ctxId, authzId, targetId,
                raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null,
                raw.getDescription(), personDetails, entityDetails,
                raw.getPermissionState() != null ? raw.getPermissionState().getValue() : null,
                raw.getStartDate(), raw.getCanDelegate());
    }
}
