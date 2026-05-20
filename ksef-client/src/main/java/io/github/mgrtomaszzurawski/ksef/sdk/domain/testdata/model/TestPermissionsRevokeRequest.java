/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * SDK request for {@code TestDataAdmin.revokePermissions(...)}.
 *
 * @since 0.1.0
 */
public record TestPermissionsRevokeRequest(
        String contextNip,
        TestDataAuthorizedIdentifierType authorizedType,
        String authorizedValue) {

    public TestPermissionsRevokeRequest {
        Objects.requireNonNull(contextNip, "contextNip");
        Objects.requireNonNull(authorizedType, "authorizedType");
        Objects.requireNonNull(authorizedValue, "authorizedValue");
    }
}
