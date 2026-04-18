/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EntityPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityAdministrationPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.IndirectPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubordinateEntityRolesQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitPermissionsQueryRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SubunitPermissions;
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

@WireMockTest
class PermissionClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_OPERATION_REF = "20260404-PM-1234567890-ABCDEF1234-09";
    private static final String TEST_PERMISSION_ID = "perm-id-abc123def456";

    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;
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

    // --- Grant tests ---

    @Test
    void grantPerson_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/persons/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantPerson(new PersonPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/entities/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantEntity(new EntityPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantAuthorization_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/authorizations/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantAuthorization(new EntityAuthorizationPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantIndirect_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/indirect/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantIndirect(new IndirectPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantSubunit_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/subunits/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantSubunit(new SubunitPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantEuEntityAdmin_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/eu-entities/administration/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantEuEntityAdmin(new EuEntityAdministrationPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void grantEuEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/api/v2/permissions/eu-entities/grants");
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        PermissionOperationResult response =
                ksef.permissions().grantEuEntity(new EuEntityPermissionsGrantRequestRaw());

        // then
        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    // --- Revoke tests ---

    @Test
    void revokeCommon_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo("/api/v2/permissions/common/grants/" + TEST_PERMISSION_ID))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(OPERATION_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PermissionOperationResult response = ksef.permissions().revokeCommon(TEST_PERMISSION_ID);

        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    @Test
    void revokeAuthorization_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo("/api/v2/permissions/authorizations/grants/" + TEST_PERMISSION_ID))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(OPERATION_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PermissionOperationResult response =
                ksef.permissions().revokeAuthorization(TEST_PERMISSION_ID);

        assertEquals(TEST_OPERATION_REF, response.referenceNumber());
    }

    // --- Status tests ---

    @Test
    void getOperationStatus_whenExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/api/v2/permissions/operations/" + TEST_OPERATION_REF))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(OPERATION_STATUS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PermissionOperationStatus response =
                ksef.permissions().getOperationStatus(TEST_OPERATION_REF);

        assertEquals(KSEF_STATUS_OK, response.status().code());
    }

    @Test
    void getAttachmentStatus_whenAuthenticated_returnsStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/api/v2/permissions/attachments/status"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ATTACHMENT_STATUS_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        AttachmentPermissionStatus response =
                ksef.permissions().getAttachmentStatus();

        assertTrue(response.attachmentAllowed());
    }

    // --- Query tests ---

    @Test
    void queryPersonal_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/personal/grants", QUERY_PERSONAL_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PersonalPermissions response =
                ksef.permissions().queryPersonal(new PersonalPermissionsQueryRequestRaw());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void queryPersons_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/persons/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PersonPermissions response =
                ksef.permissions().queryPersons(new PersonPermissionsQueryRequestRaw());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void querySubunits_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/subunits/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        SubunitPermissions response =
                ksef.permissions().querySubunits(new SubunitPermissionsQueryRequestRaw());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void queryEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/entities/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EntityPermissions response =
                ksef.permissions().queryEntities(new EntityPermissionsQueryRequestRaw());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void queryEntityRoles_whenAuthenticated_returnsRoles(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/api/v2/permissions/query/entities/roles"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(QUERY_ROLES_RESPONSE)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EntityRoles response = ksef.permissions().queryEntityRoles();

        assertNotNull(response.roles());
        assertFalse(response.hasMore());
    }

    @Test
    void querySubordinateRoles_whenAuthenticated_returnsRoles(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/subordinate-entities/roles", QUERY_ROLES_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        SubordinateEntityRoles response =
                ksef.permissions().querySubordinateRoles(new SubordinateEntityRolesQueryRequestRaw());

        assertNotNull(response.roles());
        assertFalse(response.hasMore());
    }

    @Test
    void queryAuthorizations_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/authorizations/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EntityAuthorizationPermissions response =
                ksef.permissions().queryAuthorizations(new EntityAuthorizationPermissionsQueryRequestRaw());

        assertNotNull(response.authorizationGrants());
        assertFalse(response.hasMore());
    }

    @Test
    void queryEuEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/eu-entities/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EuEntityPermissions response =
                ksef.permissions().queryEuEntities(new EuEntityPermissionsQueryRequestRaw());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    // --- Error and security tests ---

    @Test
    void grantPerson_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo("/api/v2/permissions/persons/grants"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        assertThrows(KsefAuthException.class,
                () -> ksef.permissions().grantPerson(new PersonPermissionsGrantRequestRaw()));
    }

    @Test
    void revokeCommon_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        assertThrows(IllegalArgumentException.class,
                () -> ksef.permissions().revokeCommon("../../../etc/passwd"));
    }

    @Test
    void getOperationStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        assertThrows(IllegalArgumentException.class,
                () -> ksef.permissions().getOperationStatus("../../../etc/passwd"));
    }

    // --- Helpers ---

    private static void stubGrantEndpoint(String path) {
        stubFor(post(urlEqualTo(path))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(OPERATION_RESPONSE)));
    }

    private static void stubQueryEndpoint(String path, String responseBody) {
        stubFor(post(urlEqualTo(path))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
