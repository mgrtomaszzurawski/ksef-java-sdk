/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthorizedIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsGrantRequestRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for KSeF test permissions grant requests.
 * <p>
 * Required: contextNip, authorizedIdentifier (type + value), at least one permission.
 * <p>
 * Usage:
 * <pre>{@code
 * TestDataPermissionsGrantRequestRaw request = TestPermissionsGrantBuilder
 *     .create("1234567890")
 *     .authorizedNip("0987654321")
 *     .invoiceRead()
 *     .invoiceWrite()
 *     .build();
 * }</pre>
 */
public final class TestPermissionsGrantBuilder {

    private static final String ERR_AUTHORIZED_REQUIRED = "authorized identifier must be set via authorizedNip(), authorizedPesel(), or authorizedFingerprint()";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";

    private final String contextNip;
    private TestDataAuthorizedIdentifierTypeRaw authorizedType;
    private String authorizedValue;
    private final List<TestDataPermissionRaw> permissions = new ArrayList<>();

    private TestPermissionsGrantBuilder(String contextNip) {
        this.contextNip = Objects.requireNonNull(contextNip, "contextNip is required");
    }

    /**
     * Create a builder for the given context NIP (the taxpayer granting permissions).
     *
     * @param contextNip NIP of the context (taxpayer entity)
     */
    public static TestPermissionsGrantBuilder create(String contextNip) {
        return new TestPermissionsGrantBuilder(contextNip);
    }

    /**
     * Set the authorized identifier as a NIP.
     *
     * @param nip NIP of the entity receiving permissions
     */
    public TestPermissionsGrantBuilder authorizedNip(String nip) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.NIP;
        this.authorizedValue = Objects.requireNonNull(nip, "nip is required");
        return this;
    }

    /**
     * Set the authorized identifier as a PESEL.
     *
     * @param pesel PESEL of the person receiving permissions
     */
    public TestPermissionsGrantBuilder authorizedPesel(String pesel) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.PESEL;
        this.authorizedValue = Objects.requireNonNull(pesel, "pesel is required");
        return this;
    }

    /**
     * Set the authorized identifier as a certificate fingerprint.
     *
     * @param fingerprint certificate fingerprint of the entity receiving permissions
     */
    public TestPermissionsGrantBuilder authorizedFingerprint(String fingerprint) {
        this.authorizedType = TestDataAuthorizedIdentifierTypeRaw.FINGERPRINT;
        this.authorizedValue = Objects.requireNonNull(fingerprint, "fingerprint is required");
        return this;
    }

    /**
     * Add a permission with the given type and description.
     *
     * @param permissionType the permission type
     * @param description human-readable description
     */
    public TestPermissionsGrantBuilder permission(TestDataPermissionTypeRaw permissionType, String description) {
        TestDataPermissionRaw perm = new TestDataPermissionRaw();
        perm.setPermissionType(Objects.requireNonNull(permissionType, "permissionType is required"));
        perm.setDescription(Objects.requireNonNull(description, "description is required"));
        permissions.add(perm);
        return this;
    }

    public TestPermissionsGrantBuilder invoiceRead() {
        return permission(TestDataPermissionTypeRaw.INVOICE_READ, "InvoiceRead");
    }

    public TestPermissionsGrantBuilder invoiceWrite() {
        return permission(TestDataPermissionTypeRaw.INVOICE_WRITE, "InvoiceWrite");
    }

    public TestPermissionsGrantBuilder credentialsRead() {
        return permission(TestDataPermissionTypeRaw.CREDENTIALS_READ, "CredentialsRead");
    }

    public TestPermissionsGrantBuilder credentialsManage() {
        return permission(TestDataPermissionTypeRaw.CREDENTIALS_MANAGE, "CredentialsManage");
    }

    public TestPermissionsGrantBuilder introspection() {
        return permission(TestDataPermissionTypeRaw.INTROSPECTION, "Introspection");
    }

    public TestPermissionsGrantBuilder enforcementOperations() {
        return permission(TestDataPermissionTypeRaw.ENFORCEMENT_OPERATIONS, "EnforcementOperations");
    }

    public TestPermissionsGrantBuilder subunitManage() {
        return permission(TestDataPermissionTypeRaw.SUBUNIT_MANAGE, "SubunitManage");
    }

    /**
     * Build the permissions grant request.
     *
     * @return the request ready to pass to {@code TestDataClient.grantPermissions()}
     * @throws IllegalStateException if validation fails
     */
    public TestDataPermissionsGrantRequestRaw build() {
        if (authorizedType == null || authorizedValue == null) {
            throw new IllegalStateException(ERR_AUTHORIZED_REQUIRED);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }

        TestDataContextIdentifierRaw context = new TestDataContextIdentifierRaw();
        context.setType(TestDataContextIdentifierTypeRaw.NIP);
        context.setValue(contextNip);

        TestDataAuthorizedIdentifierRaw authorized = new TestDataAuthorizedIdentifierRaw();
        authorized.setType(authorizedType);
        authorized.setValue(authorizedValue);

        TestDataPermissionsGrantRequestRaw request = new TestDataPermissionsGrantRequestRaw();
        request.setContextIdentifier(context);
        request.setAuthorizedIdentifier(authorized);
        request.setPermissions(new ArrayList<>(permissions));
        return request;
    }
}
