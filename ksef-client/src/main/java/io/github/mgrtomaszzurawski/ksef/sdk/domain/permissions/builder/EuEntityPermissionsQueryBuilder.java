/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityQueryPermissionType;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Builder for EU entity permissions query requests. All fields are optional.
 *
 * @since 0.1.0
 */
public final class EuEntityPermissionsQueryBuilder {

    private @Nullable String vatUeIdentifier;
    private @Nullable String authorizedFingerprintIdentifier;
    private final List<EuEntityQueryPermissionType> permissionTypes = new ArrayList<>();
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private EuEntityPermissionsQueryBuilder() { }

    public static EuEntityPermissionsQueryBuilder create() {
        return new EuEntityPermissionsQueryBuilder();
    }

    public EuEntityPermissionsQueryBuilder vatUeIdentifier(String vatUeIdentifier) {
        this.vatUeIdentifier = vatUeIdentifier;
        return this;
    }

    public EuEntityPermissionsQueryBuilder authorizedFingerprintIdentifier(String fingerprint) {
        this.authorizedFingerprintIdentifier = fingerprint;
        return this;
    }

    public EuEntityPermissionsQueryBuilder vatUeManage() {
        permissionTypes.add(EuEntityQueryPermissionType.VAT_UE_MANAGE);
        return this;
    }

    public EuEntityPermissionsQueryBuilder invoiceRead() {
        permissionTypes.add(EuEntityQueryPermissionType.INVOICE_READ);
        return this;
    }

    public EuEntityPermissionsQueryBuilder invoiceWrite() {
        permissionTypes.add(EuEntityQueryPermissionType.INVOICE_WRITE);
        return this;
    }

    public EuEntityPermissionsQueryBuilder introspection() {
        permissionTypes.add(EuEntityQueryPermissionType.INTROSPECTION);
        return this;
    }

    /**
     * Zero-based page offset for {@code queryEuEntities}. Default (null) → 0.
     * Must be {@code >= 0}; validated at {@code build()} time. Ignored on
     * {@code streamEuEntities}.
     */
    public EuEntityPermissionsQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    /**
     * Page size for {@code queryEuEntities}. KSeF range {@code [10, 100]};
     * validated at {@code build()} time. Default (null) → 100. Ignored on
     * {@code streamEuEntities}.
     */
    public EuEntityPermissionsQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public EuEntityPermissionsQueryBuilder toBuilder() {
        EuEntityPermissionsQueryBuilder copy = new EuEntityPermissionsQueryBuilder();
        copy.vatUeIdentifier = this.vatUeIdentifier;
        copy.authorizedFingerprintIdentifier = this.authorizedFingerprintIdentifier;
        copy.permissionTypes.addAll(this.permissionTypes);
        copy.pageOffset = this.pageOffset;
        copy.pageSize = this.pageSize;
        return copy;
    }

    public EuEntityPermissionsQueryRequest build() {
        return new EuEntityPermissionsQueryRequest(vatUeIdentifier, authorizedFingerprintIdentifier,
                permissionTypes, pageOffset, pageSize);
    }
}
