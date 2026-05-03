/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * Permission entry for test-data permission grants.
 */
public record TestDataPermission(TestDataPermissionType permissionType, String description) {

    public TestDataPermission {
        Objects.requireNonNull(permissionType, "permissionType");
        Objects.requireNonNull(description, "description");
    }
}
