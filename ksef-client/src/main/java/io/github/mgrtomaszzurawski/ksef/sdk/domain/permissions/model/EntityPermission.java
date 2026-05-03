/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * An entity permission entry from query results.
 */
public record EntityPermission(
        String id,
        PermissionIdentifier contextIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate,
        Boolean canDelegate) {

}
