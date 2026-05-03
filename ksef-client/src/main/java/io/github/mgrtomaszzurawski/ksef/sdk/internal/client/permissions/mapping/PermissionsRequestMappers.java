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

    public static io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw toEntityAuthorizationPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionsQueryRequest req) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw();
        raw.setQueryType(toAuthorizationQueryTypeRaw(req.queryType()));
        if (req.authorizingNip() != null) {
            var authorizingId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierRaw()
                    .type(io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizingEntityIdentifierTypeRaw.NIP)
                    .value(req.authorizingNip());
            raw.setAuthorizingIdentifier(authorizingId);
        }
        if (req.authorizedType() != null) {
            var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationsAuthorizedEntityIdentifierRaw()
                    .type(toAuthorizedEntityIdentifierTypeRaw(req.authorizedType()))
                    .value(req.authorizedValue());
            raw.setAuthorizedIdentifier(authorizedId);
        }
        if (!req.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.InvoicePermissionTypeRaw>(req.permissionTypes().size());
            for (var type : req.permissionTypes()) {
                perms.add(toInvoicePermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        return raw;
    }

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

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw toPersonPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest req) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw();
        raw.setQueryType(toPersonPermissionsQueryTypeRaw(req.queryType()));
        if (req.authorType() != null) {
            var authorId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorIdentifierRaw()
                    .type(toPersonAuthorIdentifierTypeRaw(req.authorType()));
            if (req.authorValue() != null) {
                authorId.value(req.authorValue());
            }
            raw.setAuthorIdentifier(authorId);
        }
        if (req.authorizedType() != null) {
            var authorizedId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsAuthorizedIdentifierRaw()
                    .type(toPersonAuthorizedIdentifierTypeRaw(req.authorizedType()))
                    .value(req.authorizedValue());
            raw.setAuthorizedIdentifier(authorizedId);
        }
        if (req.contextType() != null) {
            var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsContextIdentifierRaw()
                    .type(toPersonPermissionsContextIdentifierTypeRaw(req.contextType()))
                    .value(req.contextValue());
            raw.setContextIdentifier(contextId);
        }
        if (req.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsTargetIdentifierRaw()
                    .type(toPersonPermissionsTargetIdentifierTypeRaw(req.targetType()));
            if (req.targetValue() != null) {
                targetId.value(req.targetValue());
            }
            raw.setTargetIdentifier(targetId);
        }
        if (!req.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionTypeRaw>(req.permissionTypes().size());
            for (var type : req.permissionTypes()) {
                perms.add(toPersonPermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        if (req.permissionState() != null) {
            raw.setPermissionState(toPermissionStateRaw(req.permissionState()));
        }
        return raw;
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

    public static io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw toPersonalPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest req) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw();
        if (req.contextType() != null) {
            var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsContextIdentifierRaw()
                    .type(toPersonalContextIdentifierTypeRaw(req.contextType()))
                    .value(req.contextValue());
            raw.setContextIdentifier(contextId);
        }
        if (req.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsTargetIdentifierRaw()
                    .type(toPersonalTargetIdentifierTypeRaw(req.targetType()));
            if (req.targetValue() != null) {
                targetId.value(req.targetValue());
            }
            raw.setTargetIdentifier(targetId);
        }
        if (!req.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionTypeRaw>(req.permissionTypes().size());
            for (var type : req.permissionTypes()) {
                perms.add(toPersonalPermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        if (req.permissionState() != null) {
            raw.setPermissionState(toPermissionStateRaw(req.permissionState()));
        }
        return raw;
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

    public static io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw toEuEntityPermissionsQueryRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionsQueryRequest req) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw();
        if (req.vatUeIdentifier() != null) {
            raw.setVatUeIdentifier(req.vatUeIdentifier());
        }
        if (req.authorizedFingerprintIdentifier() != null) {
            raw.setAuthorizedFingerprintIdentifier(req.authorizedFingerprintIdentifier());
        }
        if (!req.permissionTypes().isEmpty()) {
            var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryPermissionTypeRaw>(req.permissionTypes().size());
            for (var type : req.permissionTypes()) {
                perms.add(toEuEntityQueryPermissionTypeRaw(type));
            }
            raw.setPermissionTypes(perms);
        }
        return raw;
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

    public static io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw toIndirectPermissionsGrantRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsSubjectIdentifierRaw()
                .type(toIndirectSubjectIdentifierTypeRaw(req.identifierType()))
                .value(req.identifierValue());
        var personDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonDetailsRaw()
                .firstName(req.firstName())
                .lastName(req.lastName());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionSubjectDetailsTypeRaw.PERSON_BY_IDENTIFIER)
                .personById(personDetails);
        var perms = new ArrayList<io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionTypeRaw>(req.permissions().size());
        for (var type : req.permissions()) {
            perms.add(toIndirectPermissionTypeRaw(type));
        }
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setPermissions(perms);
        raw.setDescription(req.description());
        raw.setSubjectDetails(subjectDetails);
        if (req.targetType() != null) {
            var targetId = new io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsTargetIdentifierRaw()
                    .type(toIndirectTargetIdentifierTypeRaw(req.targetType()));
            if (req.targetValue() != null) {
                targetId.value(req.targetValue());
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
            io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityAdminPermissionGrantRequest req) {
        var subjectId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsSubjectIdentifierTypeRaw.FINGERPRINT)
                .value(req.fingerprintValue());
        var contextId = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierRaw()
                .type(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsContextIdentifierTypeRaw.NIP_VAT_UE)
                .value(req.contextValue());
        var entityByFp = new io.github.mgrtomaszzurawski.ksef.client.model.EntityByFingerprintDetailsRaw()
                .fullName(req.subjectFullName())
                .address(req.subjectAddress());
        var subjectDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsRaw()
                .subjectDetailsType(io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionSubjectDetailsTypeRaw.ENTITY_BY_FINGERPRINT)
                .entityByFp(entityByFp);
        var euDetails = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityDetailsRaw()
                .fullName(req.euEntityFullName())
                .address(req.euEntityAddress());
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw();
        raw.setSubjectIdentifier(subjectId);
        raw.setContextIdentifier(contextId);
        raw.setDescription(req.description());
        raw.setEuEntityName(req.euEntityName());
        raw.setSubjectDetails(subjectDetails);
        raw.setEuEntityDetails(euDetails);
        return raw;
    }

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
