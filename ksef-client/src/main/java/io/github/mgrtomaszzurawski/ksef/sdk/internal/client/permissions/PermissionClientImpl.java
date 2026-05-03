/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.PermissionClient;
import io.github.mgrtomaszzurawski.ksef.client.model.CheckAttachmentPermissionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityAuthorizationPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryEuEntityPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPersonalPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubordinateEntityRolesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QuerySubunitPermissionsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRolesQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionsMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionsQueryRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionsRequestMappers;

/**
 * Client for KSeF permission management — granting, revoking, and querying permissions
 * for persons, entities, EU entities, subunits, and authorizations.
 */
public final class PermissionClientImpl implements PermissionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionClientImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_GRANT_PERSON = ApiPaths.PERMISSIONS + "/persons/grants";
    private static final String PATH_GRANT_ENTITY = ApiPaths.PERMISSIONS + "/entities/grants";
    private static final String PATH_GRANT_AUTHORIZATION = ApiPaths.PERMISSIONS + "/authorizations/grants";
    private static final String PATH_GRANT_INDIRECT = ApiPaths.PERMISSIONS + "/indirect/grants";
    private static final String PATH_GRANT_SUBUNIT = ApiPaths.PERMISSIONS + "/subunits/grants";
    private static final String PATH_GRANT_EU_ENTITY_ADMIN = ApiPaths.PERMISSIONS + "/eu-entities/administration/grants";
    private static final String PATH_GRANT_EU_ENTITY = ApiPaths.PERMISSIONS + "/eu-entities/grants";

    private static final String PATH_REVOKE_COMMON = ApiPaths.PERMISSIONS + "/common/grants/";
    private static final String PATH_REVOKE_AUTHORIZATION = ApiPaths.PERMISSIONS + "/authorizations/grants/";

    private static final String PATH_OPERATION_STATUS = ApiPaths.PERMISSIONS + "/operations/";
    private static final String PATH_ATTACHMENT_STATUS = ApiPaths.PERMISSIONS + "/attachments/status";

    private static final String PATH_QUERY_PERSONAL = ApiPaths.PERMISSIONS + "/query/personal/grants";
    private static final String PATH_QUERY_PERSONS = ApiPaths.PERMISSIONS + "/query/persons/grants";
    private static final String PATH_QUERY_SUBUNITS = ApiPaths.PERMISSIONS + "/query/subunits/grants";
    private static final String PATH_QUERY_ENTITIES = ApiPaths.PERMISSIONS + "/query/entities/grants";
    private static final String PATH_QUERY_ENTITY_ROLES = ApiPaths.PERMISSIONS + "/query/entities/roles";
    private static final String PATH_QUERY_SUBORDINATE = ApiPaths.PERMISSIONS + "/query/subordinate-entities/roles";
    private static final String PATH_QUERY_AUTHORIZATIONS = ApiPaths.PERMISSIONS + "/query/authorizations/grants";
    private static final String PATH_QUERY_EU_ENTITIES = ApiPaths.PERMISSIONS + "/query/eu-entities/grants";

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

    private static final String ERR_BUILDER_NULL = "builder must not be null";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public PermissionClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef.runtime());
        this.sessionContext = ksef.runtime().sessionContext();
    }

    /**
     * Grant permissions to a person (identified by PESEL or NIP).
     *
     * @param builder grant builder with subject identifier, permissions, and description
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantPerson(PersonPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_PERSON);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_PERSON,
                PermissionsRequestMappers.toPersonPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_PERSON);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant permissions to an entity (identified by NIP).
     *
     * @param builder grant builder with subject identifier, permissions, and description
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantEntity(EntityPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_ENTITY);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_ENTITY,
                PermissionsRequestMappers.toEntityPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_ENTITY);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant authorization permissions (delegate authority to act on behalf of an entity).
     *
     * @param builder grant builder with authorization details
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantAuthorization(EntityAuthorizationPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_AUTHORIZATION);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_AUTHORIZATION,
                PermissionsRequestMappers.toEntityAuthorizationPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_AUTHORIZATION);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant indirect permissions (through an intermediary entity).
     *
     * @param builder grant builder with indirect permission details
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantIndirect(IndirectPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_INDIRECT);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_INDIRECT,
                PermissionsRequestMappers.toIndirectPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_INDIRECT);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant permissions to a subunit (organizational unit within an entity).
     *
     * @param builder grant builder with subunit and permission details
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantSubunit(SubunitPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_SUBUNIT);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_SUBUNIT,
                PermissionsRequestMappers.toSubunitPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_SUBUNIT);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant EU entity administration permissions (register and manage EU entities).
     *
     * @param builder grant builder with EU entity admin details
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantEuEntityAdmin(EuEntityAdminPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_EU_ENTITY_ADMIN);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_EU_ENTITY_ADMIN,
                PermissionsRequestMappers.toEuEntityAdministrationPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_EU_ENTITY_ADMIN);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Grant permissions to an EU entity.
     *
     * @param builder grant builder with EU entity permission details
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult grantEuEntity(EuEntityPermissionGrantBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_GRANT_EU_ENTITY);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(PATH_GRANT_EU_ENTITY,
                PermissionsRequestMappers.toEuEntityPermissionsGrantRequestRaw(builder.build()), token,
                PermissionsOperationResponseRaw.class, OP_GRANT_EU_ENTITY);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Revoke a common permission by permission ID.
     *
     * @param permissionId the permission identifier to revoke
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult revokeCommon(String permissionId) {
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE_COMMON, permissionId);
        requireSafePathSegment(permissionId);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.deleteAuthenticatedWithResponse(PATH_REVOKE_COMMON + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_COMMON);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Revoke an authorization permission by permission ID.
     *
     * @param permissionId the authorization permission identifier to revoke
     * @return operation response with reference number
     */
    @Override
    public PermissionOperationResult revokeAuthorization(String permissionId) {
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE_AUTHORIZATION, permissionId);
        requireSafePathSegment(permissionId);
        String token = sessionContext.token();
        PermissionsOperationResponseRaw rawValue = http.deleteAuthenticatedWithResponse(PATH_REVOKE_AUTHORIZATION + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_AUTHORIZATION);
        return PermissionsMappers.toPermissionOperationResult(rawValue);
    }

    /**
     * Get the status of a permissions operation.
     *
     * @param referenceNumber the operation reference number
     * @return operation status
     */
    @Override
    public PermissionOperationStatus getOperationStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_OPERATION_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        PermissionsOperationStatusResponseRaw rawValue = http.getAuthenticated(PATH_OPERATION_STATUS + referenceNumber, token,
                PermissionsOperationStatusResponseRaw.class, OP_GET_OPERATION_STATUS);
        return PermissionsMappers.toPermissionOperationStatus(rawValue);
    }

    /**
     * Get the status of attachment permissions for the current context.
     *
     * @return attachment permission status
     */
    @Override
    public AttachmentPermissionStatus getAttachmentStatus() {
        LOGGER.debug(LOG_CALL, OP_GET_ATTACHMENT_STATUS);
        String token = sessionContext.token();
        CheckAttachmentPermissionStatusResponseRaw rawValue = http.getAuthenticated(PATH_ATTACHMENT_STATUS, token,
                CheckAttachmentPermissionStatusResponseRaw.class, OP_GET_ATTACHMENT_STATUS);
        return PermissionsMappers.toAttachmentPermissionStatus(rawValue);
    }

    /**
     * Query personal permissions (permissions granted to the authenticated user).
     *
     * @param builder query builder with optional filters
     * @return personal permissions
     */
    @Override
    public PersonalPermissions queryPersonal(PersonalPermissionsQueryBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_QUERY_PERSONAL);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        QueryPersonalPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_PERSONAL,
                PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(builder.build()), token,
                QueryPersonalPermissionsResponseRaw.class, OP_QUERY_PERSONAL);
        return PermissionsMappers.toPersonalPermissions(rawValue);
    }

    /**
     * Query permissions granted to persons for the current context.
     *
     * @param builder query builder with filters (queryType is required)
     * @return person permissions
     */
    @Override
    public PersonPermissions queryPersons(PersonPermissionsQueryBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_QUERY_PERSONS);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        QueryPersonPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_PERSONS,
                PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(builder.build()), token,
                QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
        return PermissionsMappers.toPersonPermissions(rawValue);
    }

    /**
     * Query permissions granted to subunits (sends empty request body).
     *
     * @return subunit permissions
     */
    @Override
    public SubunitPermissions querySubunits() {
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBUNITS);
        String token = sessionContext.token();
        QuerySubunitPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_SUBUNITS,
                new SubunitPermissionsQueryRequestRaw(), token,
                QuerySubunitPermissionsResponseRaw.class, OP_QUERY_SUBUNITS);
        return PermissionsMappers.toSubunitPermissions(rawValue);
    }

    /**
     * Query permissions granted to entities (sends empty request body).
     *
     * @return entity permissions
     */
    @Override
    public EntityPermissions queryEntities() {
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITIES);
        String token = sessionContext.token();
        QueryEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_ENTITIES,
                new EntityPermissionsQueryRequestRaw(), token,
                QueryEntityPermissionsResponseRaw.class, OP_QUERY_ENTITIES);
        return PermissionsMappers.toEntityPermissions(rawValue);
    }

    /**
     * Query entity roles for the current context.
     *
     * @return entity roles
     */
    @Override
    public EntityRoles queryEntityRoles() {
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITY_ROLES);
        String token = sessionContext.token();
        QueryEntityRolesResponseRaw rawValue = http.getAuthenticated(PATH_QUERY_ENTITY_ROLES, token,
                QueryEntityRolesResponseRaw.class, OP_QUERY_ENTITY_ROLES);
        return PermissionsMappers.toEntityRoles(rawValue);
    }

    /**
     * Query roles of subordinate entities (sends empty request body).
     *
     * @return subordinate entity roles
     */
    @Override
    public SubordinateEntityRoles querySubordinateRoles() {
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBORDINATE);
        String token = sessionContext.token();
        QuerySubordinateEntityRolesResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_SUBORDINATE,
                new SubordinateEntityRolesQueryRequestRaw(), token,
                QuerySubordinateEntityRolesResponseRaw.class, OP_QUERY_SUBORDINATE);
        return PermissionsMappers.toSubordinateEntityRoles(rawValue);
    }

    /**
     * Query authorization permissions.
     *
     * @param builder query builder with filters (queryType is required)
     * @return authorization permissions
     */
    @Override
    public EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_QUERY_AUTHORIZATIONS);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        QueryEntityAuthorizationPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_AUTHORIZATIONS,
                PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(builder.build()), token,
                QueryEntityAuthorizationPermissionsResponseRaw.class, OP_QUERY_AUTHORIZATIONS);
        return PermissionsMappers.toEntityAuthorizationPermissions(rawValue);
    }

    /**
     * Query permissions granted to EU entities.
     *
     * @param builder query builder with optional filters
     * @return EU entity permissions
     */
    @Override
    public EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryBuilder builder) {
        LOGGER.debug(LOG_CALL, OP_QUERY_EU_ENTITIES);
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        String token = sessionContext.token();
        QueryEuEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_EU_ENTITIES,
                PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(builder.build()), token,
                QueryEuEntityPermissionsResponseRaw.class, OP_QUERY_EU_ENTITIES);
        return PermissionsMappers.toEuEntityPermissions(rawValue);
    }
}
