/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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

}
