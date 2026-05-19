/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request for {@code Permissions.grant(...)}.
 *
 * @since 1.0.0
 */
public record EuEntityPermissionGrantRequest(
        String fingerprintValue,
        String description,
        String subjectFullName,
        String subjectAddress,
        List<EuEntityPermissionType> permissions) implements PermissionGrantRequest {

    public EuEntityPermissionGrantRequest {
        Objects.requireNonNull(fingerprintValue, "fingerprintValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(subjectFullName, "subjectFullName");
        Objects.requireNonNull(subjectAddress, "subjectAddress");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
