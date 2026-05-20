/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRolesQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRolesQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;
import java.time.Duration;

/**
 * KSeF permission management — granting, revoking, and querying
 * permissions for persons, entities, EU entities, subunits, and
 * authorizations. Reached via {@code KsefClient.permissions()}.
 *
 * <h2>Sync-default operation pattern (ADR-032 / ADR-033)</h2>
 *
 * Every {@link #grant} and {@code revoke*} operation polls the KSeF
 * operation-status endpoint internally and returns the terminal
 * {@link PermissionOperationStatus}. Default timeout is 5 minutes
 * (per ADR-032); pass an explicit {@link Duration} to override per
 * call. Throws {@code KsefAsyncTimeoutException} if the operation
 * does not reach terminal state within the timeout.
 *
 * <p><strong>Timeout + retry semantics</strong>: if a grant/revoke
 * operation times out, the underlying KSeF operation may still
 * complete asynchronously. Use the {@code referenceNumber} from
 * {@link PermissionOperationStatus} to query the operation status
 * before retrying — duplicating the call may produce duplicate grants.
 *
 * <h2>Sealed grant dispatch (R2-11a)</h2>
 *
 * The KSeF spec defines 7 distinct permission-grant wire endpoints
 * with different required fields (see
 * {@link PermissionGrantRequest}'s {@code permits} clause). The SDK
 * collapses the previous {@code grantPerson} / {@code grantEntity} /
 * … 14-method surface into a single
 * {@link #grant(PermissionGrantRequest)} method dispatched via
 * sealed pattern matching. Consumers select the concrete request
 * type via builder (e.g. {@code PersonPermissionGrantBuilder.create()
 * .…build()}); the SDK routes to the correct endpoint internally.
 *
 * <p><strong>Two separate revoke methods</strong>: KSeF exposes
 * two distinct DELETE endpoints — one for the "common" permission
 * grants and one for the "authorization" grants
 * (SelfInvoicing/RRInvoicing/TaxRepresentative). The SDK cannot
 * infer which from a permission ID alone, so the call site picks
 * via {@link #revokePermission} (common) or
 * {@link #revokeAuthorization} (authorizations) per spec
 * {@code ksef-docs/uprawnienia.md}.
 *
 * <h2>Query / stream pagination</h2>
 *
 * Eight {@code query*} methods return one page each, honouring the
 * request's {@code pageOffset}/{@code pageSize} (R2-9c). Eight
 * matching {@code stream*} methods walk every page lazily until the
 * server reports {@code hasMore = false}; {@code request.pageOffset()}
 * is <em>ignored</em> on stream methods — the paginator always starts
 * from page 0. Use {@code query*} for snapshot at a specific offset.
 *
 * @since 0.1.0
 */
public interface Permissions {

    /**
     * Grant a permission using the SDK's default timeout (5 minutes
     * per ADR-032). Dispatches to the correct KSeF endpoint based on
     * the concrete subtype of {@link PermissionGrantRequest}.
     *
     * @param request one of the 7 concrete subtypes of
     *     {@link PermissionGrantRequest} (sealed)
     * @return terminal operation status (reference number + result)
     * @throws io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException
     *     if the operation does not reach terminal state within the
     *     default timeout
     */
    PermissionOperationStatus grant(PermissionGrantRequest request);

    /**
     * Grant a permission with an explicit polling timeout. Dispatches
     * to the correct KSeF endpoint based on the concrete subtype of
     * {@link PermissionGrantRequest}.
     *
     * @param request one of the 7 concrete subtypes of
     *     {@link PermissionGrantRequest} (sealed)
     * @param timeout polling deadline for terminal state
     * @return terminal operation status (reference number + result)
     * @throws io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException
     *     if the operation does not reach terminal state within
     *     {@code timeout}
     */
    PermissionOperationStatus grant(PermissionGrantRequest request, Duration timeout);

    /**
     * Revoke a common permission grant (persons / entities / subunits
     * / EU entities / indirect). Maps to
     * {@code DELETE /permissions/common/grants/{permissionId}}.
     *
     * @param permissionId the permission ID returned earlier by
     *     {@link PermissionOperationStatus#permissionId()} on the
     *     grant operation
     */
    PermissionOperationStatus revokePermission(String permissionId);
    PermissionOperationStatus revokePermission(String permissionId, Duration timeout);

    /**
     * Revoke an authorization grant (SelfInvoicing / RRInvoicing /
     * TaxRepresentative / PefInvoicing). Maps to a separate KSeF
     * endpoint:
     * {@code DELETE /permissions/authorizations/grants/{permissionId}}.
     * Use {@link #revokePermission} for non-authorization permissions.
     *
     * @param permissionId the permission ID returned earlier by
     *     {@link PermissionOperationStatus#permissionId()} on the
     *     authorization grant operation
     */
    PermissionOperationStatus revokeAuthorization(String permissionId);

    /**
     * Synchronous-with-custom-timeout overload of {@link #revokeAuthorization(String)}.
     *
     * @param permissionId the permission ID returned by the authorization grant
     * @param timeout overall budget for the polling loop (non-null)
     */
    PermissionOperationStatus revokeAuthorization(String permissionId, Duration timeout);

    /**
     * Returns the attachment-consent status for the current
     * authentication context. Attachment consent is required to issue
     * invoices with attachments.
     *
     * <p><strong>Note:</strong> attachment consent is granted
     * exclusively via the external e-Urząd Skarbowy service, NOT
     * through this API. Per
     * {@code ksef-docs/uprawnienia.md}: "Zgoda jest nadawana poza API,
     * wyłącznie w usłudze e-Urząd Skarbowy". If the status returns
     * {@link AttachmentPermissionStatus#attachmentAllowed()} false /
     * null, direct the user to apply through e-Urząd Skarbowy — there
     * is no SDK method to grant the consent.
     */
    AttachmentPermissionStatus getAttachmentStatus();

    /**
     * Query permissions the authenticated subject holds in the current
     * authorisation context — paginated single-page snapshot. Wire:
     * {@code POST /permissions/query/personal/grants}. The impl substitutes
     * {@code pageOffset=0} / {@code pageSize=100} when null on the request
     * (KSeF enforces {@code pageSize in [10, 100]} on permission endpoints).
     */
    PersonalPermissions queryPersonal(PersonalPermissionsQueryRequest request);

    /**
     * Query person-grant assignments — single-page snapshot. Wire:
     * {@code POST /permissions/query/persons/grants}. Defaults: pageOffset=0,
     * pageSize=100; KSeF range {@code [10, 100]} per spec.
     */
    PersonPermissions queryPersons(PersonPermissionsQueryRequest request);

    /**
     * Query subunit-grant assignments — single-page snapshot. Wire:
     * {@code POST /permissions/query/subunits/grants}. Defaults: pageOffset=0,
     * pageSize=100.
     */
    SubunitPermissions querySubunits(SubunitPermissionsQueryRequest filter);

    /**
     * Query entity-grant assignments — single-page snapshot. Wire:
     * {@code POST /permissions/query/entities/grants}. Defaults: pageOffset=0,
     * pageSize=100.
     */
    EntityPermissions queryEntities(EntityPermissionsQueryRequest filter);

    /**
     * Query entity roles — single-page snapshot. Wire:
     * {@code GET /permissions/query/entities/roles}. Defaults: pageOffset=0,
     * pageSize=100.
     */
    EntityRoles queryEntityRoles(EntityRolesQueryRequest filter);

    /**
     * Query subordinate-entity roles — single-page snapshot. Wire:
     * {@code POST /permissions/query/subordinate-entities/roles}. Defaults:
     * pageOffset=0, pageSize=100.
     */
    SubordinateEntityRoles querySubordinateRoles(SubordinateEntityRolesQueryRequest filter);

    /**
     * Query entity-authorization permissions — single-page snapshot. Wire:
     * {@code POST /permissions/query/authorizations/grants}. Defaults:
     * pageOffset=0, pageSize=100.
     */
    EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryRequest request);

    /**
     * Query EU-entity permissions — single-page snapshot. Wire:
     * {@code POST /permissions/query/eu-entities/grants}. Defaults: pageOffset=0,
     * pageSize=100. Requires NipVatUe-context credentials.
     */
    EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryRequest request);

    // Stream paginators — each walks pageOffset = 0, 1, 2, ... until
    // hasMore=false. request.pageOffset() is ignored on stream*.

    /**
     * Stream the personal-permissions query lazily — the SDK walks
     * {@code pageOffset = 0, 1, 2, ...} until the server returns
     * {@code hasMore=false}, with the KSeF-enforced max
     * {@code pageSize=100}. {@code request.pageOffset()} is ignored on
     * stream*; {@code request.pageSize()} is also ignored — the SDK uses
     * the spec-max page for fewest round-trips. Close the stream to abort
     * mid-walk.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            streamPersonal(PersonalPermissionsQueryRequest request);

    /**
     * Stream person grants lazily across pages. See {@link #streamPersonal}
     * for the {@code pageOffset}/{@code pageSize}-ignored contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryRequest request);

    /**
     * Stream subunit grants lazily across pages. See {@link #streamPersonal}
     * for the {@code pageOffset}/{@code pageSize}-ignored contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(SubunitPermissionsQueryRequest filter);

    /**
     * Stream entity grants lazily across pages. See {@link #streamPersonal}
     * for the {@code pageOffset}/{@code pageSize}-ignored contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            streamEntities(EntityPermissionsQueryRequest filter);

    /**
     * Stream entity roles lazily across pages. See {@link #streamPersonal}
     * for the {@code pageOffset}/{@code pageSize}-ignored contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole>
            streamEntityRoles(EntityRolesQueryRequest filter);

    /**
     * Stream subordinate-entity roles lazily across pages. See
     * {@link #streamPersonal} for the {@code pageOffset}/{@code pageSize}-ignored
     * contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(SubordinateEntityRolesQueryRequest filter);

    /**
     * Stream entity-authorization grants lazily across pages. See
     * {@link #streamPersonal} for the {@code pageOffset}/{@code pageSize}-ignored
     * contract.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            streamAuthorizations(EntityAuthorizationPermissionsQueryRequest request);

    /**
     * Stream EU-entity permissions lazily across pages. See {@link #streamPersonal}
     * for the {@code pageOffset}/{@code pageSize}-ignored contract.
     * Requires NipVatUe-context credentials.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryRequest request);
}
