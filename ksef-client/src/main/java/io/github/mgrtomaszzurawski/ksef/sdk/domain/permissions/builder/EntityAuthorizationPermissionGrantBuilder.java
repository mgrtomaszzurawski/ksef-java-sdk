/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for entity authorization permission grant requests.
 * <p>Required: identifier (NIP or PEPPOL_ID), permission type, description (5-256 chars), entityDetails.
 *
 * @since 1.0.0
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

    private final EntityAuthorizationIdentifierType identifierType;
    private final String identifierValue;
    private @Nullable String description;
    private @Nullable String fullName;
    private @Nullable EntityAuthorizationPermissionType permission;

    private EntityAuthorizationPermissionGrantBuilder(EntityAuthorizationIdentifierType type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    public static EntityAuthorizationPermissionGrantBuilder forNip(String nip) {
        return new EntityAuthorizationPermissionGrantBuilder(EntityAuthorizationIdentifierType.NIP, nip);
    }

    public static EntityAuthorizationPermissionGrantBuilder forPeppolId(String peppolId) {
        return new EntityAuthorizationPermissionGrantBuilder(EntityAuthorizationIdentifierType.PEPPOL_ID, peppolId);
    }

    public EntityAuthorizationPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder entityDetails(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, ERR_NULL_FULL_NAME);
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder selfInvoicing() {
        this.permission = EntityAuthorizationPermissionType.SELF_INVOICING;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder rrInvoicing() {
        this.permission = EntityAuthorizationPermissionType.RR_INVOICING;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder taxRepresentative() {
        this.permission = EntityAuthorizationPermissionType.TAX_REPRESENTATIVE;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder pefInvoicing() {
        this.permission = EntityAuthorizationPermissionType.PEF_INVOICING;
        return this;
    }

    public EntityAuthorizationPermissionGrantBuilder toBuilder() {
        EntityAuthorizationPermissionGrantBuilder copy =
                new EntityAuthorizationPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.description = this.description;
        copy.fullName = this.fullName;
        copy.permission = this.permission;
        return copy;
    }

    public EntityAuthorizationPermissionGrantRequest build() {
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
        return new EntityAuthorizationPermissionGrantRequest(identifierType, identifierValue, description, fullName, permission);
    }
}
