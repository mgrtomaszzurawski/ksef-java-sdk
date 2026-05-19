/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code Permissions.queryEntityRoles(...)}.
 * The endpoint takes no body — only paging query parameters
 * {@code pageOffset} and {@code pageSize}.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder#build()}.
 *
 * @since 1.0.0
 */
public record EntityRolesQueryRequest(
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public EntityRolesQueryRequest {
        PermissionQueryPaging.validate(pageOffset, pageSize);
    }
}
