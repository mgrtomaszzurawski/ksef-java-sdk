/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionTypeRaw;

/**
 * Type of test data permission in KSeF.
 */
public enum TestDataPermissionType {

    INVOICE_READ,
    INVOICE_WRITE,
    INTROSPECTION,
    CREDENTIALS_READ,
    CREDENTIALS_MANAGE,
    ENFORCEMENT_OPERATIONS,
    SUBUNIT_MANAGE;

    public TestDataPermissionTypeRaw toRaw() {
        return switch (this) {
            case INVOICE_READ -> TestDataPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> TestDataPermissionTypeRaw.INVOICE_WRITE;
            case INTROSPECTION -> TestDataPermissionTypeRaw.INTROSPECTION;
            case CREDENTIALS_READ -> TestDataPermissionTypeRaw.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> TestDataPermissionTypeRaw.CREDENTIALS_MANAGE;
            case ENFORCEMENT_OPERATIONS -> TestDataPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
            case SUBUNIT_MANAGE -> TestDataPermissionTypeRaw.SUBUNIT_MANAGE;
        };
    }
}
