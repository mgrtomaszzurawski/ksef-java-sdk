/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code PermissionClient.queryEuEntities(...)}.
 */
public record EuEntityPermissionsQueryRequest(
        @Nullable String vatUeIdentifier,
        @Nullable String authorizedFingerprintIdentifier,
        List<EuEntityQueryPermissionType> permissionTypes) {

    public EuEntityPermissionsQueryRequest {
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
