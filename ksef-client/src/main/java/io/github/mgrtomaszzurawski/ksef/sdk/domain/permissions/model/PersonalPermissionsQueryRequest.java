/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code PermissionClient.queryPersonal(...)}.
 */
public record PersonalPermissionsQueryRequest(
        @Nullable PersonalContextIdentifierType contextType,
        @Nullable String contextValue,
        @Nullable PersonalTargetIdentifierType targetType,
        @Nullable String targetValue,
        List<PersonalPermissionType> permissionTypes,
        @Nullable PermissionState permissionState) {

    public PersonalPermissionsQueryRequest {
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
