/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierTypeRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for EU entity permission grant requests.
 * <p>
 * Required: subject fingerprint, description (5-256 chars), at least one permission,
 * subject details (entity by fingerprint).
 * <p>
 * Usage:
 * <pre>{@code
 * var builder = EuEntityPermissionGrantBuilder
 *     .forFingerprint("ABC123DEF456")
 *     .description("EU entity invoice access")
 *     .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
 *     .invoiceRead()
 *     .invoiceWrite();
 * }</pre>
 */
public final class EuEntityPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "subject details are required — use .subjectEntityByFingerprint()";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";

    private final String fingerprintValue;
    private String description;
    private String subjectFullName;
    private String subjectAddress;
    private final List<EuEntityPermissionTypeRaw> permissions = new ArrayList<>();

    private EuEntityPermissionGrantBuilder(String fingerprint) {
        this.fingerprintValue = Objects.requireNonNull(fingerprint, "fingerprint is required");
    }

    /**
     * Grant EU entity permissions to a subject identified by certificate fingerprint.
     */
    public static EuEntityPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new EuEntityPermissionGrantBuilder(fingerprint);
    }

    public EuEntityPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, "description is required");
        return this;
    }

    /**
     * Set subject details as entity identified by fingerprint.
     */
    public EuEntityPermissionGrantBuilder subjectEntityByFingerprint(String fullName, String address) {
        this.subjectFullName = Objects.requireNonNull(fullName, "subject fullName is required");
        this.subjectAddress = Objects.requireNonNull(address, "subject address is required");
        return this;
    }

    public EuEntityPermissionGrantBuilder invoiceRead() {
        permissions.add(EuEntityPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public EuEntityPermissionGrantBuilder invoiceWrite() {
        permissions.add(EuEntityPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    /**
     * Build the EU entity permission grant request.
     *
     * @return the request ready to pass to {@code PermissionClient.grantEuEntity()}
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public EuEntityPermissionsGrantRequestRaw build() {
        Objects.requireNonNull(description, ERR_DESCRIPTION_REQUIRED);
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }
        if (subjectFullName == null) {
            throw new IllegalStateException(ERR_SUBJECT_DETAILS_REQUIRED);
        }

        EuEntityPermissionsSubjectIdentifierRaw subjectId = new EuEntityPermissionsSubjectIdentifierRaw()
                .type(EuEntityPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                .value(fingerprintValue);

        EntityByFingerprintDetailsRaw entityByFp = new EntityByFingerprintDetailsRaw()
                .fullName(subjectFullName)
                .address(subjectAddress);

        EuEntityPermissionSubjectDetailsRaw subjectDetails = new EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);

        EuEntityPermissionsGrantRequestRaw request = new EuEntityPermissionsGrantRequestRaw();
        request.setSubjectIdentifier(subjectId);
        request.setPermissions(new ArrayList<>(permissions));
        request.setDescription(description);
        request.setSubjectDetails(subjectDetails);
        return request;
    }
}
