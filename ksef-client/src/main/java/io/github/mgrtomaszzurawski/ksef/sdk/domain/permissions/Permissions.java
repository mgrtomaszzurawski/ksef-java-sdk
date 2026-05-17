/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRolesQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityAdminPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRolesQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionsQueryRequest;
import java.time.Duration;

/**
 * KSeF permission management — granting, revoking, and querying permissions
 * for persons, entities, EU entities, subunits, and authorizations.
 *
 * <p>Reached via {@code KsefClient.permissions()}.
 *
 * <p>Per ADR-032, every {@code grant*} and {@code revoke*} method is
 * synchronous: the SDK polls the KSeF operation-status endpoint
 * internally and returns when the operation reaches a terminal state.
 * Each method ships in two overloads — one using a sensible default
 * timeout, one accepting an explicit {@link Duration}.
 *
 * @since 1.0.0
 */
public interface Permissions {

    PermissionOperationStatus grantPerson(PersonPermissionGrantRequest request);
    PermissionOperationStatus grantPerson(PersonPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantEntity(EntityPermissionGrantRequest request);
    PermissionOperationStatus grantEntity(EntityPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantAuthorization(EntityAuthorizationPermissionGrantRequest request);
    PermissionOperationStatus grantAuthorization(EntityAuthorizationPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantIndirect(IndirectPermissionGrantRequest request);
    PermissionOperationStatus grantIndirect(IndirectPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantSubunit(SubunitPermissionGrantRequest request);
    PermissionOperationStatus grantSubunit(SubunitPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantEuEntityAdmin(EuEntityAdminPermissionGrantRequest request);
    PermissionOperationStatus grantEuEntityAdmin(EuEntityAdminPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus grantEuEntity(EuEntityPermissionGrantRequest request);
    PermissionOperationStatus grantEuEntity(EuEntityPermissionGrantRequest request, Duration timeout);

    PermissionOperationStatus revokePermission(String permissionId);
    PermissionOperationStatus revokePermission(String permissionId, Duration timeout);

    PermissionOperationStatus revokeAuthorization(String permissionId);
    PermissionOperationStatus revokeAuthorization(String permissionId, Duration timeout);

    AttachmentPermissionStatus getAttachmentStatus();

    PersonalPermissions queryPersonal(PersonalPermissionsQueryRequest request);
    PersonPermissions queryPersons(PersonPermissionsQueryRequest request);
    SubunitPermissions querySubunits(SubunitPermissionsQueryRequest filter);
    EntityPermissions queryEntities(EntityPermissionsQueryRequest filter);
    EntityRoles queryEntityRoles(EntityRolesQueryRequest filter);
    SubordinateEntityRoles querySubordinateRoles(SubordinateEntityRolesQueryRequest filter);
    EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryRequest request);
    EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryRequest request);

    // Stream-based paginators. Each lazily walks pageOffset = 0, 1, 2, ...
    // until the server reports hasMore=false, yielding one record at a time.
    // The SDK never materialises the full result set — memory pressure is
    // bounded by what the caller pulls from the stream. For a hard cap, pipe
    // through .limit(N); for a snapshot list, pipe through .toList().
    //
    // request.pageOffset() is ignored by stream* methods — the paginator
    // always starts from page 0. Use query* for snapshot at a specific
    // offset.

    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            streamPersonal(PersonalPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(SubunitPermissionsQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            streamEntities(EntityPermissionsQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole>
            streamEntityRoles(EntityRolesQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(SubordinateEntityRolesQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            streamAuthorizations(EntityAuthorizationPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryRequest request);
}
