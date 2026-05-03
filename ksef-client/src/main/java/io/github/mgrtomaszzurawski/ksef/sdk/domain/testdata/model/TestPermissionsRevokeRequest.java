/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * SDK request for {@code TestDataClient.revokePermissions(...)}.
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
