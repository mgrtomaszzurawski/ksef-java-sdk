/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.Permissions;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.async.KsefAsync;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.async.KsefAsyncStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import java.time.Duration;
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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
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
public final class PermissionsImpl implements Permissions {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsImpl.class);
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

    private static final String ERR_REQUEST_NULL = "request must not be null";
    private static final String ERR_NULL_FILTER = "filter must not be null";
    private static final String ERR_NULL_TIMEOUT = "timeout must not be null";
    private static final String ERR_UNHANDLED_GRANT_SUBTYPE =
            "Unhandled PermissionGrantRequest subtype: ";

    /** Default sync timeout for permission grant/revoke operations (ADR-032). */
    private static final Duration DEFAULT_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    /**
     * KSeF-enforced max page size for permission query endpoints
     * ({@code persons/grants}, {@code entities/grants}, {@code subunits/grants},
     * {@code authorizations/grants}, {@code eu-entities/grants},
     * {@code personal/grants}, {@code entities/roles},
     * {@code subordinate-entities/roles}). The server enforces
     * {@code [10, 100]} on these endpoints and rejects requests outside the
     * bound with 21405 ("'pageSize' must be between 10 and 100"). This is
     * a tighter range than the {@code /invoices/query/metadata} endpoint
     * which accepts up to 250 — different KSeF subsystem, different
     * server-side limit. Verified by live-demo regression 2026-05-19.
     */
    private static final int PERMISSION_QUERY_MAX_PAGE_SIZE = 100;
    private static final String PERMISSION_QUERY_PAGE_PARAMS = "?pageOffset=";
    private static final String PERMISSION_QUERY_PAGE_SIZE_PARAM = "&pageSize=" + PERMISSION_QUERY_MAX_PAGE_SIZE;
    private static final String QUERY_PARAM_SEPARATOR_FIRST = "?";
    private static final String QUERY_PARAM_SEPARATOR = "&";
    private static final String PARAM_PAGE_OFFSET = "pageOffset=";
    private static final String PARAM_PAGE_SIZE = "pageSize=";

    private final HttpSupport http;

    public PermissionsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public PermissionOperationStatus grant(PermissionGrantRequest request) {
        return grant(request, DEFAULT_OPERATION_TIMEOUT);
    }

    @Override
    public PermissionOperationStatus grant(PermissionGrantRequest request, Duration timeout) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        Objects.requireNonNull(timeout, ERR_NULL_TIMEOUT);
        // Java 17: pattern matching in switch is preview, so we use an
        // instanceof ladder. The sealed permits clause on
        // PermissionGrantRequest still guarantees exhaustiveness at the
        // type-system level — adding an 8th permits member without a
        // matching arm here would compile but unreachable IllegalStateException
        // catches the dispatch gap at first call. Migrate to switch
        // pattern when the SDK moves off Java 17.
        if (request instanceof PersonPermissionGrantRequest personGrant) {
            return dispatchGrant(PATH_GRANT_PERSON,
                    PermissionsRequestMappers.toPersonPermissionsGrantRequestRaw(personGrant),
                    OP_GRANT_PERSON, timeout);
        }
        if (request instanceof EntityPermissionGrantRequest entityGrant) {
            return dispatchGrant(PATH_GRANT_ENTITY,
                    PermissionsRequestMappers.toEntityPermissionsGrantRequestRaw(entityGrant),
                    OP_GRANT_ENTITY, timeout);
        }
        if (request instanceof EntityAuthorizationPermissionGrantRequest authorizationGrant) {
            return dispatchGrant(PATH_GRANT_AUTHORIZATION,
                    PermissionsRequestMappers.toEntityAuthorizationPermissionsGrantRequestRaw(authorizationGrant),
                    OP_GRANT_AUTHORIZATION, timeout);
        }
        if (request instanceof IndirectPermissionGrantRequest indirectGrant) {
            return dispatchGrant(PATH_GRANT_INDIRECT,
                    PermissionsRequestMappers.toIndirectPermissionsGrantRequestRaw(indirectGrant),
                    OP_GRANT_INDIRECT, timeout);
        }
        if (request instanceof SubunitPermissionGrantRequest subunitGrant) {
            return dispatchGrant(PATH_GRANT_SUBUNIT,
                    PermissionsRequestMappers.toSubunitPermissionsGrantRequestRaw(subunitGrant),
                    OP_GRANT_SUBUNIT, timeout);
        }
        if (request instanceof EuEntityAdminPermissionGrantRequest euEntityAdminGrant) {
            return dispatchGrant(PATH_GRANT_EU_ENTITY_ADMIN,
                    PermissionsRequestMappers.toEuEntityAdministrationPermissionsGrantRequestRaw(euEntityAdminGrant),
                    OP_GRANT_EU_ENTITY_ADMIN, timeout);
        }
        if (request instanceof EuEntityPermissionGrantRequest euEntityGrant) {
            return dispatchGrant(PATH_GRANT_EU_ENTITY,
                    PermissionsRequestMappers.toEuEntityPermissionsGrantRequestRaw(euEntityGrant),
                    OP_GRANT_EU_ENTITY, timeout);
        }
        throw new IllegalStateException(ERR_UNHANDLED_GRANT_SUBTYPE + request.getClass().getName());
    }

    private PermissionOperationStatus dispatchGrant(String path, Object rawBody, String opName, Duration timeout) {
        LOGGER.debug(LOG_CALL, opName);
        String refNumber = postGrant(path, rawBody, opName);
        return awaitTerminalStatus(refNumber, opName, timeout);
    }

    @Override
    public PermissionOperationStatus revokePermission(String permissionId) {
        return revokePermission(permissionId, DEFAULT_OPERATION_TIMEOUT);
    }

    @Override
    public PermissionOperationStatus revokePermission(String permissionId, Duration timeout) {
        Objects.requireNonNull(timeout, ERR_NULL_TIMEOUT);
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE_COMMON, permissionId);
        requireSafePathSegment(permissionId);
        String token = http.requireToken();
        PermissionsOperationResponseRaw rawValue = http.deleteAuthenticatedWithResponse(PATH_REVOKE_COMMON + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_COMMON);
        PermissionOperationResult intermediate = PermissionsMappers.toPermissionOperationResult(rawValue);
        return awaitTerminalStatus(intermediate.referenceNumber(), OP_REVOKE_COMMON, timeout);
    }

    @Override
    public PermissionOperationStatus revokeAuthorization(String permissionId) {
        return revokeAuthorization(permissionId, DEFAULT_OPERATION_TIMEOUT);
    }

    @Override
    public PermissionOperationStatus revokeAuthorization(String permissionId, Duration timeout) {
        Objects.requireNonNull(timeout, ERR_NULL_TIMEOUT);
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE_AUTHORIZATION, permissionId);
        requireSafePathSegment(permissionId);
        String token = http.requireToken();
        PermissionsOperationResponseRaw rawValue = http.deleteAuthenticatedWithResponse(PATH_REVOKE_AUTHORIZATION + permissionId, token,
                PermissionsOperationResponseRaw.class, OP_REVOKE_AUTHORIZATION);
        PermissionOperationResult intermediate = PermissionsMappers.toPermissionOperationResult(rawValue);
        return awaitTerminalStatus(intermediate.referenceNumber(), OP_REVOKE_AUTHORIZATION, timeout);
    }

    private String postGrant(String path, Object requestBody, String opName) {
        String token = http.requireToken();
        PermissionsOperationResponseRaw rawValue = http.postJsonAuthenticated(path, requestBody, token,
                PermissionsOperationResponseRaw.class, opName);
        return PermissionsMappers.toPermissionOperationResult(rawValue).referenceNumber();
    }

    private PermissionOperationStatus awaitTerminalStatus(String referenceNumber, String opName, Duration timeout) {
        requireSafePathSegment(referenceNumber);
        return KsefAsync.awaitTerminal(new KsefAsync.Config<>(
                opName,
                () -> {
                    String token = http.requireToken();
                    PermissionsOperationStatusResponseRaw raw = http.getAuthenticated(
                            PATH_OPERATION_STATUS + referenceNumber, token,
                            PermissionsOperationStatusResponseRaw.class, OP_GET_OPERATION_STATUS);
                    return PermissionsMappers.toPermissionOperationStatus(referenceNumber, raw);
                },
                status -> status.status() != null
                        && status.status().code() >= KsefAsyncStatus.TERMINAL_STATUS_CODE_THRESHOLD,
                status -> status.status() == null ? null : status.status().code(),
                timeout,
                null));
    }

    @Override
    public AttachmentPermissionStatus getAttachmentStatus() {
        LOGGER.debug(LOG_CALL, OP_GET_ATTACHMENT_STATUS);
        String token = http.requireToken();
        CheckAttachmentPermissionStatusResponseRaw rawValue = http.getAuthenticated(PATH_ATTACHMENT_STATUS, token,
                CheckAttachmentPermissionStatusResponseRaw.class, OP_GET_ATTACHMENT_STATUS);
        return PermissionsMappers.toAttachmentPermissionStatus(rawValue);
    }

    @Override
    public PersonalPermissions queryPersonal(PersonalPermissionsQueryRequest request) {
        LOGGER.debug(LOG_CALL, OP_QUERY_PERSONAL);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_PERSONAL, request.pageOffset(), request.pageSize());
        QueryPersonalPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(request), token,
                QueryPersonalPermissionsResponseRaw.class, OP_QUERY_PERSONAL);
        return PermissionsMappers.toPersonalPermissions(rawValue);
    }

    @Override
    public PersonPermissions queryPersons(PersonPermissionsQueryRequest request) {
        LOGGER.debug(LOG_CALL, OP_QUERY_PERSONS);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_PERSONS, request.pageOffset(), request.pageSize());
        QueryPersonPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(request), token,
                QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
        return PermissionsMappers.toPersonPermissions(rawValue);
    }

    @Override
    public SubunitPermissions querySubunits(SubunitPermissionsQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBUNITS);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_SUBUNITS, filter.pageOffset(), filter.pageSize());
        QuerySubunitPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildSubunitPermissionsBody(filter), token, QuerySubunitPermissionsResponseRaw.class, OP_QUERY_SUBUNITS);
        return PermissionsMappers.toSubunitPermissions(rawValue);
    }

    @Override
    public EntityPermissions queryEntities(EntityPermissionsQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITIES);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_ENTITIES, filter.pageOffset(), filter.pageSize());
        QueryEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildEntityPermissionsBody(filter), token, QueryEntityPermissionsResponseRaw.class, OP_QUERY_ENTITIES);
        return PermissionsMappers.toEntityPermissions(rawValue);
    }

    @Override
    public EntityRoles queryEntityRoles(EntityRolesQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_ENTITY_ROLES);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_ENTITY_ROLES, filter.pageOffset(), filter.pageSize());
        QueryEntityRolesResponseRaw rawValue = http.getAuthenticated(path, token,
                QueryEntityRolesResponseRaw.class, OP_QUERY_ENTITY_ROLES);
        return PermissionsMappers.toEntityRoles(rawValue);
    }

    @Override
    public SubordinateEntityRoles querySubordinateRoles(SubordinateEntityRolesQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_QUERY_SUBORDINATE);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_SUBORDINATE, filter.pageOffset(), filter.pageSize());
        QuerySubordinateEntityRolesResponseRaw rawValue = http.postJsonAuthenticated(path,
                buildSubordinateRolesBody(filter), token, QuerySubordinateEntityRolesResponseRaw.class, OP_QUERY_SUBORDINATE);
        return PermissionsMappers.toSubordinateEntityRoles(rawValue);
    }

    @Override
    public EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryRequest request) {
        LOGGER.debug(LOG_CALL, OP_QUERY_AUTHORIZATIONS);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_AUTHORIZATIONS, request.pageOffset(), request.pageSize());
        QueryEntityAuthorizationPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(request), token,
                QueryEntityAuthorizationPermissionsResponseRaw.class, OP_QUERY_AUTHORIZATIONS);
        return PermissionsMappers.toEntityAuthorizationPermissions(rawValue);
    }

    @Override
    public EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryRequest request) {
        LOGGER.debug(LOG_CALL, OP_QUERY_EU_ENTITIES);
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        String token = http.requireToken();
        String path = appendPaging(PATH_QUERY_EU_ENTITIES, request.pageOffset(), request.pageSize());
        QueryEuEntityPermissionsResponseRaw rawValue = http.postJsonAuthenticated(path,
                PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(request), token,
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
            streamPersonal(PersonalPermissionsQueryRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryPersonalPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_PERSONAL, pageOffset),
                    PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(request),
                    token, QueryPersonalPermissionsResponseRaw.class, OP_QUERY_PERSONAL);
            PersonalPermissions page = PermissionsMappers.toPersonalPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryPersonPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_PERSONS, pageOffset),
                    PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(request),
                    token, QueryPersonPermissionsResponseRaw.class, OP_QUERY_PERSONS);
            PersonPermissions page = PermissionsMappers.toPersonPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(SubunitPermissionsQueryRequest filter) {
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
            streamEntities(EntityPermissionsQueryRequest filter) {
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
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole>
            streamEntityRoles(EntityRolesQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEntityRolesResponseRaw raw = http.getAuthenticated(
                    pagedPath(PATH_QUERY_ENTITY_ROLES, pageOffset),
                    token, QueryEntityRolesResponseRaw.class, OP_QUERY_ENTITY_ROLES);
            EntityRoles page = PermissionsMappers.toEntityRoles(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.roles(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(SubordinateEntityRolesQueryRequest filter) {
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
            streamAuthorizations(EntityAuthorizationPermissionsQueryRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEntityAuthorizationPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_AUTHORIZATIONS, pageOffset),
                    PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(request),
                    token, QueryEntityAuthorizationPermissionsResponseRaw.class, OP_QUERY_AUTHORIZATIONS);
            EntityAuthorizationPermissions page = PermissionsMappers.toEntityAuthorizationPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.authorizationGrants(), page.hasMore());
        });
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryRequest request) {
        Objects.requireNonNull(request, ERR_REQUEST_NULL);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.stream(pageOffset -> {
            String token = http.requireToken();
            QueryEuEntityPermissionsResponseRaw raw = http.postJsonAuthenticated(
                    pagedPath(PATH_QUERY_EU_ENTITIES, pageOffset),
                    PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(request),
                    token, QueryEuEntityPermissionsResponseRaw.class, OP_QUERY_EU_ENTITIES);
            EuEntityPermissions page = PermissionsMappers.toEuEntityPermissions(raw);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.Page<>(
                    page.permissions(), page.hasMore());
        });
    }

    private static SubunitPermissionsQueryRequestRaw buildSubunitPermissionsBody(SubunitPermissionsQueryRequest filter) {
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

    private static EntityPermissionsQueryRequestRaw buildEntityPermissionsBody(EntityPermissionsQueryRequest filter) {
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

    private static SubordinateEntityRolesQueryRequestRaw buildSubordinateRolesBody(SubordinateEntityRolesQueryRequest filter) {
        SubordinateEntityRolesQueryRequestRaw body = new SubordinateEntityRolesQueryRequestRaw();
        if (filter.subordinateEntityNip() != null) {
            EntityPermissionsSubordinateEntityIdentifierRaw id = new EntityPermissionsSubordinateEntityIdentifierRaw();
            id.setType(EntityPermissionsSubordinateEntityIdentifierTypeRaw.NIP);
            id.setValue(filter.subordinateEntityNip());
            body.subordinateEntityIdentifier(id);
        }
        return body;
    }
}
