/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code Permissions.queryPersonal(...)}.
 *
 * @since 0.1.0
 */
public record PersonalPermissionsQueryRequest(
        @Nullable PersonalContextIdentifierType contextType,
        @Nullable String contextValue,
        @Nullable PersonalTargetIdentifierType targetType,
        @Nullable String targetValue,
        List<PersonalPermissionType> permissionTypes,
        @Nullable PermissionState permissionState,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public PersonalPermissionsQueryRequest {
        PermissionQueryPaging.validate(pageOffset, pageSize);
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
