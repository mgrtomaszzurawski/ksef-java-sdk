/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionsQueryRequest;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for the {@code POST /permissions/query/entities} request body
 * + paging query parameters (per spec
 * {@code ksef-docs/uprawnienia.md}). All fields optional.
 *
 * @since 1.0.0
 */
public final class EntityPermissionsQueryBuilder {

    /** Identifier type qualifier per the {@code contextIdentifier} schema. */
    public enum ContextIdentifierType {
        NIP,
        INTERNAL_ID;

        String wireValue() {
            return this == NIP ? "Nip" : "InternalId";
        }
    }

    private @Nullable ContextIdentifierType contextIdentifierType;
    private @Nullable String contextIdentifierValue;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private EntityPermissionsQueryBuilder() { }

    public static EntityPermissionsQueryBuilder create() {
        return new EntityPermissionsQueryBuilder();
    }

    public EntityPermissionsQueryBuilder contextNip(String nip) {
        this.contextIdentifierType = ContextIdentifierType.NIP;
        this.contextIdentifierValue = Objects.requireNonNull(nip, "nip");
        return this;
    }

    public EntityPermissionsQueryBuilder contextInternalId(String internalId) {
        this.contextIdentifierType = ContextIdentifierType.INTERNAL_ID;
        this.contextIdentifierValue = Objects.requireNonNull(internalId, "internalId");
        return this;
    }

    /**
     * Zero-based page offset for {@code queryEntities}. Default (null) → 0.
     * Must be {@code >= 0}; validated at {@code build()} time. Ignored on
     * {@code streamEntities}.
     */
    public EntityPermissionsQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    /**
     * Page size for {@code queryEntities}. KSeF range {@code [10, 100]};
     * validated at {@code build()} time. Default (null) → 100. Ignored on
     * {@code streamEntities}.
     */
    public EntityPermissionsQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public @Nullable ContextIdentifierType contextIdentifierType() {
        return contextIdentifierType;
    }

    public @Nullable String contextIdentifierValue() {
        return contextIdentifierValue;
    }

    public @Nullable Integer pageOffsetValue() {
        return pageOffset;
    }

    public @Nullable Integer pageSizeValue() {
        return pageSize;
    }

    /** Build the immutable {@link EntityPermissionsQueryRequest} captured by this builder. */
    public EntityPermissionsQueryRequest build() {
        return new EntityPermissionsQueryRequest(
                contextIdentifierType, contextIdentifierValue, pageOffset, pageSize);
    }
}
