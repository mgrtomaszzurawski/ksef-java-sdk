/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying entity permissions.
 */
public record EntityPermissions(List<EntityPermission> permissions, boolean hasMore) {

    public static EntityPermissions from(QueryEntityPermissionsResponseRaw raw) {
        List<EntityPermission> mapped = raw.getPermissions().stream().map(EntityPermission::from).toList();
        return new EntityPermissions(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
