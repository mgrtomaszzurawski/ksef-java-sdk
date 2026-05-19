/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code Permissions.queryPersons(...)}.
 *
 * @since 1.0.0
 */
public record PersonPermissionsQueryRequest(
        PersonPermissionsQueryType queryType,
        @Nullable PersonAuthorIdentifierType authorType,
        @Nullable String authorValue,
        @Nullable PersonSubjectIdentifierType authorizedType,
        @Nullable String authorizedValue,
        @Nullable PersonalContextIdentifierType contextType,
        @Nullable String contextValue,
        @Nullable PersonalTargetIdentifierType targetType,
        @Nullable String targetValue,
        List<PersonPermissionType> permissionTypes,
        @Nullable PermissionState permissionState,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public PersonPermissionsQueryRequest {
        Objects.requireNonNull(queryType, "queryType");
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
        PermissionQueryPaging.validate(pageOffset, pageSize);
    }
}
