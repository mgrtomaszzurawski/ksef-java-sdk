/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying person permissions.
 *
 * @param permissions list of person permissions
 * @param hasMore whether more results are available
 */
public record PersonPermissions(List<PersonPermission> permissions, boolean hasMore) {

    public static PersonPermissions from(QueryPersonPermissionsResponseRaw raw) {
        List<PersonPermission> mapped = raw.getPermissions().stream().map(PersonPermission::from).toList();
        return new PersonPermissions(mapped, raw.getHasMore());
    }
}
