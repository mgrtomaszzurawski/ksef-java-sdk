/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;
import io.github.mgrtomaszzurawski.ksef.sdk.permissions.PermissionClient;

import io.github.mgrtomaszzurawski.ksef.client.model.PermissionStateRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierTypeRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for personal permissions query requests (permissions granted to the authenticated user).
 * <p>
 * All fields are optional.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = PersonalPermissionsQueryBuilder.create()
 *     .contextNip("1234567890")
 *     .activeOnly()
 *     .invoiceRead()
 *     .invoiceWrite();
 * }</pre>
 */
public final class PersonalPermissionsQueryBuilder {

    private PersonalPermissionsContextIdentifierTypeRaw contextType;
    private String contextValue;
    private PersonalPermissionsTargetIdentifierTypeRaw targetType;
    private String targetValue;
    private final List<PersonalPermissionTypeRaw> permissionTypes = new ArrayList<>();
    private PermissionStateRaw permissionState;

    private PersonalPermissionsQueryBuilder() {
    }

    /**
     * Create a new query builder with all optional fields.
     */
    public static PersonalPermissionsQueryBuilder create() {
        return new PersonalPermissionsQueryBuilder();
    }

    /**
     * Filter by context NIP.
     */
    public PersonalPermissionsQueryBuilder contextNip(String nip) {
        this.contextType = PersonalPermissionsContextIdentifierTypeRaw.NIP;
        this.contextValue = nip;
        return this;
    }

    /**
     * Filter by context internal ID.
     */
    public PersonalPermissionsQueryBuilder contextInternalId(String internalId) {
        this.contextType = PersonalPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        this.contextValue = internalId;
        return this;
    }

    /**
     * Filter by target NIP.
     */
    public PersonalPermissionsQueryBuilder targetNip(String nip) {
        this.targetType = PersonalPermissionsTargetIdentifierTypeRaw.NIP;
        this.targetValue = nip;
        return this;
    }

    /**
     * Filter by target to all partners.
     */
    public PersonalPermissionsQueryBuilder targetAllPartners() {
        this.targetType = PersonalPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    /**
     * Filter by target internal ID.
     */
    public PersonalPermissionsQueryBuilder targetInternalId(String internalId) {
        this.targetType = PersonalPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        this.targetValue = internalId;
        return this;
    }

    public PersonalPermissionsQueryBuilder invoiceRead() {
        permissionTypes.add(PersonalPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public PersonalPermissionsQueryBuilder invoiceWrite() {
        permissionTypes.add(PersonalPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    public PersonalPermissionsQueryBuilder credentialsRead() {
        permissionTypes.add(PersonalPermissionTypeRaw.CREDENTIALS_READ);
        return this;
    }

    public PersonalPermissionsQueryBuilder credentialsManage() {
        permissionTypes.add(PersonalPermissionTypeRaw.CREDENTIALS_MANAGE);
        return this;
    }

    public PersonalPermissionsQueryBuilder subunitManage() {
        permissionTypes.add(PersonalPermissionTypeRaw.SUBUNIT_MANAGE);
        return this;
    }

    public PersonalPermissionsQueryBuilder enforcementOperations() {
        permissionTypes.add(PersonalPermissionTypeRaw.ENFORCEMENT_OPERATIONS);
        return this;
    }

    public PersonalPermissionsQueryBuilder introspection() {
        permissionTypes.add(PersonalPermissionTypeRaw.INTROSPECTION);
        return this;
    }

    public PersonalPermissionsQueryBuilder vatUeManage() {
        permissionTypes.add(PersonalPermissionTypeRaw.VAT_UE_MANAGE);
        return this;
    }

    /**
     * Filter by active permissions only.
     */
    public PersonalPermissionsQueryBuilder activeOnly() {
        this.permissionState = PermissionStateRaw.ACTIVE;
        return this;
    }

    /**
     * Filter by inactive permissions only.
     */
    public PersonalPermissionsQueryBuilder inactiveOnly() {
        this.permissionState = PermissionStateRaw.INACTIVE;
        return this;
    }

    /**
     * Build the personal permissions query request.
     *
     * @return the request ready to pass to {@code PermissionClient.queryPersonal()}
     */
    public PersonalPermissionsQueryRequestRaw build() {
        PersonalPermissionsQueryRequestRaw request = new PersonalPermissionsQueryRequestRaw();

        if (contextType != null) {
            PersonalPermissionsContextIdentifierRaw contextId =
                    new PersonalPermissionsContextIdentifierRaw()
                            .type(contextType)
                            .value(contextValue);
            request.setContextIdentifier(contextId);
        }

        if (targetType != null) {
            PersonalPermissionsTargetIdentifierRaw targetId =
                    new PersonalPermissionsTargetIdentifierRaw()
                            .type(targetType);
            if (targetValue != null) {
                targetId.value(targetValue);
            }
            request.setTargetIdentifier(targetId);
        }

        if (!permissionTypes.isEmpty()) {
            request.setPermissionTypes(new ArrayList<>(permissionTypes));
        }

        if (permissionState != null) {
            request.setPermissionState(permissionState);
        }

        return request;
    }
}
