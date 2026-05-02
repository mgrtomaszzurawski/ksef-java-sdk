/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsSubjectIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import java.util.ArrayList;
import java.util.List;

/**
 * SDK-record → generated {@code *Raw} mappers for permissions requests.
 * Lives in a non-exported package; consumers can't reach it.
 */
public final class PermissionsRequestMappers {

    private PermissionsRequestMappers() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw toSubunitPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierRaw()
                .type(toSubunitSubjectIdentifierTypeRaw(req.identifierType()))
                .value(req.identifierValue());
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierRaw()
                .type(toSubunitContextIdentifierTypeRaw(req.contextType()))
                .value(req.contextValue());
        var personDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw()
                .firstName(req.firstName())
                .lastName(req.lastName());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setContextIdentifier(contextId);
        raw.setDescription(req.description());
        raw.setSubjectDetails(subjectDetails);
        if (req.subunitName() != null) {
            raw.setSubunitName(req.subunitName());
        }
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierTypeRaw toSubunitSubjectIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierTypeRaw.NIP;
            case PESEL -> io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierTypeRaw toSubunitContextIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitContextIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw toEntityAuthorizationPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierRaw()
                .type(toEntityAuthorizationIdentifierTypeRaw(req.identifierType()))
                .value(req.identifierValue());
        var entityDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw().fullName(req.fullName());
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermission(toEntityAuthorizationPermissionTypeRaw(req.permission()));
        raw.setDescription(req.description());
        raw.setSubjectDetails(entityDetails);
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierTypeRaw toEntityAuthorizationIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierTypeRaw.NIP;
            case PEPPOL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierTypeRaw.PEPPOL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw toEntityAuthorizationPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionType value) {
        return switch (value) {
            case SELF_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw.SELF_INVOICING;
            case RR_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw.RR_INVOICING;
            case TAX_REPRESENTATIVE -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw.TAX_REPRESENTATIVE;
            case PEF_INVOICING -> io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionTypeRaw.PEF_INVOICING;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw toEuEntityPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                .value(req.fingerprintValue());
        var entityByFp = new io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw()
                .fullName(req.subjectFullName())
                .address(req.subjectAddress());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw>(req.permissions().size());
        for (var type : req.permissions()) {
            perms.add(toEuEntityPermissionTypeRaw(type));
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(req.description());
        raw.setSubjectDetails(subjectDetails);
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw toEuEntityPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw.INVOICE_WRITE;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw toEntityPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierTypeRaw.NIP)
                .value(req.identifierValue());
        var entityDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw()
                .fullName(req.fullName());
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionRaw>(req.permissions().size());
        for (var entry : req.permissions()) {
            var pr = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionRaw().type(toEntityPermissionTypeRaw(entry.type()));
            if (entry.canDelegate()) {
                pr.canDelegate(true);
            }
            perms.add(pr);
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(req.description());
        raw.setSubjectDetails(entityDetails);
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionTypeRaw toEntityPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionTypeRaw.INVOICE_WRITE;
        };
    }

    public static PersonPermissionsGrantRequestRaw toPersonPermissionsGrantRequestRaw(PersonPermissionGrantRequest req) {
        PersonPermissionsSubjectIdentifierRaw subjectId = new PersonPermissionsSubjectIdentifierRaw()
                .type(toPersonSubjectIdentifierTypeRaw(req.identifierType()))
                .value(req.identifierValue());
        PersonDetailsRaw personDetails = new PersonDetailsRaw()
                .firstName(req.firstName())
                .lastName(req.lastName());
        PersonPermissionSubjectDetailsRaw subjectDetails = new PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        List<PersonPermissionTypeRaw> permTypes = new ArrayList<>(req.permissions().size());
        for (PersonPermissionType type : req.permissions()) {
            permTypes.add(toPersonPermissionTypeRaw(type));
        }
        PersonPermissionsGrantRequestRaw raw = new PersonPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(permTypes);
        raw.setDescription(req.description());
        raw.setSubjectDetails(subjectDetails);
        return raw;
    }

    public static PersonPermissionsSubjectIdentifierTypeRaw toPersonSubjectIdentifierTypeRaw(PersonSubjectIdentifierType value) {
        return switch (value) {
            case NIP -> PersonPermissionsSubjectIdentifierTypeRaw.NIP;
            case PESEL -> PersonPermissionsSubjectIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> PersonPermissionsSubjectIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static PersonPermissionTypeRaw toPersonPermissionTypeRaw(PersonPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> PersonPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> PersonPermissionTypeRaw.INVOICE_WRITE;
            case CREDENTIALS_READ -> PersonPermissionTypeRaw.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> PersonPermissionTypeRaw.CREDENTIALS_MANAGE;
            case SUBUNIT_MANAGE -> PersonPermissionTypeRaw.SUBUNIT_MANAGE;
            case ENFORCEMENT_OPERATIONS -> PersonPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
            case INTROSPECTION -> PersonPermissionTypeRaw.INTROSPECTION;
        };
    }
}
