/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRolesQueryRequest;
import org.jspecify.annotations.Nullable;

/**
 * Builder for the {@code GET /permissions/query/entities/roles}
 * paging query parameters. The endpoint takes no body — only
 * {@code pageOffset} and {@code pageSize}.
 *
 * @since 1.0.0
 */
public final class EntityRolesQueryBuilder {

    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private EntityRolesQueryBuilder() { }

    public static EntityRolesQueryBuilder create() {
        return new EntityRolesQueryBuilder();
    }

    public EntityRolesQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    public EntityRolesQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public @Nullable Integer pageOffsetValue() {
        return pageOffset;
    }

    public @Nullable Integer pageSizeValue() {
        return pageSize;
    }

    /** Build the immutable {@link EntityRolesQueryRequest} captured by this builder. */
    public EntityRolesQueryRequest build() {
        return new EntityRolesQueryRequest(pageOffset, pageSize);
    }
}
