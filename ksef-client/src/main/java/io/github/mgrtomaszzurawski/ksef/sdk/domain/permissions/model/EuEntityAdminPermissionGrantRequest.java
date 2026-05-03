/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.Objects;

/**
 * SDK request for {@code PermissionClient.grantEuEntityAdmin(...)}.
 */
public record EuEntityAdminPermissionGrantRequest(
        String fingerprintValue,
        String contextValue,
        String description,
        String euEntityName,
        String subjectFullName,
        String subjectAddress,
        String euEntityFullName,
        String euEntityAddress) {

    public EuEntityAdminPermissionGrantRequest {
        Objects.requireNonNull(fingerprintValue, "fingerprintValue");
        Objects.requireNonNull(contextValue, "contextValue");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(euEntityName, "euEntityName");
        Objects.requireNonNull(subjectFullName, "subjectFullName");
        Objects.requireNonNull(subjectAddress, "subjectAddress");
        Objects.requireNonNull(euEntityFullName, "euEntityFullName");
        Objects.requireNonNull(euEntityAddress, "euEntityAddress");
    }
}
