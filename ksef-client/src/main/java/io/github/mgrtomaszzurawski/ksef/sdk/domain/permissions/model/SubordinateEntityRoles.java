/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubordinateEntityRolesResponseRaw;
import java.util.List;

/**
 * Result of querying subordinate entity roles.
 */
public record SubordinateEntityRoles(List<SubordinateEntityRole> roles, boolean hasMore) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static SubordinateEntityRoles from(QuerySubordinateEntityRolesResponseRaw raw) {
        List<SubordinateEntityRole> mapped = raw.getRoles().stream().map(SubordinateEntityRole::from).toList();
        return new SubordinateEntityRoles(mapped, raw.getHasMore());
    }
}
