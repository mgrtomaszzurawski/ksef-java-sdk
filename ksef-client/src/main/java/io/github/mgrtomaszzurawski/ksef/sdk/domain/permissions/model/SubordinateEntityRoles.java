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

    public static SubordinateEntityRoles from(QuerySubordinateEntityRolesResponseRaw raw) {
        List<SubordinateEntityRole> mapped = raw.getRoles() != null
                ? raw.getRoles().stream().map(SubordinateEntityRole::from).toList()
                : List.of();
        return new SubordinateEntityRoles(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
