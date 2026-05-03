/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AuthorizationQueryType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for entity authorization permissions query requests.
 * <p>Required: query direction (granted or received). All other fields are optional.
 */
public final class EntityAuthorizationPermissionsQueryBuilder {

    private static final String ERR_QUERY_TYPE_REQUIRED = "queryType is required — use .granted() or .received()";

    private final AuthorizationQueryType queryType;
    private String authorizingNip;
    private EntityAuthorizationIdentifierType authorizedType;
    private String authorizedValue;
    private final List<EntityAuthorizationPermissionType> permissionTypes = new ArrayList<>();

    private EntityAuthorizationPermissionsQueryBuilder(AuthorizationQueryType queryType) {
        this.queryType = Objects.requireNonNull(queryType, ERR_QUERY_TYPE_REQUIRED);
    }

    public static EntityAuthorizationPermissionsQueryBuilder granted() {
        return new EntityAuthorizationPermissionsQueryBuilder(AuthorizationQueryType.GRANTED);
    }

    public static EntityAuthorizationPermissionsQueryBuilder received() {
        return new EntityAuthorizationPermissionsQueryBuilder(AuthorizationQueryType.RECEIVED);
    }

    public EntityAuthorizationPermissionsQueryBuilder authorizingByNip(String nip) {
        this.authorizingNip = nip;
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder authorizedByNip(String nip) {
        this.authorizedType = EntityAuthorizationIdentifierType.NIP;
        this.authorizedValue = nip;
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder authorizedByPeppolId(String peppolId) {
        this.authorizedType = EntityAuthorizationIdentifierType.PEPPOL_ID;
        this.authorizedValue = peppolId;
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder selfInvoicing() {
        permissionTypes.add(EntityAuthorizationPermissionType.SELF_INVOICING);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder rrInvoicing() {
        permissionTypes.add(EntityAuthorizationPermissionType.RR_INVOICING);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder taxRepresentative() {
        permissionTypes.add(EntityAuthorizationPermissionType.TAX_REPRESENTATIVE);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder pefInvoicing() {
        permissionTypes.add(EntityAuthorizationPermissionType.PEF_INVOICING);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder toBuilder() {
        EntityAuthorizationPermissionsQueryBuilder copy = new EntityAuthorizationPermissionsQueryBuilder(this.queryType);
        copy.authorizingNip = this.authorizingNip;
        copy.authorizedType = this.authorizedType;
        copy.authorizedValue = this.authorizedValue;
        copy.permissionTypes.addAll(this.permissionTypes);
        return copy;
    }

    public EntityAuthorizationPermissionsQueryRequest build() {
        return new EntityAuthorizationPermissionsQueryRequest(queryType, authorizingNip,
                authorizedType, authorizedValue, permissionTypes);
    }
}
