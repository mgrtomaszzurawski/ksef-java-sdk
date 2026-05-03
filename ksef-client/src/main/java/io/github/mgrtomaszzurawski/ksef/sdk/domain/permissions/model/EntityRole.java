/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * An entity role entry from query results.
 */
public record EntityRole(
        PermissionIdentifier parentEntityIdentifier,
        String role,
        String description,
        OffsetDateTime startDate) {

}
