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

    public PermissionOperationResult grantPerson(PersonPermissionGrantBuilder builder);

    public PermissionOperationResult grantEntity(EntityPermissionGrantBuilder builder);

    public PermissionOperationResult grantAuthorization(EntityAuthorizationPermissionGrantBuilder builder);

    public PermissionOperationResult grantIndirect(IndirectPermissionGrantBuilder builder);

    public PermissionOperationResult grantSubunit(SubunitPermissionGrantBuilder builder);

    public PermissionOperationResult grantEuEntityAdmin(EuEntityAdminPermissionGrantBuilder builder);

    public PermissionOperationResult grantEuEntity(EuEntityPermissionGrantBuilder builder);

    public PermissionOperationResult revokeCommon(String permissionId);

    public PermissionOperationResult revokeAuthorization(String permissionId);

    public PermissionOperationStatus getOperationStatus(String referenceNumber);

    public AttachmentPermissionStatus getAttachmentStatus();

    public PersonalPermissions queryPersonal(PersonalPermissionsQueryBuilder builder);

    public PersonPermissions queryPersons(PersonPermissionsQueryBuilder builder);

    public SubunitPermissions querySubunits();

    public EntityPermissions queryEntities();

    public EntityRoles queryEntityRoles();

    public SubordinateEntityRoles querySubordinateRoles();

    public EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder);

    public EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryBuilder builder);

}