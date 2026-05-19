/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.Objects;

/**
 * SDK request for {@code Permissions.grant(...)}.
 *
 * @since 1.0.0
 */
public record EntityAuthorizationPermissionGrantRequest(
        EntityAuthorizationIdentifierType identifierType,
        String identifierValue,
        String description,
        String fullName,
        EntityAuthorizationPermissionType permission) implements PermissionGrantRequest {

    public EntityAuthorizationPermissionGrantRequest {
        Objects.requireNonNull(identifierType, "identifierType");
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(permission, "permission");
    }
}
