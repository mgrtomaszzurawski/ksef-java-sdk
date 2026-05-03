/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code PermissionClient.grantSubunit(...)}.
 */
public record SubunitPermissionGrantRequest(
        PersonSubjectIdentifierType identifierType,
        String identifierValue,
        SubunitContextIdentifierType contextType,
        String contextValue,
        String description,
        String firstName,
        String lastName,
        @Nullable String subunitName) {

    public SubunitPermissionGrantRequest {
        Objects.requireNonNull(identifierType, "identifierType");
        Objects.requireNonNull(identifierValue, "identifierValue");
        Objects.requireNonNull(contextType, "contextType");
        Objects.requireNonNull(contextValue, "contextValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }
}
