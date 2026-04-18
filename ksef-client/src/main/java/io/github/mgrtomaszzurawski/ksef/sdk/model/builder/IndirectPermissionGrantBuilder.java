/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for indirect permission grant requests (through an intermediary entity).
 * <p>
 * Required: subject identifier (NIP, PESEL, or fingerprint), description (5-256 chars),
 * at least one permission, person details (firstName + lastName).
 * Optional: target identifier.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = IndirectPermissionGrantBuilder
 *     .forNip("1234567890")
 *     .description("Indirect invoice access")
 *     .personDetails("Jan", "Kowalski")
 *     .invoiceRead();
 * }</pre>
 */
public final class IndirectPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "personDetails (firstName, lastName) is required by KSeF server";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifier value is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_FIRST_NAME = "firstName is required";
    private static final String ERR_NULL_LAST_NAME = "lastName is required";
    private static final String ERR_NULL_TARGET_NIP = "target NIP is required";
    private static final String ERR_NULL_INTERNAL_ID = "internal ID is required";

    private final IndirectPermissionsSubjectIdentifierTypeRaw identifierType;
    private final String identifierValue;
    private String description;
    private String firstName;
    private String lastName;
    private IndirectPermissionsTargetIdentifierTypeRaw targetType;
    private String targetValue;
    private final List<IndirectPermissionTypeRaw> permissions = new ArrayList<>();

    private IndirectPermissionGrantBuilder(IndirectPermissionsSubjectIdentifierTypeRaw type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    /**
     * Grant indirect permissions to a subject identified by NIP.
     */
    public static IndirectPermissionGrantBuilder forNip(String nip) {
        return new IndirectPermissionGrantBuilder(IndirectPermissionsSubjectIdentifierTypeRaw.NIP, nip);
    }

    /**
     * Grant indirect permissions to a subject identified by PESEL.
     */
    public static IndirectPermissionGrantBuilder forPesel(String pesel) {
        return new IndirectPermissionGrantBuilder(IndirectPermissionsSubjectIdentifierTypeRaw.PESEL, pesel);
    }

    /**
     * Grant indirect permissions to a subject identified by certificate fingerprint.
     */
    public static IndirectPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new IndirectPermissionGrantBuilder(IndirectPermissionsSubjectIdentifierTypeRaw.FINGERPRINT, fingerprint);
    }

    public IndirectPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set person details (required by KSeF server).
     */
    public IndirectPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    /**
     * Set target entity by NIP.
     */
    public IndirectPermissionGrantBuilder targetNip(String nip) {
        this.targetType = IndirectPermissionsTargetIdentifierTypeRaw.NIP;
        this.targetValue = Objects.requireNonNull(nip, ERR_NULL_TARGET_NIP);
        return this;
    }

    /**
     * Set target to all partners.
     */
    public IndirectPermissionGrantBuilder targetAllPartners() {
        this.targetType = IndirectPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    /**
     * Set target by internal ID.
     */
    public IndirectPermissionGrantBuilder targetInternalId(String internalId) {
        this.targetType = IndirectPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        this.targetValue = Objects.requireNonNull(internalId, ERR_NULL_INTERNAL_ID);
        return this;
    }

    public IndirectPermissionGrantBuilder invoiceRead() {
        permissions.add(IndirectPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public IndirectPermissionGrantBuilder invoiceWrite() {
        permissions.add(IndirectPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    /**
     * Build the indirect permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantIndirect()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public IndirectPermissionsGrantRequestRaw build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }
        if (firstName == null || lastName == null) {
            throw new IllegalStateException(ERR_SUBJECT_DETAILS_REQUIRED);
        }

        IndirectPermissionsSubjectIdentifierRaw subjectId = new IndirectPermissionsSubjectIdentifierRaw()
                .type(identifierType)
                .value(identifierValue);

        PersonDetailsRaw personDetails = new PersonDetailsRaw()
                .firstName(firstName)
                .lastName(lastName);

        PersonPermissionSubjectDetailsRaw subjectDetails = new PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);

        IndirectPermissionsGrantRequestRaw request = new IndirectPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setPermissions(new ArrayList<>(permissions));
        request.setDescription(description);
        request.setSubjectDetails(subjectDetails);

        if (targetType != null) {
            IndirectPermissionsTargetIdentifierRaw targetId = new IndirectPermissionsTargetIdentifierRaw()
                    .type(targetType);
            if (targetValue != null) {
                targetId.value(targetValue);
            }
            request.setTargetIdentifier(targetId);
        }

        return request;
    }
}
