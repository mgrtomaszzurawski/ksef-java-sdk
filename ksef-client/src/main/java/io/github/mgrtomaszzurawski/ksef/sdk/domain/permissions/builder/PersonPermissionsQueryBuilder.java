/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionState;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonAuthorIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalContextIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalTargetIdentifierType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for person permissions query requests.
 * <p>Required: query type. All other fields are optional.
 *
 * @since 0.1.0
 */
public final class PersonPermissionsQueryBuilder {

    private static final String ERR_QUERY_TYPE_REQUIRED = "queryType is required — use .permissionsInCurrentContext() or .permissionsGrantedInCurrentContext()";

    private final PersonPermissionsQueryType queryType;
    private @Nullable PersonAuthorIdentifierType authorType;
    private @Nullable String authorValue;
    private @Nullable PersonSubjectIdentifierType authorizedType;
    private @Nullable String authorizedValue;
    private @Nullable PersonalContextIdentifierType contextType;
    private @Nullable String contextValue;
    private @Nullable PersonalTargetIdentifierType targetType;
    private @Nullable String targetValue;
    private final List<PersonPermissionType> permissionTypes = new ArrayList<>();
    private @Nullable PermissionState permissionState;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

    private PersonPermissionsQueryBuilder(PersonPermissionsQueryType queryType) {
        this.queryType = Objects.requireNonNull(queryType, ERR_QUERY_TYPE_REQUIRED);
    }

    public static PersonPermissionsQueryBuilder permissionsInCurrentContext() {
        return new PersonPermissionsQueryBuilder(PersonPermissionsQueryType.PERMISSIONS_IN_CURRENT_CONTEXT);
    }

    public static PersonPermissionsQueryBuilder permissionsGrantedInCurrentContext() {
        return new PersonPermissionsQueryBuilder(PersonPermissionsQueryType.PERMISSIONS_GRANTED_IN_CURRENT_CONTEXT);
    }

    public PersonPermissionsQueryBuilder authorByNip(String nip) {
        this.authorType = PersonAuthorIdentifierType.NIP;
        this.authorValue = nip;
        return this;
    }

    public PersonPermissionsQueryBuilder authorByPesel(String pesel) {
        this.authorType = PersonAuthorIdentifierType.PESEL;
        this.authorValue = pesel;
        return this;
    }

    public PersonPermissionsQueryBuilder authorByFingerprint(String fingerprint) {
        this.authorType = PersonAuthorIdentifierType.FINGERPRINT;
        this.authorValue = fingerprint;
        return this;
    }

    public PersonPermissionsQueryBuilder authorSystem() {
        this.authorType = PersonAuthorIdentifierType.SYSTEM;
        this.authorValue = null;
        return this;
    }

    public PersonPermissionsQueryBuilder authorizedByNip(String nip) {
        this.authorizedType = PersonSubjectIdentifierType.NIP;
        this.authorizedValue = nip;
        return this;
    }

    public PersonPermissionsQueryBuilder authorizedByPesel(String pesel) {
        this.authorizedType = PersonSubjectIdentifierType.PESEL;
        this.authorizedValue = pesel;
        return this;
    }

    public PersonPermissionsQueryBuilder authorizedByFingerprint(String fingerprint) {
        this.authorizedType = PersonSubjectIdentifierType.FINGERPRINT;
        this.authorizedValue = fingerprint;
        return this;
    }

    public PersonPermissionsQueryBuilder contextNip(String nip) {
        this.contextType = PersonalContextIdentifierType.NIP;
        this.contextValue = nip;
        return this;
    }

    public PersonPermissionsQueryBuilder contextInternalId(String internalId) {
        this.contextType = PersonalContextIdentifierType.INTERNAL_ID;
        this.contextValue = internalId;
        return this;
    }

    public PersonPermissionsQueryBuilder targetNip(String nip) {
        this.targetType = PersonalTargetIdentifierType.NIP;
        this.targetValue = nip;
        return this;
    }

    public PersonPermissionsQueryBuilder targetAllPartners() {
        this.targetType = PersonalTargetIdentifierType.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    public PersonPermissionsQueryBuilder targetInternalId(String internalId) {
        this.targetType = PersonalTargetIdentifierType.INTERNAL_ID;
        this.targetValue = internalId;
        return this;
    }

    public PersonPermissionsQueryBuilder invoiceRead() { permissionTypes.add(PersonPermissionType.INVOICE_READ); return this; }
    public PersonPermissionsQueryBuilder invoiceWrite() { permissionTypes.add(PersonPermissionType.INVOICE_WRITE); return this; }
    public PersonPermissionsQueryBuilder credentialsRead() { permissionTypes.add(PersonPermissionType.CREDENTIALS_READ); return this; }
    public PersonPermissionsQueryBuilder credentialsManage() { permissionTypes.add(PersonPermissionType.CREDENTIALS_MANAGE); return this; }
    public PersonPermissionsQueryBuilder subunitManage() { permissionTypes.add(PersonPermissionType.SUBUNIT_MANAGE); return this; }
    public PersonPermissionsQueryBuilder enforcementOperations() { permissionTypes.add(PersonPermissionType.ENFORCEMENT_OPERATIONS); return this; }
    public PersonPermissionsQueryBuilder introspection() { permissionTypes.add(PersonPermissionType.INTROSPECTION); return this; }

    public PersonPermissionsQueryBuilder activeOnly() {
        this.permissionState = PermissionState.ACTIVE;
        return this;
    }

    public PersonPermissionsQueryBuilder inactiveOnly() {
        this.permissionState = PermissionState.INACTIVE;
        return this;
    }

    /**
     * Zero-based page offset for {@code queryPersons}. Default (null) → 0.
     * Must be {@code >= 0}; validated at {@code build()} time. Ignored on
     * {@code streamPersons} (the SDK walks pages itself).
     */
    public PersonPermissionsQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    /**
     * Page size for {@code queryPersons}. KSeF range {@code [10, 100]};
     * validated at {@code build()} time. Default (null) → 100 (SDK uses
     * spec-max). Ignored on {@code streamPersons} (the SDK uses spec-max
     * for fewest round-trips).
     */
    public PersonPermissionsQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PersonPermissionsQueryBuilder toBuilder() {
        PersonPermissionsQueryBuilder copy = new PersonPermissionsQueryBuilder(this.queryType);
        copy.authorType = this.authorType;
        copy.authorValue = this.authorValue;
        copy.authorizedType = this.authorizedType;
        copy.authorizedValue = this.authorizedValue;
        copy.contextType = this.contextType;
        copy.contextValue = this.contextValue;
        copy.targetType = this.targetType;
        copy.targetValue = this.targetValue;
        copy.permissionTypes.addAll(this.permissionTypes);
        copy.permissionState = this.permissionState;
        copy.pageOffset = this.pageOffset;
        copy.pageSize = this.pageSize;
        return copy;
    }

    public PersonPermissionsQueryRequest build() {
        return new PersonPermissionsQueryRequest(queryType, authorType, authorValue,
                authorizedType, authorizedValue, contextType, contextValue,
                targetType, targetValue, permissionTypes, permissionState,
                pageOffset, pageSize);
    }
}
