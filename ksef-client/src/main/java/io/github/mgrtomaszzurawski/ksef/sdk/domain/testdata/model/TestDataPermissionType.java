/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

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

}
