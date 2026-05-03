/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request for {@code PermissionClient.grantEuEntity(...)}.
 */
public record EuEntityPermissionGrantRequest(
        String fingerprintValue,
        String description,
        String subjectFullName,
        String subjectAddress,
        List<EuEntityPermissionType> permissions) {

    public EuEntityPermissionGrantRequest {
        Objects.requireNonNull(fingerprintValue, "fingerprintValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(subjectFullName, "subjectFullName");
        Objects.requireNonNull(subjectAddress, "subjectAddress");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
