/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code PermissionClient.grantIndirect(...)}.
 */
public record IndirectPermissionGrantRequest(
        PersonSubjectIdentifierType identifierType,
        String identifierValue,
        String description,
        String firstName,
        String lastName,
        @Nullable IndirectTargetIdentifierType targetType,
        @Nullable String targetValue,
        List<IndirectPermissionType> permissions) {

    public IndirectPermissionGrantRequest {
        Objects.requireNonNull(identifierType, "identifierType");
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
