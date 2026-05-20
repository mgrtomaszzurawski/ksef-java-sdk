/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code Permissions.queryEuEntities(...)}.
 *
 * @since 0.1.0
 */
public record EuEntityPermissionsQueryRequest(
        @Nullable String vatUeIdentifier,
        @Nullable String authorizedFingerprintIdentifier,
        List<EuEntityQueryPermissionType> permissionTypes,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public EuEntityPermissionsQueryRequest {
        PermissionQueryPaging.validate(pageOffset, pageSize);
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
