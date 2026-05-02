/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsRevokeRequestRaw;
import java.util.Objects;

/**
 * Builder for KSeF test permissions revocation requests.
 * <p>
 * Required: contextNip, authorizedIdentifier (type + value).
 * <p>
 * Usage:
 * <pre>{@code
 * TestDataPermissionsRevokeRequestRaw request = TestPermissionsRevokeBuilder
 *     .create("1234567890")
 *     .authorizedNip("0987654321")
 *     .build();
 * }</pre>
 */
public final class TestPermissionsRevokeBuilder {

    private static final String ERR_AUTHORIZED_REQUIRED = "authorized identifier must be set via authorizedNip(), authorizedPesel(), or authorizedFingerprint()";
    private static final String ERR_NULL_CONTEXT_NIP = "contextNip is required";
    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_PESEL = "pesel is required";
    private static final String ERR_NULL_FINGERPRINT = "fingerprint is required";

    private final String contextNip;
    private TestDataAuthorizedIdentifierTypeRaw authorizedType;
    private String authorizedValue;

    private TestPermissionsRevokeBuilder(String contextNip) {
        this.contextNip = Objects.requireNonNull(contextNip, ERR_NULL_CONTEXT_NIP);
    }

    /**
     * Create a builder for the given context NIP (the taxpayer revoking permissions).
     *
     * @param contextNip NIP of the context (taxpayer entity)
     */
    public static TestPermissionsRevokeBuilder create(String contextNip) {
        return new TestPermissionsRevokeBuilder(contextNip);
    }

    /**
     * Set the authorized identifier as a NIP.
     *
     * @param nip NIP of the entity whose permissions are revoked
     */
    public TestPermissionsRevokeBuilder authorizedNip(String nip) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.NIP;
        this.authorizedValue = Objects.requireNonNull(nip, ERR_NULL_NIP);
        return this;
    }

    /**
     * Set the authorized identifier as a PESEL.
     *
     * @param pesel PESEL of the person whose permissions are revoked
     */
    public TestPermissionsRevokeBuilder authorizedPesel(String pesel) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.PESEL;
        this.authorizedValue = Objects.requireNonNull(pesel, ERR_NULL_PESEL);
        return this;
    }

    /**
     * Set the authorized identifier as a certificate fingerprint.
     *
     * @param fingerprint certificate fingerprint of the entity whose permissions are revoked
     */
    public TestPermissionsRevokeBuilder authorizedFingerprint(String fingerprint) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.FINGERPRINT;
        this.authorizedValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public TestPermissionsRevokeBuilder toBuilder() {
        TestPermissionsRevokeBuilder copy = new TestPermissionsRevokeBuilder(this.contextNip);
        copy.authorizedType = this.authorizedType;
        copy.authorizedValue = this.authorizedValue;
        return copy;
    }

    /**
     * Build the permissions revocation request.
     *
     * @return the request ready to pass to {@code TestDataClient.revokePermissions()}
     * @throws IllegalStateException if validation fails
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public TestDataPermissionsRevokeRequestRaw build() {
        if (authorizedType == null || authorizedValue == null) {
            throw new IllegalStateException(ERR_AUTHORIZED_REQUIRED);
        }

        TestDataContextIdentifierRaw context = new TestDataContextIdentifierRaw();
        context.setType(TestDataContextIdentifierTypeRaw.NIP);
        context.setValue(contextNip);

        TestDataAuthorizedIdentifierRaw authorized = new TestDataAuthorizedIdentifierRaw();
        authorized.setType(authorizedType);
        authorized.setValue(authorizedValue);

        TestDataPermissionsRevokeRequestRaw request = new TestDataPermissionsRevokeRequestRaw();
        request.setContextIdentifier(context);
        request.setAuthorizedIdentifier(authorized);
        return request;
    }
}
