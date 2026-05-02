/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryEuEntityPermissionsResponseRaw;
import java.util.List;

/**
 * Result of querying EU entity permissions.
 */
public record EuEntityPermissions(List<EuEntityPermission> permissions, boolean hasMore) {

    public static EuEntityPermissions from(QueryEuEntityPermissionsResponseRaw raw) {
        List<EuEntityPermission> mapped = raw.getPermissions().stream().map(EuEntityPermission::from).toList();
        return new EuEntityPermissions(mapped, raw.getHasMore());
    }
}
