/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionsQueryRequest;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for the {@code POST /permissions/query/subunits} request body
 * + paging query parameters (per spec
 * {@code ksef-docs/uprawnienia.md}). All fields optional.
 *
 * @since 1.0.0
 */
public final class SubunitPermissionsQueryBuilder {

    /** Identifier type qualifier per the {@code subunitIdentifier} schema. */
    public enum SubunitIdentifierType {
        NIP,
        INTERNAL_ID;

        String wireValue() {
            return this == NIP ? "Nip" : "InternalId";
        }
    }

    private @Nullable SubunitIdentifierType subunitIdentifierType;
    private @Nullable String subunitIdentifierValue;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private SubunitPermissionsQueryBuilder() { }

    public static SubunitPermissionsQueryBuilder create() {
        return new SubunitPermissionsQueryBuilder();
    }

    public SubunitPermissionsQueryBuilder subunitNip(String nip) {
        this.subunitIdentifierType = SubunitIdentifierType.NIP;
        this.subunitIdentifierValue = Objects.requireNonNull(nip, "nip");
        return this;
    }

    public SubunitPermissionsQueryBuilder subunitInternalId(String internalId) {
        this.subunitIdentifierType = SubunitIdentifierType.INTERNAL_ID;
        this.subunitIdentifierValue = Objects.requireNonNull(internalId, "internalId");
        return this;
    }

    /**
     * Zero-based page offset for {@code querySubunits}. Default (null) → 0.
     * Must be {@code >= 0}; validated at {@code build()} time. Ignored on
     * {@code streamSubunits}.
     */
    public SubunitPermissionsQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    /**
     * Page size for {@code querySubunits}. KSeF range {@code [10, 100]};
     * validated at {@code build()} time. Default (null) → 100. Ignored on
     * {@code streamSubunits}.
     */
    public SubunitPermissionsQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public @Nullable SubunitIdentifierType subunitIdentifierType() {
        return subunitIdentifierType;
    }

    public @Nullable String subunitIdentifierValue() {
        return subunitIdentifierValue;
    }

    public @Nullable Integer pageOffsetValue() {
        return pageOffset;
    }

    public @Nullable Integer pageSizeValue() {
        return pageSize;
    }

    /** Build the immutable {@link SubunitPermissionsQueryRequest} captured by this builder. */
    public SubunitPermissionsQueryRequest build() {
        return new SubunitPermissionsQueryRequest(
                subunitIdentifierType, subunitIdentifierValue, pageOffset, pageSize);
    }
}
