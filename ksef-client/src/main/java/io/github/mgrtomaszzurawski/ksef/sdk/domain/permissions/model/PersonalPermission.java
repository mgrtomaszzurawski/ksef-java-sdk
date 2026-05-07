/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

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
 *
 * @since 1.0.0
 */
public record PersonalPermission(
        String id,
        @Nullable PermissionIdentifier contextIdentifier,
        @Nullable PermissionIdentifier authorizedIdentifier,
        @Nullable PermissionIdentifier targetIdentifier,
        String permissionScope,
        String description,
        @Nullable PermissionSubjectDetails subjectPersonDetails,
        @Nullable PermissionSubjectDetails subjectEntityDetails,
        String permissionState,
        OffsetDateTime startDate,
        @Nullable Boolean canDelegate) {

}
