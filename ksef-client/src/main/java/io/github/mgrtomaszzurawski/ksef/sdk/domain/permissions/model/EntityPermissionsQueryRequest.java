/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder.ContextIdentifierType;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code PermissionClient.queryEntities(...)} and
 * {@code PermissionClient.streamEntities(...)}. Captures the optional
 * context identifier filter and paging parameters for
 * {@code POST /permissions/query/entities/grants}.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder#build()}.
 *
 * @since 1.0.0
 */
public record EntityPermissionsQueryRequest(
        @Nullable ContextIdentifierType contextIdentifierType,
        @Nullable String contextIdentifierValue,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {
}
