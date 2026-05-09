/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRolesQueryRequest;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for the {@code POST /permissions/query/subordinate-entities/roles}
 * request body + paging query parameters. The spec only defines
 * {@code Nip} as a valid identifier type for this endpoint.
 *
 * @since 1.0.0
 */
public final class SubordinateEntityRolesQueryBuilder {

    private @Nullable String subordinateEntityNip;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private SubordinateEntityRolesQueryBuilder() { }

    public static SubordinateEntityRolesQueryBuilder create() {
        return new SubordinateEntityRolesQueryBuilder();
    }

    public SubordinateEntityRolesQueryBuilder subordinateEntityNip(String nip) {
        this.subordinateEntityNip = Objects.requireNonNull(nip, "nip");
        return this;
    }

    public SubordinateEntityRolesQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    public SubordinateEntityRolesQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public @Nullable String subordinateEntityNipValue() {
        return subordinateEntityNip;
    }

    public @Nullable Integer pageOffsetValue() {
        return pageOffset;
    }

    public @Nullable Integer pageSizeValue() {
        return pageSize;
    }

    /** Build the immutable {@link SubordinateEntityRolesQueryRequest} captured by this builder. */
    public SubordinateEntityRolesQueryRequest build() {
        return new SubordinateEntityRolesQueryRequest(subordinateEntityNip, pageOffset, pageSize);
    }
}
