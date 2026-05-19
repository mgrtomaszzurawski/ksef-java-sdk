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
 * @since 1.0.0
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

    PersonalPermissions queryPersonal(PersonalPermissionsQueryRequest request);
    PersonPermissions queryPersons(PersonPermissionsQueryRequest request);
    SubunitPermissions querySubunits(SubunitPermissionsQueryRequest filter);
    EntityPermissions queryEntities(EntityPermissionsQueryRequest filter);
    EntityRoles queryEntityRoles(EntityRolesQueryRequest filter);
    SubordinateEntityRoles querySubordinateRoles(SubordinateEntityRolesQueryRequest filter);
    EntityAuthorizationPermissions queryAuthorizations(EntityAuthorizationPermissionsQueryRequest request);
    EuEntityPermissions queryEuEntities(EuEntityPermissionsQueryRequest request);

    // Stream paginators — each walks pageOffset = 0, 1, 2, ... until
    // hasMore=false. request.pageOffset() is ignored on stream*.

    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission>
            streamPersonal(PersonalPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission>
            streamPersons(PersonPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission>
            streamSubunits(SubunitPermissionsQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission>
            streamEntities(EntityPermissionsQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole>
            streamEntityRoles(EntityRolesQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole>
            streamSubordinateRoles(SubordinateEntityRolesQueryRequest filter);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant>
            streamAuthorizations(EntityAuthorizationPermissionsQueryRequest request);
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission>
            streamEuEntities(EuEntityPermissionsQueryRequest request);
}
