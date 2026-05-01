/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityAuthorizationPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying entity authorization permissions.
 */
public record EntityAuthorizationPermissions(List<EntityAuthorizationGrant> authorizationGrants, boolean hasMore) {

    public static EntityAuthorizationPermissions from(QueryEntityAuthorizationPermissionsResponseRaw raw) {
        List<EntityAuthorizationGrant> mapped = raw.getAuthorizationGrants() != null
                ? raw.getAuthorizationGrants().stream().map(EntityAuthorizationGrant::from).toList()
                : List.of();
        return new EntityAuthorizationPermissions(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
