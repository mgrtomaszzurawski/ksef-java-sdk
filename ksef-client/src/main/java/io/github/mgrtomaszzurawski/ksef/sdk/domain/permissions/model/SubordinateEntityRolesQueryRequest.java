/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code PermissionClient.querySubordinateRoles(...)} and
 * {@code PermissionClient.streamSubordinateRoles(...)}. Captures the
 * subordinate entity NIP filter and paging parameters for
 * {@code POST /permissions/query/subordinate-entities/roles}. The spec
 * only defines NIP as the supported identifier type for this endpoint.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder#build()}.
 *
 * @since 1.0.0
 */
public record SubordinateEntityRolesQueryRequest(
        @Nullable String subordinateEntityNip,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {
}
