/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthenticationContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;

/**
 * Internal mappers for testdata domain types. Non-exported package.
 */
public final class TestdataMappers {

    private TestdataMappers() { }

    public static TestDataAuthenticationContextIdentifierTypeRaw toTestDataAuthenticationContextIdentifierTypeRaw(TestDataIdentifierType value) {
            return switch (value) {
                case NIP -> TestDataAuthenticationContextIdentifierTypeRaw.NIP;
                case INTERNAL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.INTERNAL_ID;
                case NIP_VAT_UE -> TestDataAuthenticationContextIdentifierTypeRaw.NIP_VAT_UE;
                case PEPPOL_ID -> TestDataAuthenticationContextIdentifierTypeRaw.PEPPOL_ID;
            };
        
    }

    public static TestDataPermissionTypeRaw toTestDataPermissionTypeRaw(TestDataPermissionType value) {
            return switch (value) {
                case INVOICE_READ -> TestDataPermissionTypeRaw.INVOICE_READ;
                case INVOICE_WRITE -> TestDataPermissionTypeRaw.INVOICE_WRITE;
                case INTROSPECTION -> TestDataPermissionTypeRaw.INTROSPECTION;
                case CREDENTIALS_READ -> TestDataPermissionTypeRaw.CREDENTIALS_READ;
                case CREDENTIALS_MANAGE -> TestDataPermissionTypeRaw.CREDENTIALS_MANAGE;
                case ENFORCEMENT_OPERATIONS -> TestDataPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
                case SUBUNIT_MANAGE -> TestDataPermissionTypeRaw.SUBUNIT_MANAGE;
            };
        
    }

    public static SubjectIdentifierTypeRaw toSubjectIdentifierTypeRaw(TestSubjectIdentifierType value) {
            return switch (value) {
                case NIP -> SubjectIdentifierTypeRaw.NIP;
                case PESEL -> SubjectIdentifierTypeRaw.PESEL;
                case FINGERPRINT -> SubjectIdentifierTypeRaw.FINGERPRINT;
            };
        
    }

    public static SubjectTypeRaw toSubjectTypeRaw(TestSubjectType value) {
            return switch (value) {
                case ENFORCEMENT_AUTHORITY -> SubjectTypeRaw.ENFORCEMENT_AUTHORITY;
                case VAT_GROUP -> SubjectTypeRaw.VAT_GROUP;
                case JST -> SubjectTypeRaw.JST;
            };
        
    }

}
