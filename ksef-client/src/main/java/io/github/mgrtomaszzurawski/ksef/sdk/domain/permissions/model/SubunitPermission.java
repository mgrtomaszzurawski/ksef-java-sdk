/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

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

}
