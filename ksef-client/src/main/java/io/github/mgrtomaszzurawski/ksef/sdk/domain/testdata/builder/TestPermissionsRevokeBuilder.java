/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest;
import java.util.Objects;

/**
 * Builder for KSeF test permissions revocation requests.
 * <p>Required: contextNip, authorizedIdentifier (type + value).
 */
public final class TestPermissionsRevokeBuilder {

    private static final String ERR_AUTHORIZED_REQUIRED = "authorized identifier must be set via authorizedNip(), authorizedPesel(), or authorizedFingerprint()";
    private static final String ERR_NULL_CONTEXT_NIP = "contextNip is required";
    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_PESEL = "pesel is required";
    private static final String ERR_NULL_FINGERPRINT = "fingerprint is required";

    private final String contextNip;
    private TestDataAuthorizedIdentifierType authorizedType;
    private String authorizedValue;

    private TestPermissionsRevokeBuilder(String contextNip) {
        this.contextNip = Objects.requireNonNull(contextNip, ERR_NULL_CONTEXT_NIP);
    }

    public static TestPermissionsRevokeBuilder create(String contextNip) {
        return new TestPermissionsRevokeBuilder(contextNip);
    }

    public TestPermissionsRevokeBuilder authorizedNip(String nip) {
        this.authorizedType = TestDataAuthorizedIdentifierType.NIP;
        this.authorizedValue = Objects.requireNonNull(nip, ERR_NULL_NIP);
        return this;
    }

    public TestPermissionsRevokeBuilder authorizedPesel(String pesel) {
        this.authorizedType = TestDataAuthorizedIdentifierType.PESEL;
        this.authorizedValue = Objects.requireNonNull(pesel, ERR_NULL_PESEL);
        return this;
    }

    public TestPermissionsRevokeBuilder authorizedFingerprint(String fingerprint) {
        this.authorizedType = TestDataAuthorizedIdentifierType.FINGERPRINT;
        this.authorizedValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
        return this;
    }

    public TestPermissionsRevokeBuilder toBuilder() {
        TestPermissionsRevokeBuilder copy = new TestPermissionsRevokeBuilder(this.contextNip);
        copy.authorizedType = this.authorizedType;
        copy.authorizedValue = this.authorizedValue;
        return copy;
    }

    public TestPermissionsRevokeRequest build() {
        if (authorizedType == null || authorizedValue == null) {
            throw new IllegalStateException(ERR_AUTHORIZED_REQUIRED);
        }
        return new TestPermissionsRevokeRequest(contextNip, authorizedType, authorizedValue);
    }
}
