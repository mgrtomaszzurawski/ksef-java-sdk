/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.CheckAttachmentPermissionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationGrantRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityRoleRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityAuthorizationPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEuEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonalPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubordinateEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubunitPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRoleRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionSubjectDetails;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;
import java.util.List;

/**
 * Internal mappers from generated {@code *Raw} types to public permissions
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
 */
public final class PermissionsMappers {

    private PermissionsMappers() { }

    public static AttachmentPermissionStatus toAttachmentPermissionStatus(CheckAttachmentPermissionStatusResponseRaw rawValue) {
        return new AttachmentPermissionStatus(
                rawValue.getIsAttachmentAllowed(),
                rawValue.getRevokedDate());
    }

    public static EntityAuthorizationGrant toEntityAuthorizationGrant(EntityAuthorizationGrantRaw rawValue) {
        var authorRaw = rawValue.getAuthorIdentifier();
        PermissionIdentifier authorId = authorRaw != null
                ? new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue())
                : null;
        var authzRaw = rawValue.getAuthorizedEntityIdentifier();
        PermissionIdentifier authzEntityId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var authingRaw = rawValue.getAuthorizingEntityIdentifier();
        PermissionIdentifier authingEntityId = new PermissionIdentifier(authingRaw.getType().getValue(), authingRaw.getValue());
        String authScope = rawValue.getAuthorizationScope().getValue();
        return new EntityAuthorizationGrant(rawValue.getId(), authorId, authzEntityId, authingEntityId,
                authScope, rawValue.getDescription(), rawValue.getStartDate());
    }

    public static EntityAuthorizationPermissions toEntityAuthorizationPermissions(QueryEntityAuthorizationPermissionsResponseRaw rawValue) {
        List<EntityAuthorizationGrant> mapped = rawValue.getAuthorizationGrants().stream().map(PermissionsMappers::toEntityAuthorizationGrant).toList();
        return new EntityAuthorizationPermissions(mapped, rawValue.getHasMore());
    }

    public static EntityPermission toEntityPermission(EntityPermissionItemRaw rawValue) {
        var ctxRaw = rawValue.getContextIdentifier();
        PermissionIdentifier ctxId = new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue());
        String scope = rawValue.getPermissionScope().getValue();
        return new EntityPermission(rawValue.getId(), ctxId, scope, rawValue.getDescription(),
                rawValue.getStartDate(), rawValue.getCanDelegate());
    }

    public static EntityPermissions toEntityPermissions(QueryEntityPermissionsResponseRaw rawValue) {
        List<EntityPermission> mapped = rawValue.getPermissions().stream().map(PermissionsMappers::toEntityPermission).toList();
        return new EntityPermissions(mapped, rawValue.getHasMore());
    }

    public static EntityRole toEntityRole(EntityRoleRaw rawValue) {
        var parentRaw = rawValue.getParentEntityIdentifier();
        PermissionIdentifier parentId = parentRaw != null
                ? new PermissionIdentifier(parentRaw.getType().getValue(), parentRaw.getValue())
                : null;
        String role = rawValue.getRole().getValue();
        return new EntityRole(parentId, role, rawValue.getDescription(), rawValue.getStartDate());
    }

    public static EntityRoles toEntityRoles(QueryEntityRolesResponseRaw rawValue) {
        List<EntityRole> mapped = rawValue.getRoles().stream().map(PermissionsMappers::toEntityRole).toList();
        return new EntityRoles(mapped, rawValue.getHasMore());
    }

    public static EuEntityPermission toEuEntityPermission(EuEntityPermissionRaw rawValue) {
        var authorRaw = rawValue.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = rawValue.getPermissionScope().getValue();
        return new EuEntityPermission(rawValue.getId(), authorId, rawValue.getVatUeIdentifier(),
                rawValue.getEuEntityName(), rawValue.getAuthorizedFingerprintIdentifier(),
                scope, rawValue.getDescription(), rawValue.getStartDate());
    }

    public static EuEntityPermissions toEuEntityPermissions(QueryEuEntityPermissionsResponseRaw rawValue) {
        List<EuEntityPermission> mapped = rawValue.getPermissions().stream().map(PermissionsMappers::toEuEntityPermission).toList();
        return new EuEntityPermissions(mapped, rawValue.getHasMore());
    }

    public static PermissionOperationResult toPermissionOperationResult(PermissionsOperationResponseRaw rawValue) {
        return new PermissionOperationResult(rawValue.getReferenceNumber());
    }

    public static PermissionOperationStatus toPermissionOperationStatus(String referenceNumber,
                                                                        PermissionsOperationStatusResponseRaw rawValue) {
        return new PermissionOperationStatus(referenceNumber, CommonMappers.toStatusInfo(rawValue.getStatus()));
    }

    public static PersonalPermission toPersonalPermission(PersonalPermissionRaw rawValue) {
        var ctxRaw = rawValue.getContextIdentifier();
        PermissionIdentifier ctxId = ctxRaw != null
                ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                : null;
        var authzRaw = rawValue.getAuthorizedIdentifier();
        PermissionIdentifier authzId = authzRaw != null
                ? new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue())
                : null;
        var targetRaw = rawValue.getTargetIdentifier();
        PermissionIdentifier targetId = targetRaw != null
                ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                : null;
        PermissionSubjectDetails personDetails = rawValue.getSubjectPersonDetails() != null
                ? new PermissionSubjectDetails(
                        rawValue.getSubjectPersonDetails().getFirstName(),
                        rawValue.getSubjectPersonDetails().getLastName(), null)
                : null;
        PermissionSubjectDetails entityDetails = rawValue.getSubjectEntityDetails() != null
                ? new PermissionSubjectDetails(null, null,
                        rawValue.getSubjectEntityDetails().getFullName())
                : null;
        String scope = rawValue.getPermissionScope().getValue();
        String state = rawValue.getPermissionState().getValue();
        return new PersonalPermission(rawValue.getId(), ctxId, authzId, targetId, scope,
                rawValue.getDescription(), personDetails, entityDetails, state,
                rawValue.getStartDate(), rawValue.getCanDelegate());
    }

    public static PersonalPermissions toPersonalPermissions(QueryPersonalPermissionsResponseRaw rawValue) {
        List<PersonalPermission> mapped = rawValue.getPermissions().stream().map(PermissionsMappers::toPersonalPermission).toList();
        return new PersonalPermissions(mapped, rawValue.getHasMore());
    }

    public static PersonPermission toPersonPermission(PersonPermissionRaw rawValue) {
        var authzRaw = rawValue.getAuthorizedIdentifier();
        PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var ctxRaw = rawValue.getContextIdentifier();
        PermissionIdentifier ctxId = ctxRaw != null
                ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                : null;
        var targetRaw = rawValue.getTargetIdentifier();
        PermissionIdentifier targetId = targetRaw != null
                ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                : null;
        var authorRaw = rawValue.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = rawValue.getPermissionScope().getValue();
        String state = rawValue.getPermissionState().getValue();
        return new PersonPermission(rawValue.getId(), authzId, ctxId, targetId, authorId,
                scope, rawValue.getDescription(), state, rawValue.getStartDate(), rawValue.getCanDelegate());
    }

    public static PersonPermissions toPersonPermissions(QueryPersonPermissionsResponseRaw rawValue) {
        List<PersonPermission> mapped = rawValue.getPermissions().stream().map(PermissionsMappers::toPersonPermission).toList();
        return new PersonPermissions(mapped, rawValue.getHasMore());
    }

    public static SubordinateEntityRole toSubordinateEntityRole(SubordinateEntityRoleRaw rawValue) {
        var subRaw = rawValue.getSubordinateEntityIdentifier();
        PermissionIdentifier subId = new PermissionIdentifier(subRaw.getType().getValue(), subRaw.getValue());
        String role = rawValue.getRole().getValue();
        return new SubordinateEntityRole(subId, role, rawValue.getDescription(), rawValue.getStartDate());
    }

    public static SubordinateEntityRoles toSubordinateEntityRoles(QuerySubordinateEntityRolesResponseRaw rawValue) {
        List<SubordinateEntityRole> mapped = rawValue.getRoles().stream().map(PermissionsMappers::toSubordinateEntityRole).toList();
        return new SubordinateEntityRoles(mapped, rawValue.getHasMore());
    }

    public static SubunitPermission toSubunitPermission(SubunitPermissionRaw rawValue) {
        var authzRaw = rawValue.getAuthorizedIdentifier();
        PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var subunitRaw = rawValue.getSubunitIdentifier();
        PermissionIdentifier subunitId = new PermissionIdentifier(subunitRaw.getType().getValue(), subunitRaw.getValue());
        var authorRaw = rawValue.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = rawValue.getPermissionScope().getValue();
        return new SubunitPermission(rawValue.getId(), authzId, subunitId, authorId,
                scope, rawValue.getDescription(), rawValue.getSubunitName(), rawValue.getStartDate());
    }

    public static SubunitPermissions toSubunitPermissions(QuerySubunitPermissionsResponseRaw rawValue) {
        List<SubunitPermission> mapped = rawValue.getPermissions().stream().map(PermissionsMappers::toSubunitPermission).toList();
        return new SubunitPermissions(mapped, rawValue.getHasMore());
    }

}
