/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * An EU entity permission entry from query results.
 *
 * @since 0.1.0
 */
public record EuEntityPermission(
        String id,
        PermissionIdentifier authorIdentifier,
        String vatUeIdentifier,
        String euEntityName,
        String authorizedFingerprintIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate,
        @Nullable PermissionSubjectDetails subjectPersonDetails,
        @Nullable PermissionSubjectDetails subjectEntityDetails,
        @Nullable PermissionEuEntityDetails euEntityDetails) {

}
