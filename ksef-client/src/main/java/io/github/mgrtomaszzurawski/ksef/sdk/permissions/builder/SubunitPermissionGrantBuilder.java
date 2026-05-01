/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierTypeRaw;
import java.util.Objects;

/**
 * Builder for subunit permission grant requests.
 * <p>
 * Required: subject identifier (NIP, PESEL, or fingerprint), context identifier,
 * description (5-256 chars), person details.
 * Optional: subunit name.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = SubunitPermissionGrantBuilder
 *     .forPesel("82060411457")
 *     .contextNip("1234567890")
 *     .description("Subunit access for Jan Kowalski")
 *     .personDetails("Jan", "Kowalski");
 * }</pre>
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

    private final SubunitPermissionsSubjectIdentifierTypeRaw identifierType;
    private final String identifierValue;
    private SubunitPermissionsContextIdentifierTypeRaw contextType;
    private String contextValue;
    private String description;
    private String firstName;
    private String lastName;
    private String subunitName;

    private SubunitPermissionGrantBuilder(SubunitPermissionsSubjectIdentifierTypeRaw type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    /**
     * Grant subunit permissions to a subject identified by NIP.
     */
    public static SubunitPermissionGrantBuilder forNip(String nip) {
        return new SubunitPermissionGrantBuilder(SubunitPermissionsSubjectIdentifierTypeRaw.NIP, nip);
    }

    /**
     * Grant subunit permissions to a subject identified by PESEL.
     */
    public static SubunitPermissionGrantBuilder forPesel(String pesel) {
        return new SubunitPermissionGrantBuilder(SubunitPermissionsSubjectIdentifierTypeRaw.PESEL, pesel);
    }

    /**
     * Grant subunit permissions to a subject identified by certificate fingerprint.
     */
    public static SubunitPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new SubunitPermissionGrantBuilder(SubunitPermissionsSubjectIdentifierTypeRaw.FINGERPRINT, fingerprint);
    }

    /**
     * Set context identifier by NIP.
     */
    public SubunitPermissionGrantBuilder contextNip(String nip) {
        this.contextType = SubunitPermissionsContextIdentifierTypeRaw.NIP;
        this.contextValue = Objects.requireNonNull(nip, ERR_NULL_CONTEXT_NIP);
        return this;
    }

    /**
     * Set context identifier by internal ID.
     */
    public SubunitPermissionGrantBuilder contextInternalId(String internalId) {
        this.contextType = SubunitPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        this.contextValue = Objects.requireNonNull(internalId, ERR_NULL_INTERNAL_ID);
        return this;
    }

    public SubunitPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set person details (required by KSeF server).
     */
    public SubunitPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    /**
     * Set optional subunit name.
     */
    public SubunitPermissionGrantBuilder subunitName(String subunitName) {
        this.subunitName = Objects.requireNonNull(subunitName, ERR_NULL_SUBUNIT_NAME);
        return this;
    }

    /**
     * Build the subunit permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantSubunit()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public SubunitPermissionsGrantRequestRaw build() {
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

        SubunitPermissionsSubjectIdentifierRaw subjectId = new SubunitPermissionsSubjectIdentifierRaw()
                .type(identifierType)
                .value(identifierValue);

        SubunitPermissionsContextIdentifierRaw contextId = new SubunitPermissionsContextIdentifierRaw()
                .type(contextType)
                .value(contextValue);

        PersonDetailsRaw personDetails = new PersonDetailsRaw()
                .firstName(firstName)
                .lastName(lastName);

        PersonPermissionSubjectDetailsRaw subjectDetails = new PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);

        SubunitPermissionsGrantRequestRaw request = new SubunitPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setContextIdentifier(contextId);
        request.setDescription(description);
        request.setSubjectDetails(subjectDetails);

        if (subunitName != null) {
            request.setSubunitName(subunitName);
        }

        return request;
    }
}
