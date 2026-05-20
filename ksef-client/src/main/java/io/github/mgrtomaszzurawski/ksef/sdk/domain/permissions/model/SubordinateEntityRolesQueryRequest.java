/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code Permissions.querySubordinateRoles(...)} and
 * {@code Permissions.streamSubordinateRoles(...)}. Captures the
 * subordinate entity NIP filter and paging parameters for
 * {@code POST /permissions/query/subordinate-entities/roles}. The spec
 * only defines NIP as the supported identifier type for this endpoint.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder#build()}.
 *
 * @since 0.1.0
 */
public record SubordinateEntityRolesQueryRequest(
        @Nullable String subordinateEntityNip,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public SubordinateEntityRolesQueryRequest {
        PermissionQueryPaging.validate(pageOffset, pageSize);
    }
}
