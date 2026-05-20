/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for person permission grant requests.
 * <p>Required: subject identifier (NIP/PESEL/Fingerprint), description (5-256 chars),
 * subject details (firstName + lastName), and at least one permission.
 *
 * @since 0.1.0
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

    private final PersonSubjectIdentifierType identifierType;
    private final String identifierValue;
    private @Nullable String description;
    private @Nullable String firstName;
    private @Nullable String lastName;
    private final List<PersonPermissionType> permissions = new ArrayList<>();

    private PersonPermissionGrantBuilder(PersonSubjectIdentifierType type, String value) {
        this.identifierType = type;
        this.identifierValue = Objects.requireNonNull(value, ERR_NULL_IDENTIFIER_VALUE);
    }

    public static PersonPermissionGrantBuilder forPesel(String pesel) {
        return new PersonPermissionGrantBuilder(PersonSubjectIdentifierType.PESEL, pesel);
    }

    public static PersonPermissionGrantBuilder forNip(String nip) {
        return new PersonPermissionGrantBuilder(PersonSubjectIdentifierType.NIP, nip);
    }

    public static PersonPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new PersonPermissionGrantBuilder(PersonSubjectIdentifierType.FINGERPRINT, fingerprint);
    }

    public PersonPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public PersonPermissionGrantBuilder personDetails(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, ERR_NULL_FIRST_NAME);
        this.lastName = Objects.requireNonNull(lastName, ERR_NULL_LAST_NAME);
        return this;
    }

    public PersonPermissionGrantBuilder invoiceRead() { permissions.add(PersonPermissionType.INVOICE_READ); return this; }
    public PersonPermissionGrantBuilder invoiceWrite() { permissions.add(PersonPermissionType.INVOICE_WRITE); return this; }
    public PersonPermissionGrantBuilder credentialsRead() { permissions.add(PersonPermissionType.CREDENTIALS_READ); return this; }
    public PersonPermissionGrantBuilder credentialsManage() { permissions.add(PersonPermissionType.CREDENTIALS_MANAGE); return this; }
    public PersonPermissionGrantBuilder subunitManage() { permissions.add(PersonPermissionType.SUBUNIT_MANAGE); return this; }
    public PersonPermissionGrantBuilder enforcementOperations() { permissions.add(PersonPermissionType.ENFORCEMENT_OPERATIONS); return this; }
    public PersonPermissionGrantBuilder introspection() { permissions.add(PersonPermissionType.INTROSPECTION); return this; }

    public PersonPermissionGrantBuilder toBuilder() {
        PersonPermissionGrantBuilder copy = new PersonPermissionGrantBuilder(this.identifierType, this.identifierValue);
        copy.description = this.description;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.permissions.addAll(this.permissions);
        return copy;
    }

    public PersonPermissionGrantRequest build() {
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
        return new PersonPermissionGrantRequest(identifierType, identifierValue, description, firstName, lastName, permissions);
    }
}
