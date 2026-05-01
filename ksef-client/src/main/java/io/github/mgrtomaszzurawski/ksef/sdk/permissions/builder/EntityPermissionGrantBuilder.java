/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierTypeRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for entity permission grant requests.
 * <p>
 * Required: subject NIP, description (5-256 chars), at least one permission, entity details (fullName).
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EntityPermissionGrantBuilder
 *     .forNip("1234567890")
 *     .description("Invoice access for partner entity")
 *     .entityDetails("Firma Sp. z o.o.")
 *     .invoiceRead()
 *     .invoiceWrite();
 * }</pre>
 */
public final class EntityPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_ENTITY_DETAILS_REQUIRED = "entityDetails (fullName) is required by KSeF server";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_NIP = "NIP is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_FULL_NAME = "fullName is required";

    private final String identifierValue;
    private String description;
    private String fullName;
    private final List<EntityPermissionRaw> permissions = new ArrayList<>();

    private EntityPermissionGrantBuilder(String nip) {
        this.identifierValue = Objects.requireNonNull(nip, ERR_NULL_NIP);
    }

    /**
     * Grant permissions to an entity identified by NIP.
     */
    public static EntityPermissionGrantBuilder forNip(String nip) {
        return new EntityPermissionGrantBuilder(nip);
    }

    public EntityPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set entity details (required by KSeF server).
     */
    public EntityPermissionGrantBuilder entityDetails(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, ERR_NULL_FULL_NAME);
        return this;
    }

    public EntityPermissionGrantBuilder invoiceRead() {
        permissions.add(new EntityPermissionRaw().type(EntityPermissionTypeRaw.INVOICE_READ));
        return this;
    }

    public EntityPermissionGrantBuilder invoiceReadDelegatable() {
        permissions.add(new EntityPermissionRaw().type(EntityPermissionTypeRaw.INVOICE_READ).canDelegate(true));
        return this;
    }

    public EntityPermissionGrantBuilder invoiceWrite() {
        permissions.add(new EntityPermissionRaw().type(EntityPermissionTypeRaw.INVOICE_WRITE));
        return this;
    }

    public EntityPermissionGrantBuilder invoiceWriteDelegatable() {
        permissions.add(new EntityPermissionRaw().type(EntityPermissionTypeRaw.INVOICE_WRITE).canDelegate(true));
        return this;
    }

    /**
     * Build the entity permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantEntity()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public EntityPermissionsGrantRequestRaw build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }
        if (fullName == null) {
            throw new IllegalStateException(ERR_ENTITY_DETAILS_REQUIRED);
        }

        EntityPermissionsSubjectIdentifierRaw subjectId = new EntityPermissionsSubjectIdentifierRaw()
                .type(EntityPermissionsSubjectIdentifierTypeRaw.NIP)
                .value(identifierValue);

        EntityDetailsRaw entityDetails = new EntityDetailsRaw()
                .fullName(fullName);

        EntityPermissionsGrantRequestRaw request = new EntityPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setPermissions(new ArrayList<>(permissions));
        request.setDescription(description);
        request.setSubjectDetails(entityDetails);
        return request;
    }
}
