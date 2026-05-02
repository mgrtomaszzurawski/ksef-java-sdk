/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw;
import java.util.Objects;

/**
 * Builder for entity authorization permission grant requests.
 * <p>
 * Required: subject identifier (NIP or PeppolId), permission type, description (5-256 chars), entity details.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EntityAuthorizationPermissionGrantBuilder
 *     .forNip("1234567890")
 *     .selfInvoicing()
 *     .description("Self-invoicing authorization for partner")
 *     .entityDetails("Firma Sp. z o.o.");
 * }</pre>
 */
public final class EntityAuthorizationPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSION_REQUIRED = "permission type is required — use .selfInvoicing(), .rrInvoicing(), .taxRepresentative(), or .pefInvoicing()";
    private static final String ERR_ENTITY_DETAILS_REQUIRED = "entityDetails (fullName) is required by KSeF server";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifier value is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_FULL_NAME = "fullName is required";

    private final EntityAuthorizationPermissionsSubjectIdentifierTypeRaw identifierType;
    private final String identifierValue;
    private String description;
    private String fullName;
    private EntityAuthorizationPermissionTypeRaw permission;

    private EntityAuthorizationPermissionGrantBuilder(
            EntityAuthorizationPermissionsSubjectIdentifierTypeRaw type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    /**
     * Grant authorization to an entity identified by NIP.
     */
    public static EntityAuthorizationPermissionGrantBuilder forNip(String nip) {
        return new EntityAuthorizationPermissionGrantBuilder(
                EntityAuthorizationPermissionsSubjectIdentifierTypeRaw.NIP, nip);
    }

    /**
     * Grant authorization to an entity identified by Peppol ID.
     */
    public static EntityAuthorizationPermissionGrantBuilder forPeppolId(String peppolId) {
        return new EntityAuthorizationPermissionGrantBuilder(
                EntityAuthorizationPermissionsSubjectIdentifierTypeRaw.PEPPOL_ID, peppolId);
    }

    public EntityAuthorizationPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set entity details (required by KSeF server).
     */
    public EntityAuthorizationPermissionGrantBuilder entityDetails(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, ERR_NULL_FULL_NAME);
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder selfInvoicing() {
        this.permission = EntityAuthorizationPermissionTypeRaw.SELF_INVOICING;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder rrInvoicing() {
        this.permission = EntityAuthorizationPermissionTypeRaw.RR_INVOICING;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder taxRepresentative() {
        this.permission = EntityAuthorizationPermissionTypeRaw.TAX_REPRESENTATIVE;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder pefInvoicing() {
        this.permission = EntityAuthorizationPermissionTypeRaw.PEF_INVOICING;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public EntityAuthorizationPermissionGrantBuilder toBuilder() {
        EntityAuthorizationPermissionGrantBuilder copy =
                new EntityAuthorizationPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.description = this.description;
        copy.fullName = this.fullName;
        copy.permission = this.permission;
        return copy;
    }

    /**
     * Build the authorization permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantAuthorization()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public EntityAuthorizationPermissionsGrantRequestRaw build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (permission == null) {
            throw new IllegalStateException(ERR_PERMISSION_REQUIRED);
        }
        if (fullName == null) {
            throw new IllegalStateException(ERR_ENTITY_DETAILS_REQUIRED);
        }

        EntityAuthorizationPermissionsSubjectIdentifierRaw subjectId =
                new EntityAuthorizationPermissionsSubjectIdentifierRaw()
                        .type(identifierType)
                        .value(identifierValue);

        EntityDetailsRaw entityDetails = new EntityDetailsRaw()
                .fullName(fullName);

        EntityAuthorizationPermissionsGrantRequestRaw request =
                new EntityAuthorizationPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setPermission(permission);
        request.setDescription(description);
        request.setSubjectDetails(entityDetails);
        return request;
    }
}
