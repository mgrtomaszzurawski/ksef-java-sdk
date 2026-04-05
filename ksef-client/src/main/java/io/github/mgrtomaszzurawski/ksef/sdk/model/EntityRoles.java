/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityRolesResponseRaw;

import java.util.List;

/**
 * Result of querying entity roles.
 */
public record EntityRoles(List<EntityRole> roles, boolean hasMore) {

    public static EntityRoles from(QueryEntityRolesResponseRaw raw) {
        List<EntityRole> mapped = raw.getRoles() != null
                ? raw.getRoles().stream().map(EntityRole::from).toList()
                : List.of();
        return new EntityRoles(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
