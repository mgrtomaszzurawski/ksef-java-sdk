/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for EU entity permission grant requests.
 * <p>Required: fingerprint, description (5-256 chars), subject details, at least one permission.
 *
 * @since 1.0.0
 */
public final class EuEntityPermissionGrantBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_SUBJECT_DETAILS_REQUIRED = "subject details are required — use .subjectEntityByFingerprint()";
    private static final String ERR_DESCRIPTION_REQUIRED = "description is required — use .description() before .build()";
    private static final String ERR_NULL_FINGERPRINT = "fingerprint is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_SUBJECT_FULL_NAME = "subject fullName is required";
    private static final String ERR_NULL_SUBJECT_ADDRESS = "subject address is required";

    private final String fingerprintValue;
    private @Nullable String description;
    private @Nullable String subjectFullName;
    private @Nullable String subjectAddress;
    private final List<EuEntityPermissionType> permissions = new ArrayList<>();

    private EuEntityPermissionGrantBuilder(String fingerprint) {
        this.fingerprintValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
    }

    public static EuEntityPermissionGrantBuilder forFingerprint(String fingerprint) {
        return new EuEntityPermissionGrantBuilder(fingerprint);
    }

    public EuEntityPermissionGrantBuilder description(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        return this;
    }

    public EuEntityPermissionGrantBuilder subjectEntityByFingerprint(String fullName, String address) {
        this.subjectFullName = Objects.requireNonNull(fullName, ERR_NULL_SUBJECT_FULL_NAME);
        this.subjectAddress = Objects.requireNonNull(address, ERR_NULL_SUBJECT_ADDRESS);
        return this;
    }

    public EuEntityPermissionGrantBuilder invoiceRead() {
        permissions.add(EuEntityPermissionType.INVOICE_READ);
        return this;
    }

    public EuEntityPermissionGrantBuilder invoiceWrite() {
        permissions.add(EuEntityPermissionType.INVOICE_WRITE);
        return this;
    }

    public EuEntityPermissionGrantBuilder toBuilder() {
        EuEntityPermissionGrantBuilder copy = new EuEntityPermissionGrantBuilder(this.fingerprintValue);
        copy.description = this.description;
        copy.subjectFullName = this.subjectFullName;
        copy.subjectAddress = this.subjectAddress;
        copy.permissions.addAll(this.permissions);
        return copy;
    }

    public EuEntityPermissionGrantRequest build() {
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
        return new EuEntityPermissionGrantRequest(fingerprintValue, description, subjectFullName, subjectAddress, permissions);
    }
}
