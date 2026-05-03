/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * An EU entity permission entry from query results.
 */
public record EuEntityPermission(
        String id,
        PermissionIdentifier authorIdentifier,
        String vatUeIdentifier,
        String euEntityName,
        String authorizedFingerprintIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate) {

}
