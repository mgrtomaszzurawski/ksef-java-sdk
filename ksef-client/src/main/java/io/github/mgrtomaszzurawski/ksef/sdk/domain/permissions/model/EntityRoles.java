/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityRolesResponseRaw;
import java.util.List;

/**
 * Result of querying entity roles.
 */
public record EntityRoles(List<EntityRole> roles, boolean hasMore) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static EntityRoles from(QueryEntityRolesResponseRaw raw) {
        List<EntityRole> mapped = raw.getRoles().stream().map(EntityRole::from).toList();
        return new EntityRoles(mapped, raw.getHasMore());
    }
}
