/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * Permission entry for test-data permission grants.
 *
 * @since 0.1.0
 */
public record TestDataPermission(TestDataPermissionType permissionType, String description) {

    public TestDataPermission {
        Objects.requireNonNull(permissionType, "permissionType");
        Objects.requireNonNull(description, "description");
    }
}
