/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubunitPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying subunit permissions.
 *
 * @param permissions list of subunit permissions
 * @param hasMore whether more results are available
 */
public record SubunitPermissions(List<SubunitPermission> permissions, boolean hasMore) {

    public static SubunitPermissions from(QuerySubunitPermissionsResponseRaw raw) {
        List<SubunitPermission> mapped = raw.getPermissions() != null
                ? raw.getPermissions().stream().map(SubunitPermission::from).toList()
                : List.of();
        return new SubunitPermissions(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
