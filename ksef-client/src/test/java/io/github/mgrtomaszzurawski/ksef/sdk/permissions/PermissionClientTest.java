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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AttachmentPermissionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubordinateEntityRoles;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissions;
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

@WireMockTest
class PermissionClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_OPERATION_REF = "20260404-PM-1234567890-ABCDEF1234-09";
    private static final String TEST_PERMISSION_ID = "perm-id-abc123def456";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_PESEL = "82060411457";
    private static final String TEST_FINGERPRINT = "ABC123DEF456";
    private static final String TEST_DESCRIPTION = "Test permission grant";

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
                ksef.permissions().grantPerson(PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                        .description(TEST_DESCRIPTION)
                        .personDetails("Jan", "Kowalski")
                        .invoiceRead());

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
                ksef.permissions().grantEntity(EntityPermissionGrantBuilder.forNip(TEST_NIP)
                        .description(TEST_DESCRIPTION)
                        .entityDetails("Firma Sp. z o.o.")
                        .invoiceRead());

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
                ksef.permissions().grantAuthorization(EntityAuthorizationPermissionGrantBuilder.forNip(TEST_NIP)
                        .selfInvoicing()
                        .description(TEST_DESCRIPTION)
                        .entityDetails("Firma Sp. z o.o."));

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
                ksef.permissions().grantIndirect(IndirectPermissionGrantBuilder.forNip(TEST_NIP)
                        .description(TEST_DESCRIPTION)
                        .personDetails("Jan", "Kowalski")
                        .invoiceRead());

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
                ksef.permissions().grantSubunit(SubunitPermissionGrantBuilder.forPesel(TEST_PESEL)
                        .contextNip(TEST_NIP)
                        .description(TEST_DESCRIPTION)
                        .personDetails("Jan", "Kowalski"));

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
                ksef.permissions().grantEuEntityAdmin(EuEntityAdminPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                        .contextNipVatUe("PL" + TEST_NIP)
                        .description(TEST_DESCRIPTION)
                        .euEntityName("EU Partner GmbH")
                        .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                        .euEntityDetails("EU Partner GmbH", "Berlin, Germany"));

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
                ksef.permissions().grantEuEntity(EuEntityPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                        .description(TEST_DESCRIPTION)
                        .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                        .invoiceRead());

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
                ksef.permissions().queryPersonal(PersonalPermissionsQueryBuilder.create());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void queryPersons_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/persons/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        PersonPermissions response =
                ksef.permissions().queryPersons(PersonPermissionsQueryBuilder.permissionsInCurrentContext());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void querySubunits_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/subunits/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        SubunitPermissions response =
                ksef.permissions().querySubunits();

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    @Test
    void queryEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/entities/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EntityPermissions response =
                ksef.permissions().queryEntities();

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
                ksef.permissions().querySubordinateRoles();

        assertNotNull(response.roles());
        assertFalse(response.hasMore());
    }

    @Test
    void queryAuthorizations_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/authorizations/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EntityAuthorizationPermissions response =
                ksef.permissions().queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder.granted());

        assertNotNull(response.authorizationGrants());
        assertFalse(response.hasMore());
    }

    @Test
    void queryEuEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/api/v2/permissions/query/eu-entities/grants", QUERY_EMPTY_RESPONSE);
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        EuEntityPermissions response =
                ksef.permissions().queryEuEntities(EuEntityPermissionsQueryBuilder.create());

        assertNotNull(response.permissions());
        assertFalse(response.hasMore());
    }

    // --- Error and security tests ---

    @Test
    void grantPerson_whenUnauthorized_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given — both the target endpoint and the reauth security endpoint return 401,
        // so after the SDK retries once on 401 the auth exception propagates.
        stubFor(post(urlEqualTo("/api/v2/permissions/persons/grants"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));
        stubFor(get(urlEqualTo("/api/v2/security/public-key-certificates"))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("{}")));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        var permissions = ksef.permissions();
        var builder = PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                .description(TEST_DESCRIPTION)
                .personDetails("Jan", "Kowalski")
                .invoiceRead();
        assertThrows(KsefAuthException.class, () -> permissions.grantPerson(builder));
    }

    @Test
    void revokeCommon_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        var permissionsX = ksef.permissions();


        assertThrows(IllegalArgumentException.class, () -> permissionsX.revokeCommon("../../../etc/passwd"));
    }

    @Test
    void getOperationStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createAuthenticatedClient(wmInfo);

        var permissionsX = ksef.permissions();


        assertThrows(IllegalArgumentException.class, () -> permissionsX.getOperationStatus("../../../etc/passwd"));
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
