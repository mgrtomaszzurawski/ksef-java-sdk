/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

import java.util.ArrayList;

/**
 * SDK-record → generated {@code *Raw} mappers for permissions queries.
 * Lives in a non-exported package; consumers can't reach it.
 *
 * @since 0.1.0
 */
public final class PermissionsQueryRequestMappers {

    private PermissionsQueryRequestMappers() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw toEntityAuthorizationPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw();
        rawValue.setQueryType(PermissionEnumConverters.toAuthorizationQueryTypeRaw(request.queryType()));
        if (request.authorizingNip() != null) {
            var authorizingId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierRaw()
                    .type(io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw.NIP)
                    .value(request.authorizingNip());
            rawValue.setAuthorizingIdentifier(authorizingId);
        }
        if (request.authorizedType() != null) {
            var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierRaw()
                    .type(PermissionEnumConverters.toAuthorizedEntityIdentifierTypeRaw(request.authorizedType()))
                    .value(request.authorizedValue());
            rawValue.setAuthorizedIdentifier(authorizedId);
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toInvoicePermissionTypeRaw(type));
            }
            rawValue.setPermissionTypes(perms);
        }
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw toPersonPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw();
        rawValue.setQueryType(PermissionEnumConverters.toPersonPermissionsQueryTypeRaw(request.queryType()));
        applyPersonQueryAuthor(rawValue, request);
        applyPersonQueryAuthorized(rawValue, request);
        applyPersonQueryContext(rawValue, request);
        applyPersonQueryTarget(rawValue, request);
        applyPersonQueryPermissions(rawValue, request);
        if (request.permissionState() != null) {
            rawValue.setPermissionState(PermissionEnumConverters.toPermissionStateRaw(request.permissionState()));
        }
        return rawValue;
    }

    private static void applyPersonQueryAuthor(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw rawValue,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.authorType() == null) {
            return;
        }
        var authorId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierRaw()
                .type(PermissionEnumConverters.toPersonAuthorIdentifierTypeRaw(request.authorType()));
        if (request.authorValue() != null) {
            authorId.value(request.authorValue());
        }
        rawValue.setAuthorIdentifier(authorId);
    }

    private static void applyPersonQueryAuthorized(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw rawValue,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.authorizedType() == null) {
            return;
        }
        var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierRaw()
                .type(PermissionEnumConverters.toPersonAuthorizedIdentifierTypeRaw(request.authorizedType()))
                .value(request.authorizedValue());
        rawValue.setAuthorizedIdentifier(authorizedId);
    }

    private static void applyPersonQueryContext(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw rawValue,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.contextType() == null) {
            return;
        }
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierRaw()
                .type(PermissionEnumConverters.toPersonPermissionsContextIdentifierTypeRaw(request.contextType()))
                .value(request.contextValue());
        rawValue.setContextIdentifier(contextId);
    }

    private static void applyPersonQueryTarget(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw rawValue,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.targetType() == null) {
            return;
        }
        var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierRaw()
                .type(PermissionEnumConverters.toPersonPermissionsTargetIdentifierTypeRaw(request.targetType()));
        if (request.targetValue() != null) {
            targetId.value(request.targetValue());
        }
        rawValue.setTargetIdentifier(targetId);
    }

    private static void applyPersonQueryPermissions(
            io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw rawValue,
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest request) {
        if (request.permissionTypes().isEmpty()) {
            return;
        }
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw>(request.permissionTypes().size());
        for (var type : request.permissionTypes()) {
            perms.add(PermissionsRequestMappers.toPersonPermissionTypeRaw(type));
        }
        rawValue.setPermissionTypes(perms);
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw toPersonalPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw();
        if (request.contextType() != null) {
            var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierRaw()
                    .type(PermissionEnumConverters.toPersonalContextIdentifierTypeRaw(request.contextType()))
                    .value(request.contextValue());
            rawValue.setContextIdentifier(contextId);
        }
        if (request.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierRaw()
                    .type(PermissionEnumConverters.toPersonalTargetIdentifierTypeRaw(request.targetType()));
            if (request.targetValue() != null) {
                targetId.value(request.targetValue());
            }
            rawValue.setTargetIdentifier(targetId);
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toPersonalPermissionTypeRaw(type));
            }
            rawValue.setPermissionTypes(perms);
        }
        if (request.permissionState() != null) {
            rawValue.setPermissionState(PermissionEnumConverters.toPermissionStateRaw(request.permissionState()));
        }
        return rawValue;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw toEuEntityPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest request) {
        var rawValue = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw();
        if (request.vatUeIdentifier() != null) {
            rawValue.setVatUeIdentifier(request.vatUeIdentifier());
        }
        if (request.authorizedFingerprintIdentifier() != null) {
            rawValue.setAuthorizedFingerprintIdentifier(request.authorizedFingerprintIdentifier());
        }
        if (!request.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw>(request.permissionTypes().size());
            for (var type : request.permissionTypes()) {
                perms.add(PermissionEnumConverters.toEuEntityQueryPermissionTypeRaw(type));
            }
            rawValue.setPermissionTypes(perms);
        }
        return rawValue;
    }

}
