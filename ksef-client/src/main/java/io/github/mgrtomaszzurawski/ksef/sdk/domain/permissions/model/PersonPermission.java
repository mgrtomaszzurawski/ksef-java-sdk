/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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

}
