/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitContextIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionGrantRequest;
import java.util.Objects;

/**
 * Builder for subunit permission grant requests.
 * <p>Required: subject identifier (NIP/PESEL/Fingerprint), context identifier,
 * description (5-256 chars), personDetails. Optional: subunitName.
 *
 * @since 1.0.0
 */
public final class SubunitPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_CONTEXT_REQUIRED = "context identifier is required — use .contextNip() or .contextInternalId()";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "personDetails (firstName, lastName) is required by KSeF server";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifier value is required";
    private static final String ERR_NULL_CONTEXT_NIP = "context NIP is required";
    private static final String ERR_NULL_INTERNAL_ID = "internal ID is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_FIRST_NAME = "firstName is required";
    private static final String ERR_NULL_LAST_NAME = "lastName is required";
    private static final String ERR_NULL_SUBUNIT_NAME = "subunitName must not be null if provided";

    private final PersonSubjectIdentifierType identifierType;
    private final String identifierValue;
    private SubunitContextIdentifierType contextType;
    private String contextValue;
    private String description;
    private String firstName;
    private String lastName;
    private String subunitName;

    private SubunitPermissionGrantBuilder(PersonSubjectIdentifierType type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    public static SubunitPermissionGrantBuilder forNip(String nip) {
        return new SubunitPermissionGrantBuilder(PersonSubjectIdentifierType.NIP, nip);
    }

    public static SubunitPermissionGrantBuilder forPesel(String pesel) {
        return new SubunitPermissionGrantBuilder(PersonSubjectIdentifierType.PESEL, pesel);
    }

    public static SubunitPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new SubunitPermissionGrantBuilder(PersonSubjectIdentifierType.FINGERPRINT, fingerprint);
    }

    public SubunitPermissionGrantBuilder contextNip(String nip) {
        this.contextType = SubunitContextIdentifierType.NIP;
        this.contextValue = Objects.requireNonNull(nip, ERR_NULL_CONTEXT_NIP);
        return this;
    }

    public SubunitPermissionGrantBuilder contextInternalId(String internalId) {
        this.contextType = SubunitContextIdentifierType.INTERNAL_ID;
        this.contextValue = Objects.requireNonNull(internalId, ERR_NULL_INTERNAL_ID);
        return this;
    }

    public SubunitPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public SubunitPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    public SubunitPermissionGrantBuilder subunitName(String subunitName) {
        this.subunitName = Objects.requireNonNull(subunitName, ERR_NULL_SUBUNIT_NAME);
        return this;
    }

    public SubunitPermissionGrantBuilder toBuilder() {
        SubunitPermissionGrantBuilder copy = new SubunitPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.contextType = this.contextType;
        copy.contextValue = this.contextValue;
        copy.description = this.description;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.subunitName = this.subunitName;
        return copy;
    }

    public SubunitPermissionGrantRequest build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (contextType == null) {
            throw new IllegalStateException(ERR_CONTEXT_REQUIRED);
        }
        if (firstName == null || lastName == null) {
            throw new IllegalStateException(ERR_SUBJECT_DETAILS_REQUIRED);
        }
        return new SubunitPermissionGrantRequest(identifierType, identifierValue,
                contextType, contextValue, description, firstName, lastName, subunitName);
    }
}
