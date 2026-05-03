/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.Objects;

/**
 * SDK request for {@code PermissionClient.grantAuthorization(...)}.
 */
public record EntityAuthorizationPermissionGrantRequest(
        EntityAuthorizationIdentifierType identifierType,
        String identifierValue,
        String description,
        String fullName,
        EntityAuthorizationPermissionType permission) {

    public EntityAuthorizationPermissionGrantRequest {
        Objects.requireNonNull(identifierType, "identifierType");
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(permission, "permission");
    }
}
