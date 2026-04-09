/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.CheckAttachmentPermissionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityAuthorizationPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEuEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonalPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubordinateEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubunitPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRolesQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SubunitPermissions;

import static io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF permission management — granting, revoking, and querying permissions
 * for persons, entities, EU entities, subunits, and authorizations.
 */
public final class PermissionClient {

    // --- Grant paths ---
    private static final String PATH_GRANT_PERSON = "/api/v2/permissions/persons/grants";
    private static final String PATH_GRANT_ENTITY = "/api/v2/permissions/entities/grants";
    private static final String PATH_GRANT_AUTHORIZATION = "/api/v2/permissions/authorizations/grants";
    private static final String PATH_GRANT_INDIRECT = "/api/v2/permissions/indirect/grants";
    private static final String PATH_GRANT_SUBUNIT = "/api/v2/permissions/subunits/grants";
    private static final String PATH_GRANT_EU_ENTITY_ADMIN = "/api/v2/permissions/eu-entities/administration/grants";
    private static final String PATH_GRANT_EU_ENTITY = "/api/v2/permissions/eu-entities/grants";

    // --- Revoke paths ---
    private static final String PATH_REVOKE_COMMON = "/api/v2/permissions/common/grants/";
    private static final String PATH_REVOKE_AUTHORIZATION = "/api/v2/permissions/authorizations/grants/";

    // --- Status paths ---
    private static final String PATH_OPERATION_STATUS = "/api/v2/permissions/operations/";
    private static final String PATH_ATTACHMENT_STATUS = "/api/v2/permissions/attachments/status";

    // --- Query paths ---
    private static final String PATH_QUERY_PERSONAL = "/api/v2/permissions/query/personal/grants";
    private static final String PATH_QUERY_PERSONS = "/api/v2/permissions/query/persons/grants";
    private static final String PATH_QUERY_SUBUNITS = "/api/v2/permissions/query/subunits/grants";
    private static final String PATH_QUERY_ENTITIES = "/api/v2/permissions/query/entities/grants";
    private static final String PATH_QUERY_ENTITY_ROLES = "/api/v2/permissions/query/entities/roles";
    private static final String PATH_QUERY_SUBORDINATE = "/api/v2/permissions/query/subordinate-entities/roles";
    private static final String PATH_QUERY_AUTHORIZATIONS = "/api/v2/permissions/query/authorizations/grants";
    private static final String PATH_QUERY_EU_ENTITIES = "/api/v2/permissions/query/eu-entities/grants";

    // --- Operation names ---
    private static final String OP_GRANT_PERSON = "grantPersonPermissions";
    private static final String OP_GRANT_ENTITY = "grantEntityPermissions";
    private static final String OP_GRANT_AUTHORIZATION = "grantAuthorizationPermissions";
    private static final String OP_GRANT_INDIRECT = "grantIndirectPermissions";
    private static final String OP_GRANT_SUBUNIT = "grantSubunitPermissions";
    private static final String OP_GRANT_EU_ENTITY_ADMIN = "grantEuEntityAdminPermissions";
    private static final String OP_GRANT_EU_ENTITY = "grantEuEntityPermissions";
    private static final String OP_REVOKE_COMMON = "revokeCommonPermission";
    private static final String OP_REVOKE_AUTHORIZATION = "revokeAuthorizationPermission";
    private static final String OP_GET_OPERATION_STATUS = "getPermissionOperationStatus";
    private static final String OP_GET_ATTACHMENT_STATUS = "getAttachmentPermissionStatus";
    private static final String OP_QUERY_PERSONAL = "queryPersonalPermissions";
    private static final String OP_QUERY_PERSONS = "queryPersonPermissions";
    private static final String OP_QUERY_SUBUNITS = "querySubunitPermissions";
    private static final String OP_QUERY_ENTITIES = "queryEntityPermissions";
    private static final String OP_QUERY_ENTITY_ROLES = "queryEntityRoles";
    private static final String OP_QUERY_SUBORDINATE = "querySubordinateEntityRoles";
    private static final String OP_QUERY_AUTHORIZATIONS = "queryAuthorizationPermissions";
    private static final String OP_QUERY_EU_ENTITIES = "queryEuEntityPermissions";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public PermissionClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    // --- Grant operations ---

    /**
     * Grant permissions to a person (identified by PESEL or NIP).
     *
     * @param request grant request with subject identifier, permissions, and description
     * @return operation response with reference number
     */
    public PermissionOperationResult grantPerson(PersonPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_PERSON, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_PERSON);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant permissions to an entity (identified by NIP).
     *
     * @param request grant request with subject identifier, permissions, and description
     * @return operation response with reference number
     */
    public PermissionOperationResult grantEntity(EntityPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_ENTITY, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_ENTITY);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant authorization permissions (delegate authority to act on behalf of an entity).
     *
     * @param request grant request with authorization details
     * @return operation response with reference number
     */
    public PermissionOperationResult grantAuthorization(
            EntityAuthorizationPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_AUTHORIZATION, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_AUTHORIZATION);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant indirect permissions (through an intermediary entity).
     *
     * @param request grant request with indirect permission details
     * @return operation response with reference number
     */
    public PermissionOperationResult grantIndirect(IndirectPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_INDIRECT, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_INDIRECT);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant permissions to a subunit (organizational unit within an entity).
     *
     * @param request grant request with subunit and permission details
     * @return operation response with reference number
     */
    public PermissionOperationResult grantSubunit(SubunitPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_SUBUNIT, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_SUBUNIT);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant EU entity administration permissions (register and manage EU entities).
     *
     * @param request grant request with EU entity admin details
     * @return operation response with reference number
     */
    public PermissionOperationResult grantEuEntityAdmin(
            EuEntityAdministrationPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_EU_ENTITY_ADMIN, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_EU_ENTITY_ADMIN);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Grant permissions to an EU entity.
     *
     * @param request grant request with EU entity permission details
     * @return operation response with reference number
     */
    public PermissionOperationResult grantEuEntity(EuEntityPermissionsGrantRequestRaw request) {
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.postJsonAuthenticated(PATH_GRANT_EU_ENTITY, request, token,
                PermissionsOperationResponseRaw.class, OP_GRANT_EU_ENTITY);
        return PermissionOperationResult.from(raw);
    }

    // --- Revoke operations ---

    /**
     * Revoke a common permission by permission ID.
     *
     * @param permissionId the permission identifier to revoke
     * @return operation response with reference number
     */
    public PermissionOperationResult revokeCommon(String permissionId) {
        requireSafePathSegment(permissionId);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.deleteAuthenticatedWithResponse(PATH_REVOKE_COMMON + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_COMMON);
        return PermissionOperationResult.from(raw);
    }

    /**
     * Revoke an authorization permission by permission ID.
     *
     * @param permissionId the authorization permission identifier to revoke
     * @return operation response with reference number
     */
    public PermissionOperationResult revokeAuthorization(String permissionId) {
        requireSafePathSegment(permissionId);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw raw = http.deleteAuthenticatedWithResponse(PATH_REVOKE_AUTHORIZATION + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_AUTHORIZATION);
        return PermissionOperationResult.from(raw);
    }

    // --- Status operations ---

    /**
     * Get the status of a permissions operation.
     *
     * @param referenceNumber the operation reference number
     * @return operation status
     */
    public PermissionOperationStatus getOperationStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        PermissionsOperationStatusResponseRaw raw = http.getAuthenticated(PATH_OPERATION_STATUS + referenceNumber, token,
                PermissionsOperationStatusResponseRaw.class, OP_GET_OPERATION_STATUS);
        return PermissionOperationStatus.from(raw);
    }

    /**
     * Get the status of attachment permissions for the current context.
     *
     * @return attachment permission status
     */
    public AttachmentPermissionStatus getAttachmentStatus() {
        String token = sessionContext.token();
        CheckAttachmentPermissionStatusResponseRaw raw = http.getAuthenticated(PATH_ATTACHMENT_STATUS, token,
                CheckAttachmentPermissionStatusResponseRaw.class, OP_GET_ATTACHMENT_STATUS);
        return AttachmentPermissionStatus.from(raw);
    }

    // --- Query operations ---

    /**
     * Query personal permissions (permissions granted to the authenticated user).
     *
     * @param request query filters
     * @return personal permissions
     */
    public PersonalPermissions queryPersonal(PersonalPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QueryPersonalPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_PERSONAL, request, token,
                QueryPersonalPermissionsResponseRaw.class, OP_QUERY_PERSONAL);
        return PersonalPermissions.from(raw);
    }

    /**
     * Query permissions granted to persons for the current context.
     *
     * @param request query filters
     * @return person permissions
     */
    public PersonPermissions queryPersons(PersonPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QueryPersonPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_PERSONS, request, token,
                QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
        return PersonPermissions.from(raw);
    }

    /**
     * Query permissions granted to subunits.
     *
     * @param request query filters
     * @return subunit permissions
     */
    public SubunitPermissions querySubunits(SubunitPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QuerySubunitPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_SUBUNITS, request, token,
                QuerySubunitPermissionsResponseRaw.class, OP_QUERY_SUBUNITS);
        return SubunitPermissions.from(raw);
    }

    /**
     * Query permissions granted to entities.
     *
     * @param request query filters
     * @return entity permissions
     */
    public EntityPermissions queryEntities(EntityPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QueryEntityPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_ENTITIES, request, token,
                QueryEntityPermissionsResponseRaw.class, OP_QUERY_ENTITIES);
        return EntityPermissions.from(raw);
    }

    /**
     * Query entity roles for the current context.
     *
     * @return entity roles
     */
    public EntityRoles queryEntityRoles() {
        String token = sessionContext.token();
        QueryEntityRolesResponseRaw raw = http.getAuthenticated(PATH_QUERY_ENTITY_ROLES, token,
                QueryEntityRolesResponseRaw.class, OP_QUERY_ENTITY_ROLES);
        return EntityRoles.from(raw);
    }

    /**
     * Query roles of subordinate entities.
     *
     * @param request query filters
     * @return subordinate entity roles
     */
    public SubordinateEntityRoles querySubordinateRoles(
            SubordinateEntityRolesQueryRequestRaw request) {
        String token = sessionContext.token();
        QuerySubordinateEntityRolesResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_SUBORDINATE, request, token,
                QuerySubordinateEntityRolesResponseRaw.class, OP_QUERY_SUBORDINATE);
        return SubordinateEntityRoles.from(raw);
    }

    /**
     * Query authorization permissions.
     *
     * @param request query filters
     * @return authorization permissions
     */
    public EntityAuthorizationPermissions queryAuthorizations(
            EntityAuthorizationPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QueryEntityAuthorizationPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_AUTHORIZATIONS, request, token,
                QueryEntityAuthorizationPermissionsResponseRaw.class, OP_QUERY_AUTHORIZATIONS);
        return EntityAuthorizationPermissions.from(raw);
    }

    /**
     * Query permissions granted to EU entities.
     *
     * @param request query filters
     * @return EU entity permissions
     */
    public EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryRequestRaw request) {
        String token = sessionContext.token();
        QueryEuEntityPermissionsResponseRaw raw = http.postJsonAuthenticated(PATH_QUERY_EU_ENTITIES, request, token,
                QueryEuEntityPermissionsResponseRaw.class, OP_QUERY_EU_ENTITIES);
        return EuEntityPermissions.from(raw);
    }
}
