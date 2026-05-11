/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request for {@code Permissions.grantPerson(...)}.
 *
 * @since 1.0.0
 */
public record PersonPermissionGrantRequest(
        PersonSubjectIdentifierType identifierType,
        String identifierValue,
        String description,
        String firstName,
        String lastName,
        List<PersonPermissionType> permissions) {

    public PersonPermissionGrantRequest {
        Objects.requireNonNull(identifierType, "identifierType");
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
