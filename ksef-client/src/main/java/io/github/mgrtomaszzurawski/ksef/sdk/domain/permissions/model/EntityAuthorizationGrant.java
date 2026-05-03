/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.OffsetDateTime;

/**
 * An entity authorization grant from query results.
 */
public record EntityAuthorizationGrant(
        String id,
        PermissionIdentifier authorIdentifier,
        PermissionIdentifier authorizedEntityIdentifier,
        PermissionIdentifier authorizingEntityIdentifier,
        String authorizationScope,
        String description,
        OffsetDateTime startDate) {

}
