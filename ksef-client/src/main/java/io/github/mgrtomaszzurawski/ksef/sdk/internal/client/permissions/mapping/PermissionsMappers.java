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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationResult;
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
 */
public final class PermissionsMappers {

    private PermissionsMappers() { }

    public static AttachmentPermissionStatus toAttachmentPermissionStatus(CheckAttachmentPermissionStatusResponseRaw raw) {
            return new AttachmentPermissionStatus(
                    raw.getIsAttachmentAllowed(),
                    raw.getRevokedDate());
        
    }

    public static EntityAuthorizationGrant toEntityAuthorizationGrant(EntityAuthorizationGrantRaw raw) {
            var authorRaw = raw.getAuthorIdentifier();
            PermissionIdentifier authorId = authorRaw != null
                    ? new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue())
                    : null;
            var authzRaw = raw.getAuthorizedEntityIdentifier();
            PermissionIdentifier authzEntityId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
            var authingRaw = raw.getAuthorizingEntityIdentifier();
            PermissionIdentifier authingEntityId = new PermissionIdentifier(authingRaw.getType().getValue(), authingRaw.getValue());
            String authScope = raw.getAuthorizationScope().getValue();
            return new EntityAuthorizationGrant(raw.getId(), authorId, authzEntityId, authingEntityId,
                    authScope, raw.getDescription(), raw.getStartDate());
        
    }

    public static EntityAuthorizationPermissions toEntityAuthorizationPermissions(QueryEntityAuthorizationPermissionsResponseRaw raw) {
            List<EntityAuthorizationGrant> mapped = raw.getAuthorizationGrants().stream().map(PermissionsMappers::toEntityAuthorizationGrant).toList();
            return new EntityAuthorizationPermissions(mapped, raw.getHasMore());
        
    }

    public static EntityPermission toEntityPermission(EntityPermissionItemRaw raw) {
            var ctxRaw = raw.getContextIdentifier();
            PermissionIdentifier ctxId = new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue());
            String scope = raw.getPermissionScope().getValue();
            return new EntityPermission(raw.getId(), ctxId, scope, raw.getDescription(),
                    raw.getStartDate(), raw.getCanDelegate());
        
    }

    public static EntityPermissions toEntityPermissions(QueryEntityPermissionsResponseRaw raw) {
            List<EntityPermission> mapped = raw.getPermissions().stream().map(PermissionsMappers::toEntityPermission).toList();
            return new EntityPermissions(mapped, raw.getHasMore());
        
    }

    public static EntityRole toEntityRole(EntityRoleRaw raw) {
            var parentRaw = raw.getParentEntityIdentifier();
            PermissionIdentifier parentId = parentRaw != null
                    ? new PermissionIdentifier(parentRaw.getType().getValue(), parentRaw.getValue())
                    : null;
            String role = raw.getRole().getValue();
            return new EntityRole(parentId, role, raw.getDescription(), raw.getStartDate());
        
    }

    public static EntityRoles toEntityRoles(QueryEntityRolesResponseRaw raw) {
            List<EntityRole> mapped = raw.getRoles().stream().map(PermissionsMappers::toEntityRole).toList();
            return new EntityRoles(mapped, raw.getHasMore());
        
    }

    public static EuEntityPermission toEuEntityPermission(EuEntityPermissionRaw raw) {
            var authorRaw = raw.getAuthorIdentifier();
            PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
            String scope = raw.getPermissionScope().getValue();
            return new EuEntityPermission(raw.getId(), authorId, raw.getVatUeIdentifier(),
                    raw.getEuEntityName(), raw.getAuthorizedFingerprintIdentifier(),
                    scope, raw.getDescription(), raw.getStartDate());
        
    }

    public static EuEntityPermissions toEuEntityPermissions(QueryEuEntityPermissionsResponseRaw raw) {
            List<EuEntityPermission> mapped = raw.getPermissions().stream().map(PermissionsMappers::toEuEntityPermission).toList();
            return new EuEntityPermissions(mapped, raw.getHasMore());
        
    }

    public static PermissionOperationResult toPermissionOperationResult(PermissionsOperationResponseRaw raw) {
            return new PermissionOperationResult(raw.getReferenceNumber());
        
    }

    public static PermissionOperationStatus toPermissionOperationStatus(PermissionsOperationStatusResponseRaw raw) {
            return new PermissionOperationStatus(CommonMappers.toStatusInfo(raw.getStatus()));
        
    }

    public static PersonalPermission toPersonalPermission(PersonalPermissionRaw raw) {
            var ctxRaw = raw.getContextIdentifier();
            PermissionIdentifier ctxId = ctxRaw != null
                    ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                    : null;
            var authzRaw = raw.getAuthorizedIdentifier();
            PermissionIdentifier authzId = authzRaw != null
                    ? new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue())
                    : null;
            var targetRaw = raw.getTargetIdentifier();
            PermissionIdentifier targetId = targetRaw != null
                    ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                    : null;
            PermissionSubjectDetails personDetails = raw.getSubjectPersonDetails() != null
                    ? new PermissionSubjectDetails(
                            raw.getSubjectPersonDetails().getFirstName(),
                            raw.getSubjectPersonDetails().getLastName(), null)
                    : null;
            PermissionSubjectDetails entityDetails = raw.getSubjectEntityDetails() != null
                    ? new PermissionSubjectDetails(null, null,
                            raw.getSubjectEntityDetails().getFullName())
                    : null;
            String scope = raw.getPermissionScope().getValue();
            String state = raw.getPermissionState().getValue();
            return new PersonalPermission(raw.getId(), ctxId, authzId, targetId, scope,
                    raw.getDescription(), personDetails, entityDetails, state,
                    raw.getStartDate(), raw.getCanDelegate());
        
    }

    public static PersonalPermissions toPersonalPermissions(QueryPersonalPermissionsResponseRaw raw) {
            List<PersonalPermission> mapped = raw.getPermissions().stream().map(PermissionsMappers::toPersonalPermission).toList();
            return new PersonalPermissions(mapped, raw.getHasMore());
        
    }

    public static PersonPermission toPersonPermission(PersonPermissionRaw raw) {
            var authzRaw = raw.getAuthorizedIdentifier();
            PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
            var ctxRaw = raw.getContextIdentifier();
            PermissionIdentifier ctxId = ctxRaw != null
                    ? new PermissionIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue())
                    : null;
            var targetRaw = raw.getTargetIdentifier();
            PermissionIdentifier targetId = targetRaw != null
                    ? new PermissionIdentifier(targetRaw.getType().getValue(), targetRaw.getValue())
                    : null;
            var authorRaw = raw.getAuthorIdentifier();
            PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
            String scope = raw.getPermissionScope().getValue();
            String state = raw.getPermissionState().getValue();
            return new PersonPermission(raw.getId(), authzId, ctxId, targetId, authorId,
                    scope, raw.getDescription(), state, raw.getStartDate(), raw.getCanDelegate());
        
    }

    public static PersonPermissions toPersonPermissions(QueryPersonPermissionsResponseRaw raw) {
            List<PersonPermission> mapped = raw.getPermissions().stream().map(PermissionsMappers::toPersonPermission).toList();
            return new PersonPermissions(mapped, raw.getHasMore());
        
    }

    public static SubordinateEntityRole toSubordinateEntityRole(SubordinateEntityRoleRaw raw) {
            var subRaw = raw.getSubordinateEntityIdentifier();
            PermissionIdentifier subId = new PermissionIdentifier(subRaw.getType().getValue(), subRaw.getValue());
            String role = raw.getRole().getValue();
            return new SubordinateEntityRole(subId, role, raw.getDescription(), raw.getStartDate());
        
    }

    public static SubordinateEntityRoles toSubordinateEntityRoles(QuerySubordinateEntityRolesResponseRaw raw) {
            List<SubordinateEntityRole> mapped = raw.getRoles().stream().map(PermissionsMappers::toSubordinateEntityRole).toList();
            return new SubordinateEntityRoles(mapped, raw.getHasMore());
        
    }

    public static SubunitPermission toSubunitPermission(SubunitPermissionRaw raw) {
            var authzRaw = raw.getAuthorizedIdentifier();
            PermissionIdentifier authzId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
            var subunitRaw = raw.getSubunitIdentifier();
            PermissionIdentifier subunitId = new PermissionIdentifier(subunitRaw.getType().getValue(), subunitRaw.getValue());
            var authorRaw = raw.getAuthorIdentifier();
            PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
            String scope = raw.getPermissionScope().getValue();
            return new SubunitPermission(raw.getId(), authzId, subunitId, authorId,
                    scope, raw.getDescription(), raw.getSubunitName(), raw.getStartDate());
        
    }

    public static SubunitPermissions toSubunitPermissions(QuerySubunitPermissionsResponseRaw raw) {
            List<SubunitPermission> mapped = raw.getPermissions().stream().map(PermissionsMappers::toSubunitPermission).toList();
            return new SubunitPermissions(mapped, raw.getHasMore());
        
    }

}
