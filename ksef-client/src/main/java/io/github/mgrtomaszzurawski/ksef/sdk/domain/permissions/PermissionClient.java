/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;

/**
 * Client for KSeF permission management — granting, revoking, and querying permissions
 * for persons, entities, EU entities, subunits, and authorizations.
 */
public interface PermissionClient {

    PermissionOperationResult grantPerson(PersonPermissionGrantBuilder builder);
    PermissionOperationResult grantEntity(EntityPermissionGrantBuilder builder);
    PermissionOperationResult grantAuthorization(EntityAuthorizationPermissionGrantBuilder builder);
    PermissionOperationResult grantIndirect(IndirectPermissionGrantBuilder builder);
    PermissionOperationResult grantSubunit(SubunitPermissionGrantBuilder builder);
    PermissionOperationResult grantEuEntityAdmin(EuEntityAdminPermissionGrantBuilder builder);
    PermissionOperationResult grantEuEntity(EuEntityPermissionGrantBuilder builder);
    PermissionOperationResult revokeCommon(String permissionId);
    PermissionOperationResult revokeAuthorization(String permissionId);
    PermissionOperationStatus getOperationStatus(String referenceNumber);
    AttachmentPermissionStatus getAttachmentStatus();
    PersonalPermissions queryPersonal(PersonalPermissionsQueryBuilder builder);
    PersonPermissions queryPersons(PersonPermissionsQueryBuilder builder);
    SubunitPermissions querySubunits();
    EntityPermissions queryEntities();
    EntityRoles queryEntityRoles();
    SubordinateEntityRoles querySubordinateRoles();
    EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder);
    EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryBuilder builder);

    // Codex round-9 manual-validation A.4.1 — queryAll variants. Each
    // walks pageOffset internally with spec-max page size and returns one
    // flat list. Saves consumers from composing pagination loops.
    //
    // Implemented as default methods so the interface remains
    // source-compatible for any external implementor (the impl pattern is
    // canonical: call single-page method while hasMore() is true). The
    // actual paginated impl in PermissionClientImpl overrides each one
    // with the spec-max pageSize=250 + URL-level pageOffset query param.
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            queryAllPersonal(PersonalPermissionsQueryBuilder builder) {
        return loopWhileHasMore(() -> queryPersonal(builder),
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions::permissions);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            queryAllPersons(PersonPermissionsQueryBuilder builder) {
        return loopWhileHasMore(() -> queryPersons(builder),
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions::permissions);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            queryAllSubunits() {
        return loopWhileHasMore(this::querySubunits,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions::permissions);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            queryAllEntities() {
        return loopWhileHasMore(this::queryEntities,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions::permissions);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            queryAllSubordinateRoles() {
        return loopWhileHasMore(this::querySubordinateRoles,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles::roles);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            queryAllAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder) {
        return loopWhileHasMore(() -> queryAuthorizations(builder),
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions::authorizationGrants);
    }
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            queryAllEuEntities(EuEntityPermissionsQueryBuilder builder) {
        return loopWhileHasMore(() -> queryEuEntities(builder),
                io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions::permissions);
    }

    /**
     * Default-method helper. The default fallback returns only the FIRST
     * page's items because the single-page {@code query*} methods don't
     * accept a pageOffset parameter — looping would re-fetch the same page
     * forever. The real impl in {@code PermissionClientImpl} overrides each
     * {@code queryAll*} method with proper URL-level pageOffset iteration.
     * Default exists so external implementors of this interface (test
     * doubles, mocks) keep compiling without forcing them to wire up
     * pagination — they get first-page semantics for free, which matches
     * the existing {@code query*} method behaviour.
     */
    private static <P, I> java.util.List<I> loopWhileHasMore(
            java.util.function.Supplier<P> fetchPage,
            java.util.function.Function<P, java.util.List<I>> extractItems) {
        return java.util.List.copyOf(extractItems.apply(fetchPage.get()));
    }
}
