/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw;
import java.util.Objects;

/**
 * Builder for EU entity administration permission grant requests.
 * <p>
 * Required: subject fingerprint, context NIP-VAT-UE, description (5-256 chars),
 * EU entity name, subject details (entity by fingerprint), EU entity details.
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EuEntityAdminPermissionGrantBuilder
 *     .forFingerprint("ABC123DEF456")
 *     .contextNipVatUe("PL1234567890")
 *     .description("EU entity admin access")
 *     .euEntityName("EU Partner GmbH")
 *     .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
 *     .euEntityDetails("EU Partner GmbH", "Berlin, Germany");
 * }</pre>
 */
public final class EuEntityAdminPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_CONTEXT_REQUIRED = "context identifier is required — use .contextNipVatUe()";
    private static final String ERR_EU_ENTITY_NAME_REQUIRED = "euEntityName is required — use .euEntityName()";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "subject details are required — use .subjectEntityByFingerprint()";
    private static final String ERR_EU_ENTITY_DETAILS_REQUIRED = "EU entity details are required — use .euEntityDetails()";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_FINGERPRINT = "fingerprint is required";
    private static final String ERR_NULL_NIP_VAT_UE = "NIP-VAT-UE is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_EU_ENTITY_NAME = "euEntityName is required";
    private static final String ERR_NULL_SUBJECT_FULL_NAME = "subject fullName is required";
    private static final String ERR_NULL_SUBJECT_ADDRESS = "subject address is required";
    private static final String ERR_NULL_EU_ENTITY_FULL_NAME = "EU entity fullName is required";
    private static final String ERR_NULL_EU_ENTITY_ADDRESS = "EU entity address is required";

    private final String fingerprintValue;
    private String contextValue;
    private String description;
    private String euEntityName;
    private String subjectFullName;
    private String subjectAddress;
    private String euEntityFullName;
    private String euEntityAddress;

    private EuEntityAdminPermissionGrantBuilder(String fingerprint) {
        this.fingerprintValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
    }

    /**
     * Grant EU entity admin permissions to a subject identified by certificate fingerprint.
     */
    public static EuEntityAdminPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new EuEntityAdminPermissionGrantBuilder(fingerprint);
    }

    /**
     * Set context identifier by NIP-VAT-UE.
     */
    public EuEntityAdminPermissionGrantBuilder contextNipVatUe(String nipVatUe) {
        this.contextValue = Objects.requireNonNull(nipVatUe, ERR_NULL_NIP_VAT_UE);
        return this;
    }

    public EuEntityAdminPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    /**
     * Set the EU entity name.
     */
    public EuEntityAdminPermissionGrantBuilder euEntityName(String euEntityName) {
        this.euEntityName = Objects.requireNonNull(euEntityName, ERR_NULL_EU_ENTITY_NAME);
        return this;
    }

    /**
     * Set subject details as entity identified by fingerprint.
     */
    public EuEntityAdminPermissionGrantBuilder subjectEntityByFingerprint(String fullName, String address) {
        this.subjectFullName = Objects.requireNonNull(fullName, ERR_NULL_SUBJECT_FULL_NAME);
        this.subjectAddress = Objects.requireNonNull(address, ERR_NULL_SUBJECT_ADDRESS);
        return this;
    }

    /**
     * Set EU entity details (name and address).
     */
    public EuEntityAdminPermissionGrantBuilder euEntityDetails(String fullName, String address) {
        this.euEntityFullName = Objects.requireNonNull(fullName, ERR_NULL_EU_ENTITY_FULL_NAME);
        this.euEntityAddress = Objects.requireNonNull(address, ERR_NULL_EU_ENTITY_ADDRESS);
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public EuEntityAdminPermissionGrantBuilder toBuilder() {
        EuEntityAdminPermissionGrantBuilder copy = new EuEntityAdminPermissionGrantBuilder(this.fingerprintValue);
        copy.contextValue = this.contextValue;
        copy.description = this.description;
        copy.euEntityName = this.euEntityName;
        copy.subjectFullName = this.subjectFullName;
        copy.subjectAddress = this.subjectAddress;
        copy.euEntityFullName = this.euEntityFullName;
        copy.euEntityAddress = this.euEntityAddress;
        return copy;
    }

    /**
     * Build the EU entity admin permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantEuEntityAdmin()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public EuEntityAdministrationPermissionsGrantRequestRaw build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (contextValue == null) {
            throw new IllegalStateException(ERR_CONTEXT_REQUIRED);
        }
        if (euEntityName == null) {
            throw new IllegalStateException(ERR_EU_ENTITY_NAME_REQUIRED);
        }
        if (subjectFullName == null) {
            throw new IllegalStateException(ERR_SUBJECT_DETAILS_REQUIRED);
        }
        if (euEntityFullName == null) {
            throw new IllegalStateException(ERR_EU_ENTITY_DETAILS_REQUIRED);
        }

        EuEntityAdministrationPermissionsSubjectIdentifierRaw subjectId =
                new EuEntityAdministrationPermissionsSubjectIdentifierRaw()
                        .type(EuEntityAdministrationPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                        .value(fingerprintValue);

        EuEntityAdministrationPermissionsContextIdentifierRaw contextId =
                new EuEntityAdministrationPermissionsContextIdentifierRaw()
                        .type(EuEntityAdministrationPermissionsContextIdentifierTypeRaw.NIP_VAT_UE)
                        .value(contextValue);

        EntityByFingerprintDetailsRaw entityByFp = new EntityByFingerprintDetailsRaw()
                .fullName(subjectFullName)
                .address(subjectAddress);

        EuEntityPermissionSubjectDetailsRaw subjectDetails = new EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);

        EuEntityDetailsRaw euDetails = new EuEntityDetailsRaw()
                .fullName(euEntityFullName)
                .address(euEntityAddress);

        EuEntityAdministrationPermissionsGrantRequestRaw request =
                new EuEntityAdministrationPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setContextIdentifier(contextId);
        request.setDescription(description);
        request.setEuEntityName(euEntityName);
        request.setSubjectDetails(subjectDetails);
        request.setEuEntityDetails(euDetails);
        return request;
    }
}
