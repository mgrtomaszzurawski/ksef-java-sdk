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
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubordinateEntityIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubordinateEntityIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRolesQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubunitIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubunitIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
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
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
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
 *
 * @since 1.0.0
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
    private static final String ERR_NULL_FILTER = "filter must not be null";

    /** Spec-defined max page size for permission query endpoints. */
    private static final int PERMISSION_QUERY_MAX_PAGE_SIZE = 250;
    private static final String PERMISSION_QUERY_PAGE_PARAMS = "?pageOffset=";
    private static final String PERMISSION_QUERY_PAGE_SIZE_PARAM = "&pageSize=" + PERMISSION_QUERY_MAX_PAGE_SIZE;
    private static final String QUERY_PARAM_SEPARATOR_FIRST = "?";
    private static final String QUERY_PARAM_SEPARATOR = "&";
    private static final String PARAM_PAGE_OFFSET = "pageOffset=";
    private static final String PARAM_PAGE_SIZE = "pageSize=";

    private final HttpSupport http;

    public PermissionClientImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
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
        String token = http.requireToken();
        QueryPersonPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_PERSONS,
                PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(builder.build()), token,
                QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
        return PermissionsMappers.toPersonPermissions(rawValue);
    }

    @Override
    public SubunitPermissions querySubunits(SubunitPermissionsQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBUNITS);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_SUBUNITS, filter.pageOffsetValue(), filter.pageSizeValue());
        QuerySubunitPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildSubunitPermissionsBody(filter), token, QuerySubunitPermissionsResponseRaw.class, OP_QUERY_SUBUNITS);
        return PermissionsMappers.toSubunitPermissions(rawValue);
    }

    @Override
    public EntityPermissions queryEntities(EntityPermissionsQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITIES);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_ENTITIES, filter.pageOffsetValue(), filter.pageSizeValue());
        QueryEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildEntityPermissionsBody(filter), token, QueryEntityPermissionsResponseRaw.class, OP_QUERY_ENTITIES);
        return PermissionsMappers.toEntityPermissions(rawValue);
    }

    @Override
    public EntityRoles queryEntityRoles(EntityRolesQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITY_ROLES);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_ENTITY_ROLES, filter.pageOffsetValue(), filter.pageSizeValue());
        QueryEntityRolesResponseRaw rawValue = http.getAuthenticated(path, token,
                QueryEntityRolesResponseRaw.class, OP_QUERY_ENTITY_ROLES);
        return PermissionsMappers.toEntityRoles(rawValue);
    }

    @Override
    public SubordinateEntityRoles querySubordinateRoles(SubordinateEntityRolesQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBORDINATE);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_SUBORDINATE, filter.pageOffsetValue(), filter.pageSizeValue());
        QuerySubordinateEntityRolesResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildSubordinateRolesBody(filter), token, QuerySubordinateEntityRolesResponseRaw.class, OP_QUERY_SUBORDINATE);
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
        String token = http.requireToken();
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
        String token = http.requireToken();
        QueryEuEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(PATH_QUERY_EU_ENTITIES,
                PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(builder.build()), token,
                QueryEuEntityPermissionsResponseRaw.class, OP_QUERY_EU_ENTITIES);
        return PermissionsMappers.toEuEntityPermissions(rawValue);
    }

    // ======================== stream variants (Codex A.4.1 / F3) ========================
    // Each returns a lazy Stream<T> that walks one page at a time using
    // spec-max page size; callers bound memory via .limit(N) / .takeWhile(...).

    private static String pagedPath(String basePath, int pageOffset) {
        return basePath + PERMISSION_QUERY_PAGE_PARAMS + pageOffset + PERMISSION_QUERY_PAGE_SIZE_PARAM;
    }

    /** Build {@code ?pageOffset=...&pageSize=...} query string fragments only when present. */
    private static String appendPaging(String basePath,
                                       @org.jspecify.annotations.Nullable Integer pageOffset,
                                       @org.jspecify.annotations.Nullable Integer pageSize) {
        if (pageOffset == null && pageSize == null) {
            return basePath;
        }
        StringBuilder query = new StringBuilder(basePath);
        if (pageOffset != null) {
            query.append(QUERY_PARAM_SEPARATOR_FIRST).append(PARAM_PAGE_OFFSET).append(pageOffset);
        }
        if (pageSize != null) {
            query.append(query.indexOf(QUERY_PARAM_SEPARATOR_FIRST) >= 0 ? QUERY_PARAM_SEPARATOR : QUERY_PARAM_SEPARATOR_FIRST)
                    .append(PARAM_PAGE_SIZE).append(pageSize);
        }
        return query.toString();
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            streamPersonal(PersonalPermissionsQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryPersonalPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_PERSONAL, pageOffset),
                    PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(builder.build()),
                    token, QueryPersonalPermissionsResponseRaw.class, OP_QUERY_PERSONAL);
            PersonalPermissions page = PermissionsMappers.toPersonalPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryPersonPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_PERSONS, pageOffset),
                    PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(builder.build()),
                    token, QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
            PersonPermissions page = PermissionsMappers.toPersonPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(SubunitPermissionsQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QuerySubunitPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_SUBUNITS, pageOffset),
                    buildSubunitPermissionsBody(filter), token,
                    QuerySubunitPermissionsResponseRaw.class, OP_QUERY_SUBUNITS);
            SubunitPermissions page = PermissionsMappers.toSubunitPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            streamEntities(EntityPermissionsQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEntityPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_ENTITIES, pageOffset),
                    buildEntityPermissionsBody(filter), token,
                    QueryEntityPermissionsResponseRaw.class, OP_QUERY_ENTITIES);
            EntityPermissions page = PermissionsMappers.toEntityPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(SubordinateEntityRolesQueryBuilder filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QuerySubordinateEntityRolesResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_SUBORDINATE, pageOffset),
                    buildSubordinateRolesBody(filter), token,
                    QuerySubordinateEntityRolesResponseRaw.class, OP_QUERY_SUBORDINATE);
            SubordinateEntityRoles page = PermissionsMappers.toSubordinateEntityRoles(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.roles(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            streamAuthorizations(EntityAuthorizationPermissionsQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEntityAuthorizationPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_AUTHORIZATIONS, pageOffset),
                    PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(builder.build()),
                    token, QueryEntityAuthorizationPermissionsResponseRaw.class, OP_QUERY_AUTHORIZATIONS);
            EntityAuthorizationPermissions page = PermissionsMappers.toEntityAuthorizationPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.authorizationGrants(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryBuilder builder) {
        Objects.requireNonNull(builder, ERR_BUILDER_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEuEntityPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_EU_ENTITIES, pageOffset),
                    PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(builder.build()),
                    token, QueryEuEntityPermissionsResponseRaw.class, OP_QUERY_EU_ENTITIES);
            EuEntityPermissions page = PermissionsMappers.toEuEntityPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    private static SubunitPermissionsQueryRequestRaw buildSubunitPermissionsBody(SubunitPermissionsQueryBuilder filter) {
        SubunitPermissionsQueryRequestRaw body = new SubunitPermissionsQueryRequestRaw();
        if (filter.subunitIdentifierType() != null && filter.subunitIdentifierValue() != null) {
            SubunitPermissionsSubunitIdentifierRaw id = new SubunitPermissionsSubunitIdentifierRaw();
            id.setType(filter.subunitIdentifierType() == SubunitPermissionsQueryBuilder.SubunitIdentifierType.NIP
                    ? SubunitPermissionsSubunitIdentifierTypeRaw.NIP
                    : SubunitPermissionsSubunitIdentifierTypeRaw.INTERNAL_ID);
            id.setValue(filter.subunitIdentifierValue());
            body.subunitIdentifier(id);
        }
        return body;
    }

    private static EntityPermissionsQueryRequestRaw buildEntityPermissionsBody(EntityPermissionsQueryBuilder filter) {
        EntityPermissionsQueryRequestRaw body = new EntityPermissionsQueryRequestRaw();
        if (filter.contextIdentifierType() != null && filter.contextIdentifierValue() != null) {
            EntityPermissionsContextIdentifierRaw id = new EntityPermissionsContextIdentifierRaw();
            id.setType(filter.contextIdentifierType() == EntityPermissionsQueryBuilder.ContextIdentifierType.NIP
                    ? EntityPermissionsContextIdentifierTypeRaw.NIP
                    : EntityPermissionsContextIdentifierTypeRaw.INTERNAL_ID);
            id.setValue(filter.contextIdentifierValue());
            body.contextIdentifier(id);
        }
        return body;
    }

    private static SubordinateEntityRolesQueryRequestRaw buildSubordinateRolesBody(SubordinateEntityRolesQueryBuilder filter) {
        SubordinateEntityRolesQueryRequestRaw body = new SubordinateEntityRolesQueryRequestRaw();
        if (filter.subordinateEntityNipValue() != null) {
            EntityPermissionsSubordinateEntityIdentifierRaw id = new EntityPermissionsSubordinateEntityIdentifierRaw();
            id.setType(EntityPermissionsSubordinateEntityIdentifierTypeRaw.NIP);
            id.setValue(filter.subordinateEntityNipValue());
            body.subordinateEntityIdentifier(id);
        }
        return body;
    }
}
