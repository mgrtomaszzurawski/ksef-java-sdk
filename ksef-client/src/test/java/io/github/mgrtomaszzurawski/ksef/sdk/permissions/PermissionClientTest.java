/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.Permissions;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class PermissionClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_OPERATION_REF = "20260404-PM-1111111111-ABCDEF1234-09";
    private static final String TEST_PERMISSION_ID = "perm-id-abc123def456";
    private static final String TEST_NIP = "1111111111";
    private static final String TEST_PESEL = "82060411457";
    private static final String TEST_FINGERPRINT = "ABC123DEF456";
    private static final String TEST_DESCRIPTION = "Test permission grant";
    private static final int KSEF_STATUS_OK = 200;

    private static final String OPERATION_RESPONSE = """
            {
              "referenceNumber": "%s"
            }
            """.formatted(TEST_OPERATION_REF);

    private static final String OPERATION_STATUS_RESPONSE = """
            {
              "status": {"code": 200, "description": "Completed"}
            }
            """;

    /** Per-spec REST paths used by individual grant tests. */
    private static final String PATH_PERSONS_GRANTS = "/v2/permissions/persons/grants";
    private static final String PATH_ENTITIES_GRANTS = "/v2/permissions/entities/grants";
    private static final String PATH_AUTHORIZATIONS_GRANTS = "/v2/permissions/authorizations/grants";
    private static final String PATH_INDIRECT_GRANTS = "/v2/permissions/indirect/grants";
    private static final String PATH_SUBUNITS_GRANTS = "/v2/permissions/subunits/grants";
    private static final String PATH_EU_ENTITIES_ADMIN_GRANTS = "/v2/permissions/eu-entities/administration/grants";
    private static final String PATH_EU_ENTITIES_GRANTS = "/v2/permissions/eu-entities/grants";
    private static final String PATH_REVOKE_COMMON = "/v2/permissions/common/grants/" + TEST_PERMISSION_ID;
    private static final String PATH_REVOKE_AUTHORIZATION = "/v2/permissions/authorizations/grants/" + TEST_PERMISSION_ID;
    /** Test fixtures for grant-builder argument lists. */
    private static final String TEST_FIRST_NAME = "Jan";
    private static final String TEST_LAST_NAME = "Kowalski";
    private static final String TEST_ENTITY_NAME = "Firma Sp. z o.o.";
    private static final String TEST_EU_ENTITY_NAME = "EU Partner GmbH";
    private static final String TEST_PARTNER_NAME = "Partner Corp";
    private static final String TEST_PARTNER_ADDRESS = "Berlin, Germany";
    private static final String NIP_VAT_UE_PREFIX = "PL";

    private static final String ATTACHMENT_STATUS_RESPONSE = """
            {
              "isAttachmentAllowed": true
            }
            """;

    private static final String QUERY_PERSONAL_RESPONSE = """
            {
              "permissions": [],
              "hasMore": false
            }
            """;

    private static final String QUERY_EMPTY_RESPONSE = """
            {
              "permissions": [],
              "hasMore": false
            }
            """;

    private static final String QUERY_ROLES_RESPONSE = """
            {
              "roles": [],
              "hasMore": false
            }
            """;

    @Test
    void grantPerson_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_PERSONS_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantPerson(PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                            .description(TEST_DESCRIPTION)
                            .personDetails(TEST_FIRST_NAME, TEST_LAST_NAME)
                            .invoiceRead()
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_ENTITIES_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantEntity(EntityPermissionGrantBuilder.forNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .entityDetails("Firma Sp. z o.o.")
                            .invoiceRead()
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantAuthorization_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_AUTHORIZATIONS_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantAuthorization(EntityAuthorizationPermissionGrantBuilder.forNip(TEST_NIP)
                            .selfInvoicing()
                            .description(TEST_DESCRIPTION)
                            .entityDetails("Firma Sp. z o.o.")
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantIndirect_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_INDIRECT_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantIndirect(IndirectPermissionGrantBuilder.forNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .personDetails(TEST_FIRST_NAME, TEST_LAST_NAME)
                            .invoiceRead()
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantSubunit_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_SUBUNITS_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantSubunit(SubunitPermissionGrantBuilder.forPesel(TEST_PESEL)
                            .contextNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .personDetails(TEST_FIRST_NAME, TEST_LAST_NAME)
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantEuEntityAdmin_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_EU_ENTITIES_ADMIN_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantEuEntityAdmin(EuEntityAdminPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                            .contextNipVatUe("PL" + TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .euEntityName("EU Partner GmbH")
                            .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                            .euEntityDetails("EU Partner GmbH", "Berlin, Germany")
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantEuEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint(PATH_EU_ENTITIES_GRANTS);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationStatus response =
                    ksef.permissions().grantEuEntity(EuEntityPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                            .description(TEST_DESCRIPTION)
                            .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                            .invoiceRead()
                            .build());

            // then
            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void revokeCommon_whenAuthenticated_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo(PATH_REVOKE_COMMON))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));
        stubOperationStatusEndpoint();

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PermissionOperationStatus response = ksef.permissions().revokePermission(TEST_PERMISSION_ID);

            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void revokeAuthorization_whenAuthenticated_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo(PATH_REVOKE_AUTHORIZATION))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));
        stubOperationStatusEndpoint();

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PermissionOperationStatus response =
                    ksef.permissions().revokeAuthorization(TEST_PERMISSION_ID);

            assertEquals(KSEF_STATUS_OK, response.status().code());
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void getAttachmentStatus_whenAuthenticated_returnsStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/v2/permissions/attachments/status"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(ATTACHMENT_STATUS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            AttachmentPermissionStatus response =
                    ksef.permissions().getAttachmentStatus();

            assertTrue(response.attachmentAllowed());
        }
    }

    @Test
    void queryPersonal_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/personal/grants", QUERY_PERSONAL_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PersonalPermissions response =
                    ksef.permissions().queryPersonal(PersonalPermissionsQueryBuilder.create().build());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryPersons_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/persons/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PersonPermissions response =
                    ksef.permissions().queryPersons(PersonPermissionsQueryBuilder.permissionsInCurrentContext().build());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void querySubunits_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/subunits/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            SubunitPermissions response =
                    ksef.permissions().querySubunits(SubunitPermissionsQueryBuilder.create().build());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/entities/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EntityPermissions response =
                    ksef.permissions().queryEntities(EntityPermissionsQueryBuilder.create().build());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryEntityRoles_whenAuthenticated_returnsRoles(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/v2/permissions/query/entities/roles"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(QUERY_ROLES_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EntityRoles response = ksef.permissions().queryEntityRoles(EntityRolesQueryBuilder.create().build());

            assertNotNull(response.roles());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void querySubordinateRoles_whenAuthenticated_returnsRoles(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/subordinate-entities/roles", QUERY_ROLES_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            SubordinateEntityRoles response =
                    ksef.permissions().querySubordinateRoles(SubordinateEntityRolesQueryBuilder.create().build());

            assertNotNull(response.roles());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryAuthorizations_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/authorizations/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EntityAuthorizationPermissions response =
                    ksef.permissions().queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder.granted().build());

            assertNotNull(response.authorizationGrants());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryEuEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/eu-entities/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EuEntityPermissions response =
                    ksef.permissions().queryEuEntities(EuEntityPermissionsQueryBuilder.create().build());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void grantPerson_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given — both the target endpoint and the reauth security endpoint return 401,
        // so after the SDK retries once on 401 the auth exception propagates.
        stubFor(post(urlEqualTo("/v2/permissions/persons/grants"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo("/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED).withBody("{}")));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            var permissions = ksef.permissions();
            var request = PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                    .description(TEST_DESCRIPTION)
                    .personDetails(TEST_FIRST_NAME, TEST_LAST_NAME)
                    .invoiceRead()
                    .build();
            assertThrows(KsefAuthException.class, () -> permissions.grantPerson(request));
        }
    }

    @Test
    void revokeCommon_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            var permissions = ksef.permissions();

            assertThrows(IllegalArgumentException.class, () -> permissions.revokePermission("../../../etc/passwd"));
        }
    }

    private static void stubGrantEndpoint(String path) {
        stubFor(post(urlEqualTo(path))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));
        // Sync grant polls /operations/{ref} until terminal — stub it with a
        // terminal success body so the poll loop returns on first tick.
        stubOperationStatusEndpoint();
    }

    private static void stubOperationStatusEndpoint() {
        stubFor(get(urlEqualTo("/v2/permissions/operations/" + TEST_OPERATION_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_STATUS_RESPONSE)));
    }

    private static void stubQueryEndpoint(String path, String responseBody) {
        stubFor(post(urlEqualTo(path))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(responseBody)));
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1111111111");
    }
}
