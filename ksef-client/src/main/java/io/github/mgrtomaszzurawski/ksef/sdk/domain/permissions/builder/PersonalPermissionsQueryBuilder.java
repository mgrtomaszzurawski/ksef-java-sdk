/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionState;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalContextIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalTargetIdentifierType;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for personal permissions query requests. All fields are optional.
 */
public final class PersonalPermissionsQueryBuilder {

    private PersonalContextIdentifierType contextType;
    private String contextValue;
    private PersonalTargetIdentifierType targetType;
    private String targetValue;
    private final List<PersonalPermissionType> permissionTypes = new ArrayList<>();
    private PermissionState permissionState;

    private PersonalPermissionsQueryBuilder() { }

    public static PersonalPermissionsQueryBuilder create() {
        return new PersonalPermissionsQueryBuilder();
    }

    public PersonalPermissionsQueryBuilder contextNip(String nip) {
        this.contextType = PersonalContextIdentifierType.NIP;
        this.contextValue = nip;
        return this;
    }

    public PersonalPermissionsQueryBuilder contextInternalId(String internalId) {
        this.contextType = PersonalContextIdentifierType.INTERNAL_ID;
        this.contextValue = internalId;
        return this;
    }

    public PersonalPermissionsQueryBuilder targetNip(String nip) {
        this.targetType = PersonalTargetIdentifierType.NIP;
        this.targetValue = nip;
        return this;
    }

    public PersonalPermissionsQueryBuilder targetAllPartners() {
        this.targetType = PersonalTargetIdentifierType.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    public PersonalPermissionsQueryBuilder targetInternalId(String internalId) {
        this.targetType = PersonalTargetIdentifierType.INTERNAL_ID;
        this.targetValue = internalId;
        return this;
    }

    public PersonalPermissionsQueryBuilder invoiceRead() { permissionTypes.add(PersonalPermissionType.INVOICE_READ); return this; }
    public PersonalPermissionsQueryBuilder invoiceWrite() { permissionTypes.add(PersonalPermissionType.INVOICE_WRITE); return this; }
    public PersonalPermissionsQueryBuilder credentialsRead() { permissionTypes.add(PersonalPermissionType.CREDENTIALS_READ); return this; }
    public PersonalPermissionsQueryBuilder credentialsManage() { permissionTypes.add(PersonalPermissionType.CREDENTIALS_MANAGE); return this; }
    public PersonalPermissionsQueryBuilder subunitManage() { permissionTypes.add(PersonalPermissionType.SUBUNIT_MANAGE); return this; }
    public PersonalPermissionsQueryBuilder enforcementOperations() { permissionTypes.add(PersonalPermissionType.ENFORCEMENT_OPERATIONS); return this; }
    public PersonalPermissionsQueryBuilder introspection() { permissionTypes.add(PersonalPermissionType.INTROSPECTION); return this; }
    public PersonalPermissionsQueryBuilder vatUeManage() { permissionTypes.add(PersonalPermissionType.VAT_UE_MANAGE); return this; }

    public PersonalPermissionsQueryBuilder activeOnly() {
        this.permissionState = PermissionState.ACTIVE;
        return this;
    }

    public PersonalPermissionsQueryBuilder inactiveOnly() {
        this.permissionState = PermissionState.INACTIVE;
        return this;
    }

    public PersonalPermissionsQueryBuilder toBuilder() {
        PersonalPermissionsQueryBuilder copy = new PersonalPermissionsQueryBuilder();
        copy.contextType = this.contextType;
        copy.contextValue = this.contextValue;
        copy.targetType = this.targetType;
        copy.targetValue = this.targetValue;
        copy.permissionTypes.addAll(this.permissionTypes);
        copy.permissionState = this.permissionState;
        return copy;
    }

    public PersonalPermissionsQueryRequest build() {
        return new PersonalPermissionsQueryRequest(contextType, contextValue, targetType, targetValue,
                permissionTypes, permissionState);
    }
}
