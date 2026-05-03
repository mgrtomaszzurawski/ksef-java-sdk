/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityQueryPermissionType;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for EU entity permissions query requests. All fields are optional.
 */
public final class EuEntityPermissionsQueryBuilder {

    private String vatUeIdentifier;
    private String authorizedFingerprintIdentifier;
    private final List<EuEntityQueryPermissionType> permissionTypes = new ArrayList<>();

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

    public EuEntityPermissionsQueryBuilder toBuilder() {
        EuEntityPermissionsQueryBuilder copy = new EuEntityPermissionsQueryBuilder();
        copy.vatUeIdentifier = this.vatUeIdentifier;
        copy.authorizedFingerprintIdentifier = this.authorizedFingerprintIdentifier;
        copy.permissionTypes.addAll(this.permissionTypes);
        return copy;
    }

    public EuEntityPermissionsQueryRequest build() {
        return new EuEntityPermissionsQueryRequest(vatUeIdentifier, authorizedFingerprintIdentifier, permissionTypes);
    }
}
