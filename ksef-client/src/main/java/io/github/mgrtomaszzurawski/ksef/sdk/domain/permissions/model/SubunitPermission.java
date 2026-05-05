/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * A subunit permission entry from query results.
 *
 * @since 1.0.0
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

}
