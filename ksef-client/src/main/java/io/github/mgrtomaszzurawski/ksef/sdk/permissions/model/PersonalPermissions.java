/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonalPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying personal permissions.
 *
 * @param permissions list of personal permissions
 * @param hasMore whether more results are available
 */
public record PersonalPermissions(List<PersonalPermission> permissions, boolean hasMore) {

    public static PersonalPermissions from(QueryPersonalPermissionsResponseRaw raw) {
        List<PersonalPermission> mapped = raw.getPermissions() != null
                ? raw.getPermissions().stream().map(PersonalPermission::from).toList()
                : List.of();
        return new PersonalPermissions(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
