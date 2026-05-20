/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder.SubunitIdentifierType;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code Permissions.querySubunits(...)} and
 * {@code Permissions.streamSubunits(...)}. Captures the optional
 * subunit identifier filter and paging parameters for
 * {@code POST /permissions/query/subunits/grants}.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder#build()}.
 *
 * @since 0.1.0
 */
public record SubunitPermissionsQueryRequest(
        @Nullable SubunitIdentifierType subunitIdentifierType,
        @Nullable String subunitIdentifierValue,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    public SubunitPermissionsQueryRequest {
        PermissionQueryPaging.validate(pageOffset, pageSize);
    }
}
