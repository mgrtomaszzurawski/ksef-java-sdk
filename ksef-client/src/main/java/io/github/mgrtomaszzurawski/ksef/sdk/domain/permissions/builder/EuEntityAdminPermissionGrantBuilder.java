/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityAdminPermissionGrantRequest;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for EU entity administration permission grant requests.
 * <p>Required: fingerprint, contextNipVatUe, description (5-256 chars),
 * euEntityName, subjectEntityByFingerprint, euEntityDetails.
 *
 * @since 1.0.0
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
    private @Nullable String contextValue;
    private @Nullable String description;
    private @Nullable String euEntityName;
    private @Nullable String subjectFullName;
    private @Nullable String subjectAddress;
    private @Nullable String euEntityFullName;
    private @Nullable String euEntityAddress;

    private EuEntityAdminPermissionGrantBuilder(String fingerprint) {
        this.fingerprintValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
    }

    public static EuEntityAdminPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new EuEntityAdminPermissionGrantBuilder(fingerprint);
    }

    public EuEntityAdminPermissionGrantBuilder contextNipVatUe(String nipVatUe) {
        this.contextValue = Objects.requireNonNull(nipVatUe, ERR_NULL_NIP_VAT_UE);
        return this;
    }

    public EuEntityAdminPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public EuEntityAdminPermissionGrantBuilder euEntityName(String euEntityName) {
        this.euEntityName = Objects.requireNonNull(euEntityName, ERR_NULL_EU_ENTITY_NAME);
        return this;
    }

    public EuEntityAdminPermissionGrantBuilder subjectEntityByFingerprint(String fullName, String address) {
        this.subjectFullName = Objects.requireNonNull(fullName, ERR_NULL_SUBJECT_FULL_NAME);
        this.subjectAddress = Objects.requireNonNull(address, ERR_NULL_SUBJECT_ADDRESS);
        return this;
    }

    public EuEntityAdminPermissionGrantBuilder euEntityDetails(String fullName, String address) {
        this.euEntityFullName = Objects.requireNonNull(fullName, ERR_NULL_EU_ENTITY_FULL_NAME);
        this.euEntityAddress = Objects.requireNonNull(address, ERR_NULL_EU_ENTITY_ADDRESS);
        return this;
    }

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

    public EuEntityAdminPermissionGrantRequest build() {
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
        if (subjectFullName == null || subjectAddress == null) {
            throw new IllegalStateException(ERR_SUBJECT_DETAILS_REQUIRED);
        }
        if (euEntityFullName == null || euEntityAddress == null) {
            throw new IllegalStateException(ERR_EU_ENTITY_DETAILS_REQUIRED);
        }
        return new EuEntityAdminPermissionGrantRequest(fingerprintValue, contextValue, description,
                euEntityName, subjectFullName, subjectAddress, euEntityFullName, euEntityAddress);
    }
}
