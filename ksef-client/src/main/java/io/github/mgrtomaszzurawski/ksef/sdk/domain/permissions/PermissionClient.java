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
 *
 * @since 1.0.0
 */
public interface PermissionClient {

    /**
     * KSeF permission-operation status codes are conventionally
     * {@code 100/110} (in-progress), {@code 200} (success), and
     * {@code 4xx/5xx} (failure). Anything {@code &gt;= 200} is terminal.
     *
     * <p>Useful when building a poll predicate for
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.common.KsefAsync#awaitTerminal}:
     * <pre>{@code
     * status -> status.status() != null
     *           && status.status().code() >= PermissionClient.TERMINAL_STATUS_CODE_THRESHOLD
     * }</pre>
     */
    int TERMINAL_STATUS_CODE_THRESHOLD = 200;

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
    SubunitPermissions querySubunits(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder filter);
    EntityPermissions queryEntities(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder filter);
    EntityRoles queryEntityRoles(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder filter);
    SubordinateEntityRoles querySubordinateRoles(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder filter);
    EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder);
    EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryBuilder builder);

    // Stream-based paginators (AWS-SDK-style). Each lazily walks
    // pageOffset = 0, 1, 2, ... until the server reports hasMore=false,
    // yielding one record at a time. The SDK never materialises the full
    // result set — memory pressure is bounded by what the caller pulls
    // from the stream. For a hard cap, pipe through {@code .limit(N)};
    // for a snapshot list, pipe through {@code .toList()}.

    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            streamPersonal(PersonalPermissionsQueryBuilder builder);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryBuilder builder);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            streamEntities(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            streamAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryBuilder builder);
}
