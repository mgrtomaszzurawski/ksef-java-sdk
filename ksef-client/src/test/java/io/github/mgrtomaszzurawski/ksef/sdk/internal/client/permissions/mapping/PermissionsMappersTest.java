/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationGrantRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityRoleRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRoleRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationGrant;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRole;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermission;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRole;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermission;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Branch coverage for {@link PermissionsMappers} singular-record converters.
 *
 * <p>The plural wrappers (e.g. {@code toEntityPermissions(QueryEntityPermissionsResponseRaw)})
 * are already covered by the WireMock {@code PermissionClientTest} suite which
 * stubs empty {@code permissions: []} responses; those tests never exercise the
 * singular per-element mappers because the streams iterate empty lists. This
 * suite drives each singular mapper with a fully-populated {@code Raw} so the
 * non-null branches of its three identifier-coalescing patterns
 * ({@code ctxRaw}, {@code authzRaw}, {@code targetRaw}, etc.) all execute.
 *
 * <p>The {@code Raw} types come from the OpenAPI generator and ship JsonCreator
 * factories — so we drive them from JSON via the same {@link ObjectMapper} the
 * runtime uses, rather than calling 5-deep nested constructors by hand. That
 * also exercises the JsonProperty annotations the way real KSeF responses
 * would.
 */
class PermissionsMappersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());

    private static final String GRANT_ID = "grant-1";
    private static final String DESCRIPTION = "test grant";
    private static final String START_DATE_ISO = "2026-04-01T10:00:00+02:00";
    private static final String NIP_VALUE = "1111111111";
    private static final String PESEL_VALUE = "82060411457";
    private static final String FINGERPRINT_VALUE = "ABC123DEF456";
    private static final String VAT_UE = "DE123456789";
    private static final String EU_ENTITY_NAME = "EU Test Entity";
    private static final String SUBUNIT_NAME = "Subunit Test";
    private static final String FIRST_NAME = "Jan";
    private static final String LAST_NAME = "Kowalski";
    private static final String ENTITY_FULL_NAME = "Acme Sp. z o.o.";

    @Test
    void toEntityAuthorizationGrant_withAuthorIdentifier_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizedEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizingEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizationScope": "SelfInvoicing",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(GRANT_ID, NIP_VALUE, NIP_VALUE, NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        EntityAuthorizationGrantRaw raw = OBJECT_MAPPER.readValue(json, EntityAuthorizationGrantRaw.class);

        // when
        EntityAuthorizationGrant result = PermissionsMappers.toEntityAuthorizationGrant(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertNotNull(result.authorIdentifier());
        assertEquals(NIP_VALUE, result.authorIdentifier().value());
        assertNotNull(result.authorizedEntityIdentifier());
        assertNotNull(result.authorizingEntityIdentifier());
        assertEquals("SelfInvoicing", result.authorizationScope());
        assertEquals(DESCRIPTION, result.description());
        assertNotNull(result.startDate());
    }

    @Test
    void toEntityAuthorizationGrant_withoutAuthorIdentifier_yieldsNullAuthor() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorIdentifier": null,
                  "authorizedEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizingEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizationScope": "TaxRepresentative",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(GRANT_ID, NIP_VALUE, NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        EntityAuthorizationGrantRaw raw = OBJECT_MAPPER.readValue(json, EntityAuthorizationGrantRaw.class);

        // when
        EntityAuthorizationGrant result = PermissionsMappers.toEntityAuthorizationGrant(raw);

        // then
        assertNull(result.authorIdentifier(), "null authorIdentifier branch must yield null mapped author");
    }

    @Test
    void toEntityPermission_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "contextIdentifier": {"type": "Nip", "value": "%s"},
                  "permissionScope": "InvoiceRead",
                  "description": "%s",
                  "startDate": "%s",
                  "canDelegate": true
                }""".formatted(GRANT_ID, NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        EntityPermissionItemRaw raw = OBJECT_MAPPER.readValue(json, EntityPermissionItemRaw.class);

        // when
        EntityPermission result = PermissionsMappers.toEntityPermission(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertNotNull(result.contextIdentifier());
        assertEquals(NIP_VALUE, result.contextIdentifier().value());
        assertEquals("InvoiceRead", result.permissionScope());
        assertEquals(DESCRIPTION, result.description());
        assertTrue(result.canDelegate());
    }

    @Test
    void toEntityRole_withParent_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "parentEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "role": "CourtBailiff",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        EntityRoleRaw raw = OBJECT_MAPPER.readValue(json, EntityRoleRaw.class);

        // when
        EntityRole result = PermissionsMappers.toEntityRole(raw);

        // then
        assertNotNull(result.parentEntityIdentifier());
        assertEquals(NIP_VALUE, result.parentEntityIdentifier().value());
        assertEquals("CourtBailiff", result.role());
    }

    @Test
    void toEntityRole_withoutParent_yieldsNullParent() throws Exception {
        // given
        String json = """
                {
                  "parentEntityIdentifier": null,
                  "role": "LocalGovernmentSubUnit",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(DESCRIPTION, START_DATE_ISO);
        EntityRoleRaw raw = OBJECT_MAPPER.readValue(json, EntityRoleRaw.class);

        // when
        EntityRole result = PermissionsMappers.toEntityRole(raw);

        // then
        assertNull(result.parentEntityIdentifier(), "null parent branch must propagate");
    }

    @Test
    void toEuEntityPermission_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorIdentifier": {"type": "Nip", "value": "%s"},
                  "vatUeIdentifier": "%s",
                  "euEntityName": "%s",
                  "authorizedFingerprintIdentifier": "%s",
                  "permissionScope": "InvoiceRead",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(GRANT_ID, NIP_VALUE, VAT_UE, EU_ENTITY_NAME,
                        FINGERPRINT_VALUE, DESCRIPTION, START_DATE_ISO);
        EuEntityPermissionRaw raw = OBJECT_MAPPER.readValue(json, EuEntityPermissionRaw.class);

        // when
        EuEntityPermission result = PermissionsMappers.toEuEntityPermission(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertEquals(VAT_UE, result.vatUeIdentifier());
        assertEquals(EU_ENTITY_NAME, result.euEntityName());
        assertEquals(FINGERPRINT_VALUE, result.authorizedFingerprintIdentifier());
    }

    @Test
    void toPersonalPermission_withAllIdentifiersAndPersonDetails_mapsAllBranches() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "contextIdentifier": {"type": "Nip", "value": "%s"},
                  "authorizedIdentifier": {"type": "Pesel", "value": "%s"},
                  "targetIdentifier": {"type": "Nip", "value": "%s"},
                  "permissionScope": "InvoiceRead",
                  "description": "%s",
                  "subjectPersonDetails": {"firstName": "%s", "lastName": "%s"},
                  "subjectEntityDetails": null,
                  "permissionState": "Active",
                  "startDate": "%s",
                  "canDelegate": true
                }""".formatted(GRANT_ID, NIP_VALUE, PESEL_VALUE, NIP_VALUE, DESCRIPTION,
                        FIRST_NAME, LAST_NAME, START_DATE_ISO);
        PersonalPermissionRaw raw = OBJECT_MAPPER.readValue(json, PersonalPermissionRaw.class);

        // when
        PersonalPermission result = PermissionsMappers.toPersonalPermission(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertNotNull(result.contextIdentifier());
        assertNotNull(result.authorizedIdentifier());
        assertEquals(PESEL_VALUE, result.authorizedIdentifier().value());
        assertNotNull(result.targetIdentifier());
        assertNotNull(result.subjectPersonDetails());
        assertEquals(FIRST_NAME, result.subjectPersonDetails().firstName());
        assertEquals(LAST_NAME, result.subjectPersonDetails().surname());
        assertNull(result.subjectEntityDetails());
        assertEquals("Active", result.permissionState());
    }

    @Test
    void toPersonalPermission_withEntityDetailsAndNullIdentifiers_mapsNullBranches() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "contextIdentifier": null,
                  "authorizedIdentifier": null,
                  "targetIdentifier": null,
                  "permissionScope": "InvoiceWrite",
                  "description": "%s",
                  "subjectPersonDetails": null,
                  "subjectEntityDetails": {"fullName": "%s"},
                  "permissionState": "Active",
                  "startDate": "%s",
                  "canDelegate": false
                }""".formatted(GRANT_ID, DESCRIPTION, ENTITY_FULL_NAME, START_DATE_ISO);
        PersonalPermissionRaw raw = OBJECT_MAPPER.readValue(json, PersonalPermissionRaw.class);

        // when
        PersonalPermission result = PermissionsMappers.toPersonalPermission(raw);

        // then
        assertNull(result.contextIdentifier());
        assertNull(result.authorizedIdentifier());
        assertNull(result.targetIdentifier());
        assertNull(result.subjectPersonDetails());
        assertNotNull(result.subjectEntityDetails());
        assertEquals(ENTITY_FULL_NAME, result.subjectEntityDetails().fullName());
    }

    @Test
    void toPersonPermission_withAllOptionalIdentifiers_mapsAllBranches() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorizedIdentifier": {"type": "Pesel", "value": "%s"},
                  "contextIdentifier": {"type": "Nip", "value": "%s"},
                  "targetIdentifier": {"type": "Nip", "value": "%s"},
                  "authorIdentifier": {"type": "Nip", "value": "%s"},
                  "permissionScope": "InvoiceRead",
                  "description": "%s",
                  "permissionState": "Active",
                  "startDate": "%s",
                  "canDelegate": true
                }""".formatted(GRANT_ID, PESEL_VALUE, NIP_VALUE, NIP_VALUE, NIP_VALUE,
                        DESCRIPTION, START_DATE_ISO);
        PersonPermissionRaw raw = OBJECT_MAPPER.readValue(json, PersonPermissionRaw.class);

        // when
        PersonPermission result = PermissionsMappers.toPersonPermission(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertNotNull(result.authorizedIdentifier());
        assertNotNull(result.contextIdentifier());
        assertNotNull(result.targetIdentifier());
        assertNotNull(result.authorIdentifier());
    }

    @Test
    void toPersonPermission_withoutOptionalIdentifiers_mapsNullBranches() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorizedIdentifier": {"type": "Pesel", "value": "%s"},
                  "contextIdentifier": null,
                  "targetIdentifier": null,
                  "authorIdentifier": {"type": "Nip", "value": "%s"},
                  "permissionScope": "InvoiceWrite",
                  "description": "%s",
                  "permissionState": "Active",
                  "startDate": "%s",
                  "canDelegate": false
                }""".formatted(GRANT_ID, PESEL_VALUE, NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        PersonPermissionRaw raw = OBJECT_MAPPER.readValue(json, PersonPermissionRaw.class);

        // when
        PersonPermission result = PermissionsMappers.toPersonPermission(raw);

        // then
        assertNull(result.contextIdentifier());
        assertNull(result.targetIdentifier());
    }

    @Test
    void toSubordinateEntityRole_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "subordinateEntityIdentifier": {"type": "Nip", "value": "%s"},
                  "role": "LocalGovernmentSubUnit",
                  "description": "%s",
                  "startDate": "%s"
                }""".formatted(NIP_VALUE, DESCRIPTION, START_DATE_ISO);
        SubordinateEntityRoleRaw raw = OBJECT_MAPPER.readValue(json, SubordinateEntityRoleRaw.class);

        // when
        SubordinateEntityRole result = PermissionsMappers.toSubordinateEntityRole(raw);

        // then
        assertNotNull(result.subordinateEntityIdentifier());
        assertEquals(NIP_VALUE, result.subordinateEntityIdentifier().value());
        assertEquals("LocalGovernmentSubUnit", result.role());
    }

    @Test
    void toSubunitPermission_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "id": "%s",
                  "authorizedIdentifier": {"type": "Pesel", "value": "%s"},
                  "subunitIdentifier": {"type": "Nip", "value": "%s"},
                  "authorIdentifier": {"type": "Nip", "value": "%s"},
                  "permissionScope": "CredentialsManage",
                  "description": "%s",
                  "subunitName": "%s",
                  "startDate": "%s"
                }""".formatted(GRANT_ID, PESEL_VALUE, NIP_VALUE, NIP_VALUE, DESCRIPTION,
                        SUBUNIT_NAME, START_DATE_ISO);
        SubunitPermissionRaw raw = OBJECT_MAPPER.readValue(json, SubunitPermissionRaw.class);

        // when
        SubunitPermission result = PermissionsMappers.toSubunitPermission(raw);

        // then
        assertEquals(GRANT_ID, result.id());
        assertEquals(SUBUNIT_NAME, result.subunitName());
        assertNotNull(result.subunitIdentifier());
        assertNotNull(result.authorIdentifier());
    }
}
