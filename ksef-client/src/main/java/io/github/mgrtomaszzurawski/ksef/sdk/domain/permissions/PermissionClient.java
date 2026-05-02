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
}
