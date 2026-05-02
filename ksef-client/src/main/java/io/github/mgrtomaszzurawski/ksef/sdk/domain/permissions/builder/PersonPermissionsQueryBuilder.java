/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.PermissionStateRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierTypeRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for person permissions query requests.
 * <p>
 * Required: query type. All other fields are optional.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = PersonPermissionsQueryBuilder
 *     .permissionsInCurrentContext()
 *     .authorizedByPesel("82060411457")
 *     .activeOnly()
 *     .invoiceRead();
 * }</pre>
 */
public final class PersonPermissionsQueryBuilder {

    private static final String ERR_QUERY_TYPE_REQUIRED = "queryType is required — use .permissionsInCurrentContext() or .permissionsGrantedInCurrentContext()";

    private final PersonPermissionsQueryTypeRaw queryType;
    private PersonPermissionsAuthorIdentifierTypeRaw authorType;
    private String authorValue;
    private PersonPermissionsAuthorizedIdentifierTypeRaw authorizedType;
    private String authorizedValue;
    private PersonPermissionsContextIdentifierTypeRaw contextType;
    private String contextValue;
    private PersonPermissionsTargetIdentifierTypeRaw targetType;
    private String targetValue;
    private final List<PersonPermissionTypeRaw> permissionTypes = new ArrayList<>();
    private PermissionStateRaw permissionState;

    private PersonPermissionsQueryBuilder(PersonPermissionsQueryTypeRaw queryType) {
        this.queryType = Objects.requireNonNull(queryType, ERR_QUERY_TYPE_REQUIRED);
    }

    /**
     * Query permissions in the current context.
     */
    public static PersonPermissionsQueryBuilder permissionsInCurrentContext() {
        return new PersonPermissionsQueryBuilder(PersonPermissionsQueryTypeRaw.PERMISSIONS_IN_CURRENT_CONTEXT);
    }

    /**
     * Query permissions granted in the current context.
     */
    public static PersonPermissionsQueryBuilder permissionsGrantedInCurrentContext() {
        return new PersonPermissionsQueryBuilder(PersonPermissionsQueryTypeRaw.PERMISSIONS_GRANTED_IN_CURRENT_CONTEXT);
    }

    /**
     * Filter by author NIP.
     */
    public PersonPermissionsQueryBuilder authorByNip(String nip) {
        this.authorType = PersonPermissionsAuthorIdentifierTypeRaw.NIP;
        this.authorValue = nip;
        return this;
    }

    /**
     * Filter by author PESEL.
     */
    public PersonPermissionsQueryBuilder authorByPesel(String pesel) {
        this.authorType = PersonPermissionsAuthorIdentifierTypeRaw.PESEL;
        this.authorValue = pesel;
        return this;
    }

    /**
     * Filter by author fingerprint.
     */
    public PersonPermissionsQueryBuilder authorByFingerprint(String fingerprint) {
        this.authorType = PersonPermissionsAuthorIdentifierTypeRaw.FINGERPRINT;
        this.authorValue = fingerprint;
        return this;
    }

    /**
     * Filter by system author.
     */
    public PersonPermissionsQueryBuilder authorSystem() {
        this.authorType = PersonPermissionsAuthorIdentifierTypeRaw.SYSTEM;
        this.authorValue = null;
        return this;
    }

    /**
     * Filter by authorized person NIP.
     */
    public PersonPermissionsQueryBuilder authorizedByNip(String nip) {
        this.authorizedType = PersonPermissionsAuthorizedIdentifierTypeRaw.NIP;
        this.authorizedValue = nip;
        return this;
    }

    /**
     * Filter by authorized person PESEL.
     */
    public PersonPermissionsQueryBuilder authorizedByPesel(String pesel) {
        this.authorizedType = PersonPermissionsAuthorizedIdentifierTypeRaw.PESEL;
        this.authorizedValue = pesel;
        return this;
    }

    /**
     * Filter by authorized person fingerprint.
     */
    public PersonPermissionsQueryBuilder authorizedByFingerprint(String fingerprint) {
        this.authorizedType = PersonPermissionsAuthorizedIdentifierTypeRaw.FINGERPRINT;
        this.authorizedValue = fingerprint;
        return this;
    }

    /**
     * Filter by context NIP.
     */
    public PersonPermissionsQueryBuilder contextNip(String nip) {
        this.contextType = PersonPermissionsContextIdentifierTypeRaw.NIP;
        this.contextValue = nip;
        return this;
    }

    /**
     * Filter by context internal ID.
     */
    public PersonPermissionsQueryBuilder contextInternalId(String internalId) {
        this.contextType = PersonPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        this.contextValue = internalId;
        return this;
    }

    /**
     * Filter by target NIP.
     */
    public PersonPermissionsQueryBuilder targetNip(String nip) {
        this.targetType = PersonPermissionsTargetIdentifierTypeRaw.NIP;
        this.targetValue = nip;
        return this;
    }

    /**
     * Filter by target to all partners.
     */
    public PersonPermissionsQueryBuilder targetAllPartners() {
        this.targetType = PersonPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    /**
     * Filter by target internal ID.
     */
    public PersonPermissionsQueryBuilder targetInternalId(String internalId) {
        this.targetType = PersonPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        this.targetValue = internalId;
        return this;
    }

    public PersonPermissionsQueryBuilder invoiceRead() {
        permissionTypes.add(PersonPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public PersonPermissionsQueryBuilder invoiceWrite() {
        permissionTypes.add(PersonPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    public PersonPermissionsQueryBuilder credentialsRead() {
        permissionTypes.add(PersonPermissionTypeRaw.CREDENTIALS_READ);
        return this;
    }

    public PersonPermissionsQueryBuilder credentialsManage() {
        permissionTypes.add(PersonPermissionTypeRaw.CREDENTIALS_MANAGE);
        return this;
    }

    public PersonPermissionsQueryBuilder subunitManage() {
        permissionTypes.add(PersonPermissionTypeRaw.SUBUNIT_MANAGE);
        return this;
    }

    public PersonPermissionsQueryBuilder enforcementOperations() {
        permissionTypes.add(PersonPermissionTypeRaw.ENFORCEMENT_OPERATIONS);
        return this;
    }

    public PersonPermissionsQueryBuilder introspection() {
        permissionTypes.add(PersonPermissionTypeRaw.INTROSPECTION);
        return this;
    }

    /**
     * Filter by active permissions only.
     */
    public PersonPermissionsQueryBuilder activeOnly() {
        this.permissionState = PermissionStateRaw.ACTIVE;
        return this;
    }

    /**
     * Filter by inactive permissions only.
     */
    public PersonPermissionsQueryBuilder inactiveOnly() {
        this.permissionState = PermissionStateRaw.INACTIVE;
        return this;
    }

    /**
     * Build the person permissions query request.
     *
     * @return the request ready to pass to {@code PermissionClient.queryPersons()}
     */
    public PersonPermissionsQueryRequestRaw build() {
        PersonPermissionsQueryRequestRaw request = new PersonPermissionsQueryRequestRaw();
        request.setQueryType(queryType);

        if (authorType != null) {
            PersonPermissionsAuthorIdentifierRaw authorId =
                    new PersonPermissionsAuthorIdentifierRaw()
                            .type(authorType);
            if (authorValue != null) {
                authorId.value(authorValue);
            }
            request.setAuthorIdentifier(authorId);
        }

        if (authorizedType != null) {
            PersonPermissionsAuthorizedIdentifierRaw authorizedId =
                    new PersonPermissionsAuthorizedIdentifierRaw()
                            .type(authorizedType)
                            .value(authorizedValue);
            request.setAuthorizedIdentifier(authorizedId);
        }

        if (contextType != null) {
            PersonPermissionsContextIdentifierRaw contextId =
                    new PersonPermissionsContextIdentifierRaw()
                            .type(contextType)
                            .value(contextValue);
            request.setContextIdentifier(contextId);
        }

        if (targetType != null) {
            PersonPermissionsTargetIdentifierRaw targetId =
                    new PersonPermissionsTargetIdentifierRaw()
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
