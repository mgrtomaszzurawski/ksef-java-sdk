/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

import java.util.ArrayList;

/**
 * SDK-record → generated {@code *Raw} mappers for permissions queries.
 * Lives in a non-exported package; consumers can't reach it.
 */
public final class PermissionsQueryRequestMappers {

    private PermissionsQueryRequestMappers() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw toEntityAuthorizationPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw();
        raw.setQueryType(PermissionEnumConverters.toAuthorizationQueryTypeRaw(request.queryType()));
        if (request.authorizingNip() != null) {
            var authorizingId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierRaw()
                    .type(io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw.NIP)
                    .value(request.authorizingNip());
            raw.setAuthorizingIdentifier(authorizingId);
        }
        if (request.authorizedType() != null) {
            var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierRaw()
                    .type(PermissionEnumConverters.toAuthorizedEntityIdentifierTypeRaw(request.authorizedType()))
                    .value(request.authorizedValue());
            raw.setAuthorizedIdentifier(authorizedId);
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toInvoicePermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        return raw;
    }







    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw toPersonPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw();
        raw.setQueryType(PermissionEnumConverters.toPersonPermissionsQueryTypeRaw(request.queryType()));
        applyPersonQueryAuthor(raw, request);
        applyPersonQueryAuthorized(raw, request);
        applyPersonQueryContext(raw, request);
        applyPersonQueryTarget(raw, request);
        applyPersonQueryPermissions(raw, request);
        if (request.permissionState() != null) {
            raw.setPermissionState(PermissionEnumConverters.toPermissionStateRaw(request.permissionState()));
        }
        return raw;
    }

    private static void applyPersonQueryAuthor(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw raw,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.authorType() == null) {
            return;
        }
        var authorId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierRaw()
                .type(PermissionEnumConverters.toPersonAuthorIdentifierTypeRaw(request.authorType()));
        if (request.authorValue() != null) {
            authorId.value(request.authorValue());
        }
        raw.setAuthorIdentifier(authorId);
    }

    private static void applyPersonQueryAuthorized(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw raw,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.authorizedType() == null) {
            return;
        }
        var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierRaw()
                .type(PermissionEnumConverters.toPersonAuthorizedIdentifierTypeRaw(request.authorizedType()))
                .value(request.authorizedValue());
        raw.setAuthorizedIdentifier(authorizedId);
    }

    private static void applyPersonQueryContext(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw raw,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.contextType() == null) {
            return;
        }
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierRaw()
                .type(PermissionEnumConverters.toPersonPermissionsContextIdentifierTypeRaw(request.contextType()))
                .value(request.contextValue());
        raw.setContextIdentifier(contextId);
    }

    private static void applyPersonQueryTarget(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw raw,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.targetType() == null) {
            return;
        }
        var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierRaw()
                .type(PermissionEnumConverters.toPersonPermissionsTargetIdentifierTypeRaw(request.targetType()));
        if (request.targetValue() != null) {
            targetId.value(request.targetValue());
        }
        raw.setTargetIdentifier(targetId);
    }

    private static void applyPersonQueryPermissions(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw raw,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.permissionTypes().isEmpty()) {
            return;
        }
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw>(request.permissionTypes().size());
        for (var type : request.permissionTypes()) {
            perms.add(PermissionsRequestMappers.toPersonPermissionTypeRaw(type));
        }
        raw.setPermissionTypes(perms);
    }











    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw toPersonalPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw();
        if (request.contextType() != null) {
            var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierRaw()
                    .type(PermissionEnumConverters.toPersonalContextIdentifierTypeRaw(request.contextType()))
                    .value(request.contextValue());
            raw.setContextIdentifier(contextId);
        }
        if (request.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierRaw()
                    .type(PermissionEnumConverters.toPersonalTargetIdentifierTypeRaw(request.targetType()));
            if (request.targetValue() != null) {
                targetId.value(request.targetValue());
            }
            raw.setTargetIdentifier(targetId);
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toPersonalPermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        if (request.permissionState() != null) {
            raw.setPermissionState(PermissionEnumConverters.toPermissionStateRaw(request.permissionState()));
        }
        return raw;
    }









    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw toEuEntityPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw();
        if (request.vatUeIdentifier() != null) {
            raw.setVatUeIdentifier(request.vatUeIdentifier());
        }
        if (request.authorizedFingerprintIdentifier() != null) {
            raw.setAuthorizedFingerprintIdentifier(request.authorizedFingerprintIdentifier());
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toEuEntityQueryPermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        return raw;
    }



}
