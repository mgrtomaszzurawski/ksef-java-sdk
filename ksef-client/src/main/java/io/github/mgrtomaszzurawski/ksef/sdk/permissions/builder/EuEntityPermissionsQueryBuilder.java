/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;
import io.github.mgrtomaszzurawski.ksef.sdk.permissions.PermissionClient;

import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for EU entity permissions query requests.
 * <p>
 * All fields are optional.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EuEntityPermissionsQueryBuilder.create()
 *     .vatUeIdentifier("PL1234567890")
 *     .invoiceRead();
 * }</pre>
 */
public final class EuEntityPermissionsQueryBuilder {

    private String vatUeIdentifier;
    private String authorizedFingerprintIdentifier;
    private final List<EuEntityPermissionsQueryPermissionTypeRaw> permissionTypes = new ArrayList<>();

    private EuEntityPermissionsQueryBuilder() {
    }

    /**
     * Create a new query builder with all optional fields.
     */
    public static EuEntityPermissionsQueryBuilder create() {
        return new EuEntityPermissionsQueryBuilder();
    }

    /**
     * Filter by VAT-UE identifier.
     */
    public EuEntityPermissionsQueryBuilder vatUeIdentifier(String vatUeIdentifier) {
        this.vatUeIdentifier = vatUeIdentifier;
        return this;
    }

    /**
     * Filter by authorized fingerprint identifier.
     */
    public EuEntityPermissionsQueryBuilder authorizedFingerprintIdentifier(String fingerprint) {
        this.authorizedFingerprintIdentifier = fingerprint;
        return this;
    }

    public EuEntityPermissionsQueryBuilder vatUeManage() {
        permissionTypes.add(EuEntityPermissionsQueryPermissionTypeRaw.VAT_UE_MANAGE);
        return this;
    }

    public EuEntityPermissionsQueryBuilder invoiceRead() {
        permissionTypes.add(EuEntityPermissionsQueryPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public EuEntityPermissionsQueryBuilder invoiceWrite() {
        permissionTypes.add(EuEntityPermissionsQueryPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    public EuEntityPermissionsQueryBuilder introspection() {
        permissionTypes.add(EuEntityPermissionsQueryPermissionTypeRaw.INTROSPECTION);
        return this;
    }

    /**
     * Build the EU entity permissions query request.
     *
     * @return the request ready to pass to {@code PermissionClient.queryEuEntities()}
     */
    public EuEntityPermissionsQueryRequestRaw build() {
        EuEntityPermissionsQueryRequestRaw request = new EuEntityPermissionsQueryRequestRaw();

        if (vatUeIdentifier != null) {
            request.setVatUeIdentifier(vatUeIdentifier);
        }

        if (authorizedFingerprintIdentifier != null) {
            request.setAuthorizedFingerprintIdentifier(authorizedFingerprintIdentifier);
        }

        if (!permissionTypes.isEmpty()) {
            request.setPermissionTypes(new ArrayList<>(permissionTypes));
        }

        return request;
    }
}
