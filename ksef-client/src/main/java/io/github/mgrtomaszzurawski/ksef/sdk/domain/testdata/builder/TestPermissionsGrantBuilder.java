/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for KSeF test permissions grant requests.
 * <p>Required: contextNip, authorizedIdentifier, at least one permission.
 *
 * @since 0.1.0
 */
public final class TestPermissionsGrantBuilder {

    private static final String ERR_AUTHORIZED_REQUIRED = "authorized identifier must be set via authorizedNip(), authorizedPesel(), or authorizedFingerprint()";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_NULL_CONTEXT_NIP = "contextNip is required";
    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_PESEL = "pesel is required";
    private static final String ERR_NULL_FINGERPRINT = "fingerprint is required";
    private static final String ERR_NULL_PERMISSION_TYPE = "permissionType is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";

    private static final String DESC_INVOICE_READ = "InvoiceRead";
    private static final String DESC_INVOICE_WRITE = "InvoiceWrite";
    private static final String DESC_CREDENTIALS_READ = "CredentialsRead";
    private static final String DESC_CREDENTIALS_MANAGE = "CredentialsManage";
    private static final String DESC_INTROSPECTION = "Introspection";
    private static final String DESC_ENFORCEMENT_OPERATIONS = "EnforcementOperations";
    private static final String DESC_SUBUNIT_MANAGE = "SubunitManage";

    private final String contextNip;
    private @Nullable TestDataAuthorizedIdentifierType authorizedType;
    private @Nullable String authorizedValue;
    private final List<TestDataPermission> permissions = new ArrayList<>();

    private TestPermissionsGrantBuilder(String contextNip) {
        this.contextNip = Objects.requireNonNull(contextNip, ERR_NULL_CONTEXT_NIP);
    }

    public static TestPermissionsGrantBuilder create(String contextNip) {
        return new TestPermissionsGrantBuilder(contextNip);
    }

    public TestPermissionsGrantBuilder authorizedNip(String nip) {
        this.authorizedType = TestDataAuthorizedIdentifierType.NIP;
        this.authorizedValue = Objects.requireNonNull(nip, ERR_NULL_NIP);
        return this;
    }

    public TestPermissionsGrantBuilder authorizedPesel(String pesel) {
        this.authorizedType = TestDataAuthorizedIdentifierType.PESEL;
        this.authorizedValue = Objects.requireNonNull(pesel, ERR_NULL_PESEL);
        return this;
    }

    public TestPermissionsGrantBuilder authorizedFingerprint(String fingerprint) {
        this.authorizedType = TestDataAuthorizedIdentifierType.FINGERPRINT;
        this.authorizedValue = Objects.requireNonNull(fingerprint, ERR_NULL_FINGERPRINT);
        return this;
    }

    public TestPermissionsGrantBuilder permission(TestDataPermissionType permissionType, String description) {
        permissions.add(new TestDataPermission(
                Objects.requireNonNull(permissionType, ERR_NULL_PERMISSION_TYPE),
                Objects.requireNonNull(description, ERR_NULL_DESCRIPTION)));
        return this;
    }

    public TestPermissionsGrantBuilder invoiceRead() {
        return permission(TestDataPermissionType.INVOICE_READ, DESC_INVOICE_READ);
    }

    public TestPermissionsGrantBuilder invoiceWrite() {
        return permission(TestDataPermissionType.INVOICE_WRITE, DESC_INVOICE_WRITE);
    }

    public TestPermissionsGrantBuilder credentialsRead() {
        return permission(TestDataPermissionType.CREDENTIALS_READ, DESC_CREDENTIALS_READ);
    }

    public TestPermissionsGrantBuilder credentialsManage() {
        return permission(TestDataPermissionType.CREDENTIALS_MANAGE, DESC_CREDENTIALS_MANAGE);
    }

    public TestPermissionsGrantBuilder introspection() {
        return permission(TestDataPermissionType.INTROSPECTION, DESC_INTROSPECTION);
    }

    public TestPermissionsGrantBuilder enforcementOperations() {
        return permission(TestDataPermissionType.ENFORCEMENT_OPERATIONS, DESC_ENFORCEMENT_OPERATIONS);
    }

    public TestPermissionsGrantBuilder subunitManage() {
        return permission(TestDataPermissionType.SUBUNIT_MANAGE, DESC_SUBUNIT_MANAGE);
    }

    public TestPermissionsGrantBuilder toBuilder() {
        TestPermissionsGrantBuilder copy = new TestPermissionsGrantBuilder(this.contextNip);
        copy.authorizedType = this.authorizedType;
        copy.authorizedValue = this.authorizedValue;
        copy.permissions.addAll(this.permissions);
        return copy;
    }

    public TestPermissionsGrantRequest build() {
        if (authorizedType == null || authorizedValue == null) {
            throw new IllegalStateException(ERR_AUTHORIZED_REQUIRED);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }
        return new TestPermissionsGrantRequest(contextNip, authorizedType, authorizedValue, permissions);
    }
}
