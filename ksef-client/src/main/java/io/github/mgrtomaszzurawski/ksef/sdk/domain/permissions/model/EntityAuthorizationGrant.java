/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * An entity authorization grant from query results.
 *
 * @since 0.1.0
 */
public record EntityAuthorizationGrant(
        String id,
        @Nullable PermissionIdentifier authorIdentifier,
        PermissionIdentifier authorizedEntityIdentifier,
        PermissionIdentifier authorizingEntityIdentifier,
        String authorizationScope,
        String description,
        OffsetDateTime startDate) {

}
