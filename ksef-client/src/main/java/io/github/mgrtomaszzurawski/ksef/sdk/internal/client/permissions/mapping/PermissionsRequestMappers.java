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

                                                                        public static io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw toIndirectPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierRaw()
                .type(toIndirectSubjectIdentifierTypeRaw(request.identifierType()))
                .value(request.identifierValue());
        var personDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw()
                .firstName(request.firstName())
                .lastName(request.lastName());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw>(request.permissions().size());
        for (var type : request.permissions()) {
            perms.add(toIndirectPermissionTypeRaw(type));
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(request.description());
        raw.setSubjectDetails(subjectDetails);
        if (request.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierRaw()
                    .type(toIndirectTargetIdentifierTypeRaw(request.targetType()));
            if (request.targetValue() != null) {
                targetId.value(request.targetValue());
            }
            raw.setTargetIdentifier(targetId);
        }
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierTypeRaw toIndirectSubjectIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierTypeRaw.NIP;
            case PESEL -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierTypeRaw.PESEL;
            case FINGERPRINT -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierTypeRaw.FINGERPRINT;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierTypeRaw toIndirectTargetIdentifierTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectTargetIdentifierType value) {
        return switch (value) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierTypeRaw.NIP;
            case ALL_PARTNERS -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierTypeRaw.ALL_PARTNERS;
            case INTERNAL_ID -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierTypeRaw.INTERNAL_ID;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw toIndirectPermissionTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw.INVOICE_WRITE;
        };
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw toEuEntityAdministrationPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityAdminPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                .value(request.fingerprintValue());
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierTypeRaw.NIP_VAT_UE)
                .value(request.contextValue());
        var entityByFp = new io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw()
                .fullName(request.subjectFullName())
                .address(request.subjectAddress());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);
        var euDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityDetailsRaw()
                .fullName(request.euEntityFullName())
                .address(request.euEntityAddress());
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setContextIdentifier(contextId);
        raw.setDescription(request.description());
        raw.setEuEntityName(request.euEntityName());
        raw.setSubjectDetails(subjectDetails);
        raw.setEuEntityDetails(euDetails);
        return raw;
    }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw toSubunitPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsSubjectIdentifierRaw()
                .type(toSubunitSubjectIdentifierTypeRaw(request.identifierType()))
                .value(request.identifierValue());
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsContextIdentifierRaw()
                .type(toSubunitContextIdentifierTypeRaw(request.contextType()))
                .value(request.contextValue());
        var personDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw()
                .firstName(request.firstName())
                .lastName(request.lastName());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setContextIdentifier(contextId);
        raw.setDescription(request.description());
        raw.setSubjectDetails(subjectDetails);
        if (request.subunitName() != null) {
            raw.setSubunitName(request.subunitName());
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
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsSubjectIdentifierRaw()
                .type(toEntityAuthorizationIdentifierTypeRaw(request.identifierType()))
                .value(request.identifierValue());
        var entityDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw().fullName(request.fullName());
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermission(toEntityAuthorizationPermissionTypeRaw(request.permission()));
        raw.setDescription(request.description());
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
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                .value(request.fingerprintValue());
        var entityByFp = new io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw()
                .fullName(request.subjectFullName())
                .address(request.subjectAddress());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionTypeRaw>(request.permissions().size());
        for (var type : request.permissions()) {
            perms.add(toEuEntityPermissionTypeRaw(type));
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(request.description());
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
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionGrantRequest request) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsSubjectIdentifierTypeRaw.NIP)
                .value(request.identifierValue());
        var entityDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EntityDetailsRaw()
                .fullName(request.fullName());
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionRaw>(request.permissions().size());
        for (var entry : request.permissions()) {
            var permRaw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionRaw().type(toEntityPermissionTypeRaw(entry.type()));
            if (entry.canDelegate()) {
                permRaw.canDelegate(true);
            }
            perms.add(permRaw);
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(request.description());
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

    public static PersonPermissionsGrantRequestRaw toPersonPermissionsGrantRequestRaw(PersonPermissionGrantRequest request) {
        PersonPermissionsSubjectIdentifierRaw subjectId = new PersonPermissionsSubjectIdentifierRaw()
                .type(toPersonSubjectIdentifierTypeRaw(request.identifierType()))
                .value(request.identifierValue());
        PersonDetailsRaw personDetails = new PersonDetailsRaw()
                .firstName(request.firstName())
                .lastName(request.lastName());
        PersonPermissionSubjectDetailsRaw subjectDetails = new PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        List<PersonPermissionTypeRaw> permTypes = new ArrayList<>(request.permissions().size());
        for (PersonPermissionType type : request.permissions()) {
            permTypes.add(toPersonPermissionTypeRaw(type));
        }
        PersonPermissionsGrantRequestRaw raw = new PersonPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(permTypes);
        raw.setDescription(request.description());
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
