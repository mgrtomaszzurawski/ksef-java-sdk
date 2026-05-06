/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request for {@code TestDataClient.grantPermissions(...)}.
 *
 * @since 1.0.0
 */
public record TestPermissionsGrantRequest(
        String contextNip,
        TestDataAuthorizedIdentifierType authorizedType,
        String authorizedValue,
        List<TestDataPermission> permissions) {

    public TestPermissionsGrantRequest {
        Objects.requireNonNull(contextNip, "contextNip");
        Objects.requireNonNull(authorizedType, "authorizedType");
        Objects.requireNonNull(authorizedValue, "authorizedValue");
        Objects.requireNonNull(permissions, "permissions");
        permissions = List.copyOf(permissions);
    }
}
