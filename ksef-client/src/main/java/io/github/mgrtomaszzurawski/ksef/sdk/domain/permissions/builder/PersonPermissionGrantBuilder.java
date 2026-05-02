/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsSubjectIdentifierTypeRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for person permission grant requests.
 * <p>
 * Required: subject identifier (NIP or PESEL), description (5-256 chars),
 * at least one permission, subject details (firstName + lastName).
 * <p>
 * Usage:
 * <pre>{@code
 * var request = PersonPermissionGrantBuilder
 *     .forPesel("82060411457")
 *     .description("Invoice access for Jan Kowalski")
 *     .personDetails("Jan", "Kowalski")
 *     .invoiceRead()
 *     .build();
 * }</pre>
 */
public final class PersonPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "personDetails (firstName, lastName) is required by KSeF server";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifier value is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_FIRST_NAME = "firstName is required";
    private static final String ERR_NULL_LAST_NAME = "lastName is required";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";

    private final PersonPermissionsSubjectIdentifierTypeRaw identifierType;
    private final String identifierValue;
    private String description;
    private String firstName;
    private String lastName;
    private final List<PersonPermissionTypeRaw> permissions = new ArrayList<>();

    private PersonPermissionGrantBuilder(PersonPermissionsSubjectIdentifierTypeRaw type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    /**
     * Grant permissions to a person identified by PESEL.
     */
    public static PersonPermissionGrantBuilder forPesel(String pesel) {
        return new PersonPermissionGrantBuilder(PersonPermissionsSubjectIdentifierTypeRaw.PESEL, pesel);
    }

    /**
     * Grant permissions to a person identified by NIP.
     */
    public static PersonPermissionGrantBuilder forNip(String nip) {
        return new PersonPermissionGrantBuilder(PersonPermissionsSubjectIdentifierTypeRaw.NIP, nip);
    }

    /**
     * Grant permissions to a person identified by certificate fingerprint.
     */
    public static PersonPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new PersonPermissionGrantBuilder(PersonPermissionsSubjectIdentifierTypeRaw.FINGERPRINT, fingerprint);
    }

    public PersonPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set person details (required by KSeF server, even though not marked as required in spec).
     */
    public PersonPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    public PersonPermissionGrantBuilder invoiceRead() {
        permissions.add(PersonPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public PersonPermissionGrantBuilder invoiceWrite() {
        permissions.add(PersonPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    public PersonPermissionGrantBuilder credentialsRead() {
        permissions.add(PersonPermissionTypeRaw.CREDENTIALS_READ);
        return this;
    }

    public PersonPermissionGrantBuilder credentialsManage() {
        permissions.add(PersonPermissionTypeRaw.CREDENTIALS_MANAGE);
        return this;
    }

    public PersonPermissionGrantBuilder subunitManage() {
        permissions.add(PersonPermissionTypeRaw.SUBUNIT_MANAGE);
        return this;
    }

    public PersonPermissionGrantBuilder enforcementOperations() {
        permissions.add(PersonPermissionTypeRaw.ENFORCEMENT_OPERATIONS);
        return this;
    }

    public PersonPermissionGrantBuilder introspection() {
        permissions.add(PersonPermissionTypeRaw.INTROSPECTION);
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public PersonPermissionGrantBuilder toBuilder() {
        PersonPermissionGrantBuilder copy = new PersonPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.description = this.description;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.permissions.addAll(this.permissions);
        return copy;
    }

    /**
     * Build the permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantPerson()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public PersonPermissionsGrantRequestRaw build() {
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

        PersonPermissionsSubjectIdentifierRaw subjectId = new PersonPermissionsSubjectIdentifierRaw()
                .type(identifierType)
                .value(identifierValue);

        PersonDetailsRaw personDetails = new PersonDetailsRaw()
                .firstName(firstName)
                .lastName(lastName);

        PersonPermissionSubjectDetailsRaw subjectDetails = new PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);

        PersonPermissionsGrantRequestRaw request = new PersonPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setPermissions(new ArrayList<>(permissions));
        request.setDescription(description);
        request.setSubjectDetails(subjectDetails);
        return request;
    }
}
