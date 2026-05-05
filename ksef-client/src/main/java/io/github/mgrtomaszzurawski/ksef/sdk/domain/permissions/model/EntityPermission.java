/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * An entity permission entry from query results.
 *
 * @since 1.0.0
 */
public record EntityPermission(
        String id,
        PermissionIdentifier contextIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate,
        Boolean canDelegate) {

}
