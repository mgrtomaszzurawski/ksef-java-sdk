/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request for {@code Permissions.grantEntity(...)}.
 *
 * @since 1.0.0
 */
public record EntityPermissionGrantRequest(
        String identifierValue,
        String description,
        String fullName,
        List<EntityPermissionEntry> permissions) {

    public EntityPermissionGrantRequest {
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
