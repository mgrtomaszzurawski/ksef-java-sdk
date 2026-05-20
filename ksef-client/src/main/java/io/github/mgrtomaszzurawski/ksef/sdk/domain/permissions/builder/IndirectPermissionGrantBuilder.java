/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectTargetIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for indirect permission grant requests (through an intermediary entity).
 * <p>Required: subject identifier (NIP/PESEL/Fingerprint), description (5-256 chars),
 * personDetails, at least one permission. Optional: target identifier.
 *
 * @since 0.1.0
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

    private final PersonSubjectIdentifierType identifierType;
    private final String identifierValue;
    private @Nullable String description;
    private @Nullable String firstName;
    private @Nullable String lastName;
    private @Nullable IndirectTargetIdentifierType targetType;
    private @Nullable String targetValue;
    private final List<IndirectPermissionType> permissions = new ArrayList<>();

    private IndirectPermissionGrantBuilder(PersonSubjectIdentifierType type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    public static IndirectPermissionGrantBuilder forNip(String nip) {
        return new IndirectPermissionGrantBuilder(PersonSubjectIdentifierType.NIP, nip);
    }

    public static IndirectPermissionGrantBuilder forPesel(String pesel) {
        return new IndirectPermissionGrantBuilder(PersonSubjectIdentifierType.PESEL, pesel);
    }

    public static IndirectPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new IndirectPermissionGrantBuilder(PersonSubjectIdentifierType.FINGERPRINT, fingerprint);
    }

    public IndirectPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public IndirectPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    public IndirectPermissionGrantBuilder targetNip(String nip) {
        this.targetType = IndirectTargetIdentifierType.NIP;
        this.targetValue = Objects.requireNonNull(nip, ERR_NULL_TARGET_NIP);
        return this;
    }

    public IndirectPermissionGrantBuilder targetAllPartners() {
        this.targetType = IndirectTargetIdentifierType.ALL_PARTNERS;
        this.targetValue = null;
        return this;
    }

    public IndirectPermissionGrantBuilder targetInternalId(String internalId) {
        this.targetType = IndirectTargetIdentifierType.INTERNAL_ID;
        this.targetValue = Objects.requireNonNull(internalId, ERR_NULL_INTERNAL_ID);
        return this;
    }

    public IndirectPermissionGrantBuilder invoiceRead() {
        permissions.add(IndirectPermissionType.INVOICE_READ);
        return this;
    }

    public IndirectPermissionGrantBuilder invoiceWrite() {
        permissions.add(IndirectPermissionType.INVOICE_WRITE);
        return this;
    }

    public IndirectPermissionGrantBuilder toBuilder() {
        IndirectPermissionGrantBuilder copy = new IndirectPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.description = this.description;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.targetType = this.targetType;
        copy.targetValue = this.targetValue;
        copy.permissions.addAll(this.permissions);
        return copy;
    }

    public IndirectPermissionGrantRequest build() {
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
        return new IndirectPermissionGrantRequest(identifierType, identifierValue, description,
                firstName, lastName, targetType, targetValue, permissions);
    }
}
