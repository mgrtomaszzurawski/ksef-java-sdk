/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

/**
 * SDK enum → {@code *Raw} converters used by permission query/grant mappers.
 * Lives in a non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
 */
final class PermissionEnumConverters {

    private PermissionEnumConverters() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.QueryTypeRaw toAuthorizationQueryTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AuthorizationQueryType value) {
        return switch (value) {
            case GRANTED -> io.github.mgrtomaszzurawski.ksef.client.model.QueryTypeRaw.GRANTED;
            case RECEIVED -> io.github.mgrtomaszzurawski.ksef.client.model.QueryTypeRaw.RECEIVED;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw toAuthorizedEntityIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw.NIP;
            case PEPPOL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierTypeRaw.PEPPOL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw toInvoicePermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionType value) {
        return switch (value) {
            case SELF_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw.SELF_INVOICING;
            case RR_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw.RR_INVOICING;
            case TAX_REPRESENTATIVE -> io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw.TAX_REPRESENTATIVE;
            case PEF_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw.PEF_INVOICING;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryTypeRaw toPersonPermissionsQueryTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryType value) {
        return switch (value) {
            case PERMISSIONS_IN_CURRENT_CONTEXT -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryTypeRaw.PERMISSIONS_IN_CURRENT_CONTEXT;
            case PERMISSIONS_GRANTED_IN_CURRENT_CONTEXT -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryTypeRaw.PERMISSIONS_GRANTED_IN_CURRENT_CONTEXT;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw toPersonAuthorIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonAuthorIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw.NIP;
            case PESEL -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw.FINGERPRINT;
            case SYSTEM -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierTypeRaw.SYSTEM;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierTypeRaw toPersonAuthorizedIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierTypeRaw.NIP;
            case PESEL -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierTypeRaw toPersonPermissionsContextIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalContextIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierTypeRaw toPersonPermissionsTargetIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalTargetIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierTypeRaw.NIP;
            case ALL_PARTNERS -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierTypeRaw toPersonalContextIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalContextIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierTypeRaw toPersonalTargetIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalTargetIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierTypeRaw.NIP;
            case ALL_PARTNERS -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw toPersonalPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.INVOICE_WRITE;
            case CREDENTIALS_READ -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.CREDENTIALS_MANAGE;
            case SUBUNIT_MANAGE -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.SUBUNIT_MANAGE;
            case ENFORCEMENT_OPERATIONS -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
            case INTROSPECTION -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.INTROSPECTION;
            case VAT_UE_MANAGE -> io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw.VAT_UE_MANAGE;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.PermissionStateRaw toPermissionStateRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionState value) {
        return switch (value) {
            case ACTIVE -> io.github.mgrtomaszzurawski.ksef.client.model.PermissionStateRaw.ACTIVE;
            case INACTIVE -> io.github.mgrtomaszzurawski.ksef.client.model.PermissionStateRaw.INACTIVE;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw toEuEntityQueryPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityQueryPermissionType value) {
        return switch (value) {
            case VAT_UE_MANAGE -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw.VAT_UE_MANAGE;
            case INVOICE_READ -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw.INVOICE_WRITE;
            case INTROSPECTION -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw.INTROSPECTION;
        };
    }

}
