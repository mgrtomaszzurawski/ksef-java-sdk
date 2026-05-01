/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTypeRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for entity authorization permissions query requests.
 * <p>
 * Required: query type (granted or received). All other fields are optional.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EntityAuthorizationPermissionsQueryBuilder
 *     .granted()
 *     .authorizedByNip("1234567890")
 *     .selfInvoicing();
 * }</pre>
 */
public final class EntityAuthorizationPermissionsQueryBuilder {

    private static final String ERR_QUERY_TYPE_REQUIRED = "queryType is required — use .granted() or .received()";

    private final QueryTypeRaw queryType;
    private EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw authorizingType;
    private String authorizingValue;
    private EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw authorizedType;
    private String authorizedValue;
    private final List<InvoicePermissionTypeRaw> permissionTypes = new ArrayList<>();

    private EntityAuthorizationPermissionsQueryBuilder(QueryTypeRaw queryType) {
        this.queryType = Objects.requireNonNull(queryType, ERR_QUERY_TYPE_REQUIRED);
    }

    /**
     * Query granted authorization permissions.
     */
    public static EntityAuthorizationPermissionsQueryBuilder granted() {
        return new EntityAuthorizationPermissionsQueryBuilder(QueryTypeRaw.GRANTED);
    }

    /**
     * Query received authorization permissions.
     */
    public static EntityAuthorizationPermissionsQueryBuilder received() {
        return new EntityAuthorizationPermissionsQueryBuilder(QueryTypeRaw.RECEIVED);
    }

    // --- Authorizing identifier ---

    /**
     * Filter by authorizing entity NIP.
     */
    public EntityAuthorizationPermissionsQueryBuilder authorizingByNip(String nip) {
        this.authorizingType = EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw.NIP;
        this.authorizingValue = nip;
        return this;
    }

    // --- Authorized identifier ---

    /**
     * Filter by authorized entity NIP.
     */
    public EntityAuthorizationPermissionsQueryBuilder authorizedByNip(String nip) {
        this.authorizedType = EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw.NIP;
        this.authorizedValue = nip;
        return this;
    }

    /**
     * Filter by authorized entity Peppol ID.
     */
    public EntityAuthorizationPermissionsQueryBuilder authorizedByPeppolId(String peppolId) {
        this.authorizedType = EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw.PEPPOL_ID;
        this.authorizedValue = peppolId;
        return this;
    }

    // --- Permission types ---

    public EntityAuthorizationPermissionsQueryBuilder selfInvoicing() {
        permissionTypes.add(InvoicePermissionTypeRaw.SELF_INVOICING);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder rrInvoicing() {
        permissionTypes.add(InvoicePermissionTypeRaw.RR_INVOICING);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder taxRepresentative() {
        permissionTypes.add(InvoicePermissionTypeRaw.TAX_REPRESENTATIVE);
        return this;
    }

    public EntityAuthorizationPermissionsQueryBuilder pefInvoicing() {
        permissionTypes.add(InvoicePermissionTypeRaw.PEF_INVOICING);
        return this;
    }

    /**
     * Build the entity authorization permissions query request.
     *
     * @return the request ready to pass to {@code PermissionClient.queryAuthorizations()}
     */
    public EntityAuthorizationPermissionsQueryRequestRaw build() {
        EntityAuthorizationPermissionsQueryRequestRaw request =
                new EntityAuthorizationPermissionsQueryRequestRaw();
        request.setQueryType(queryType);

        if (authorizingType != null) {
            EntityAuthorizationsAuthorizingEntityIdentifierRaw authorizingId =
                    new EntityAuthorizationsAuthorizingEntityIdentifierRaw()
                            .type(authorizingType)
                            .value(authorizingValue);
            request.setAuthorizingIdentifier(authorizingId);
        }

        if (authorizedType != null) {
            EntityAuthorizationsAuthorizedEntityIdentifierRaw authorizedId =
                    new EntityAuthorizationsAuthorizedEntityIdentifierRaw()
                            .type(authorizedType)
                            .value(authorizedValue);
            request.setAuthorizedIdentifier(authorizedId);
        }

        if (!permissionTypes.isEmpty()) {
            request.setPermissionTypes(new ArrayList<>(permissionTypes));
        }

        return request;
    }
}
