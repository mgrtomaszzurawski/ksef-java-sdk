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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.PermissionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;
import java.time.Duration;
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
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_OPERATION_REF = "20260404-PM-1234567890-ABCDEF1234-09";
    private static final String TEST_PERMISSION_ID = "perm-id-abc123def456";
    private static final String TEST_NIP = "1234567890";
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

    /** In-progress (code 110) — used by the {@code *AndAwait} timeout test. */
    private static final String OPERATION_STATUS_IN_PROGRESS_RESPONSE = """
            {
              "status": {"code": 110, "description": "InProgress"}
            }
            """;
    private static final String OPERATIONS_PATH = "/v2/permissions/operations/";
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_TINY_TIMEOUT = Duration.ofMillis(50);

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
        stubGrantEndpoint("/v2/permissions/persons/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantPerson(PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                            .description(TEST_DESCRIPTION)
                            .personDetails("Jan", "Kowalski")
                            .invoiceRead());

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/entities/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantEntity(EntityPermissionGrantBuilder.forNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .entityDetails("Firma Sp. z o.o.")
                            .invoiceRead());

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantAuthorization_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/authorizations/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantAuthorization(EntityAuthorizationPermissionGrantBuilder.forNip(TEST_NIP)
                            .selfInvoicing()
                            .description(TEST_DESCRIPTION)
                            .entityDetails("Firma Sp. z o.o."));

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantIndirect_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/indirect/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantIndirect(IndirectPermissionGrantBuilder.forNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .personDetails("Jan", "Kowalski")
                            .invoiceRead());

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantSubunit_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/subunits/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantSubunit(SubunitPermissionGrantBuilder.forPesel(TEST_PESEL)
                            .contextNip(TEST_NIP)
                            .description(TEST_DESCRIPTION)
                            .personDetails("Jan", "Kowalski"));

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void grantEuEntityAdmin_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/eu-entities/administration/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

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
    }

    @Test
    void grantEuEntity_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        // given
        stubGrantEndpoint("/v2/permissions/eu-entities/grants");
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            PermissionOperationResult response =
                    ksef.permissions().grantEuEntity(EuEntityPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                            .description(TEST_DESCRIPTION)
                            .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                            .invoiceRead());

            // then
            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void revokeCommon_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo("/v2/permissions/common/grants/" + TEST_PERMISSION_ID))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PermissionOperationResult response = ksef.permissions().revokeCommon(TEST_PERMISSION_ID);

            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    @Test
    void revokeAuthorization_whenAuthenticated_returnsOperationReference(WireMockRuntimeInfo wmInfo) {
        stubFor(delete(urlEqualTo("/v2/permissions/authorizations/grants/" + TEST_PERMISSION_ID))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PermissionOperationResult response =
                    ksef.permissions().revokeAuthorization(TEST_PERMISSION_ID);

            assertEquals(TEST_OPERATION_REF, response.referenceNumber());
        }
    }

    // ----- *AndAwait coverage (PLAN A.9 — JaCoCo METHOD=1.00 on PermissionClient) -----
    //
    // The 9 default *AndAwait methods on PermissionClient share one
    // private helper (awaitOperationTerminal) that polls
    // GET /v2/permissions/operations/{ref} until status.code >= 200.
    // Each test stubs the corresponding grant/revoke endpoint plus the
    // status endpoint with terminal code 200, then calls the *AndAwait
    // overload and asserts the helper returns the terminal status.

    @Test
    void grantPersonAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/persons/grants",
                permissions -> permissions.grantPersonAndAwait(
                        PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                                .description(TEST_DESCRIPTION)
                                .personDetails("Jan", "Kowalski")
                                .invoiceRead(),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantEntityAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/entities/grants",
                permissions -> permissions.grantEntityAndAwait(
                        EntityPermissionGrantBuilder.forNip(TEST_NIP)
                                .description(TEST_DESCRIPTION)
                                .entityDetails("Firma Sp. z o.o.")
                                .invoiceRead(),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantAuthorizationAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/authorizations/grants",
                permissions -> permissions.grantAuthorizationAndAwait(
                        EntityAuthorizationPermissionGrantBuilder.forNip(TEST_NIP)
                                .selfInvoicing()
                                .description(TEST_DESCRIPTION)
                                .entityDetails("Firma Sp. z o.o."),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantIndirectAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/indirect/grants",
                permissions -> permissions.grantIndirectAndAwait(
                        IndirectPermissionGrantBuilder.forNip(TEST_NIP)
                                .description(TEST_DESCRIPTION)
                                .personDetails("Jan", "Kowalski")
                                .invoiceRead(),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantSubunitAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/subunits/grants",
                permissions -> permissions.grantSubunitAndAwait(
                        SubunitPermissionGrantBuilder.forPesel(TEST_PESEL)
                                .contextNip(TEST_NIP)
                                .description(TEST_DESCRIPTION)
                                .personDetails("Jan", "Kowalski"),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantEuEntityAdminAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/eu-entities/administration/grants",
                permissions -> permissions.grantEuEntityAdminAndAwait(
                        EuEntityAdminPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                                .contextNipVatUe("PL" + TEST_NIP)
                                .description(TEST_DESCRIPTION)
                                .euEntityName("EU Partner GmbH")
                                .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                                .euEntityDetails("EU Partner GmbH", "Berlin, Germany"),
                        AWAIT_TIMEOUT));
    }

    @Test
    void grantEuEntityAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertGrantAndAwaitReturnsTerminal(wmInfo, "/v2/permissions/eu-entities/grants",
                permissions -> permissions.grantEuEntityAndAwait(
                        EuEntityPermissionGrantBuilder.forFingerprint(TEST_FINGERPRINT)
                                .description(TEST_DESCRIPTION)
                                .subjectEntityByFingerprint("Partner Corp", "Berlin, Germany")
                                .invoiceRead(),
                        AWAIT_TIMEOUT));
    }

    @Test
    void revokeCommonAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertRevokeAndAwaitReturnsTerminal(wmInfo,
                "/v2/permissions/common/grants/" + TEST_PERMISSION_ID,
                permissions -> permissions.revokeCommonAndAwait(TEST_PERMISSION_ID, AWAIT_TIMEOUT));
    }

    @Test
    void revokeAuthorizationAndAwait_whenStatusTerminal_returnsTerminalStatus(WireMockRuntimeInfo wmInfo) {
        assertRevokeAndAwaitReturnsTerminal(wmInfo,
                "/v2/permissions/authorizations/grants/" + TEST_PERMISSION_ID,
                permissions -> permissions.revokeAuthorizationAndAwait(TEST_PERMISSION_ID, AWAIT_TIMEOUT));
    }

    /**
     * Stub the supplied grant endpoint + the operation-status poll
     * (terminal code 200), invoke {@code grantInvocation}, and assert the
     * returned status is terminal. Used by the seven {@code grant*AndAwait}
     * variants to avoid copy-pasting the stub-and-assert template.
     */
    private static void assertGrantAndAwaitReturnsTerminal(WireMockRuntimeInfo wmInfo,
                                                           String endpoint,
                                                           AwaitInvocation grantInvocation) {
        stubGrantEndpoint(endpoint);
        stubOperationStatusTerminal();
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            PermissionOperationStatus terminal = grantInvocation.run(ksef.permissions());
            assertEquals(KSEF_STATUS_OK, terminal.status().code());
        }
    }

    /**
     * Variant of {@link #assertGrantAndAwaitReturnsTerminal} for revoke
     * paths, which use {@code DELETE} instead of {@code POST} and carry a
     * permission-id segment in the URL.
     */
    private static void assertRevokeAndAwaitReturnsTerminal(WireMockRuntimeInfo wmInfo,
                                                            String deletePath,
                                                            AwaitInvocation revokeInvocation) {
        stubFor(delete(urlEqualTo(deletePath))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));
        stubOperationStatusTerminal();
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            PermissionOperationStatus terminal = revokeInvocation.run(ksef.permissions());
            assertEquals(KSEF_STATUS_OK, terminal.status().code());
        }
    }

    /** Captures one specific {@code *AndAwait} default call against the permissions API. */
    @FunctionalInterface
    private interface AwaitInvocation {
        PermissionOperationStatus run(PermissionClient permissions);
    }

    @Test
    void grantPersonAndAwait_whenStatusNeverTerminal_throwsTimeout(WireMockRuntimeInfo wmInfo) {
        // Covers awaitOperationTerminal's statusCodeOf lambda — that branch
        // only fires when the deadline elapses with the last status still
        // in-progress (code < 200).
        stubGrantEndpoint("/v2/permissions/persons/grants");
        stubFor(get(urlEqualTo(OPERATIONS_PATH + TEST_OPERATION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_STATUS_IN_PROGRESS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {
            PersonPermissionGrantBuilder builder = PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                    .description(TEST_DESCRIPTION)
                    .personDetails("Jan", "Kowalski")
                    .invoiceRead();

            assertThrows(KsefAsyncTimeoutException.class,
                    () -> ksef.permissions().grantPersonAndAwait(builder, AWAIT_TINY_TIMEOUT));
        }
    }

    @Test
    void getOperationStatus_whenExists_returnsStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(get(urlEqualTo("/v2/permissions/operations/" + TEST_OPERATION_REF))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_STATUS_RESPONSE)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PermissionOperationStatus response =
                    ksef.permissions().getOperationStatus(TEST_OPERATION_REF);

            assertEquals(KSEF_STATUS_OK, response.status().code());
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
                    ksef.permissions().queryPersonal(PersonalPermissionsQueryBuilder.create());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryPersons_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/persons/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            PersonPermissions response =
                    ksef.permissions().queryPersons(PersonPermissionsQueryBuilder.permissionsInCurrentContext());

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void querySubunits_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/subunits/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            SubunitPermissions response =
                    ksef.permissions().querySubunits();

            assertNotNull(response.permissions());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/entities/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EntityPermissions response =
                    ksef.permissions().queryEntities();

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

            EntityRoles response = ksef.permissions().queryEntityRoles();

            assertNotNull(response.roles());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void querySubordinateRoles_whenAuthenticated_returnsRoles(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/subordinate-entities/roles", QUERY_ROLES_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            SubordinateEntityRoles response =
                    ksef.permissions().querySubordinateRoles();

            assertNotNull(response.roles());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryAuthorizations_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/authorizations/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EntityAuthorizationPermissions response =
                    ksef.permissions().queryAuthorizations(EntityAuthorizationPermissionsQueryBuilder.granted());

            assertNotNull(response.authorizationGrants());
            assertFalse(response.hasMore());
        }
    }

    @Test
    void queryEuEntities_whenAuthenticated_returnsPermissions(WireMockRuntimeInfo wmInfo) {
        stubQueryEndpoint("/v2/permissions/query/eu-entities/grants", QUERY_EMPTY_RESPONSE);
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            EuEntityPermissions response =
                    ksef.permissions().queryEuEntities(EuEntityPermissionsQueryBuilder.create());

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
            var builder = PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                    .description(TEST_DESCRIPTION)
                    .personDetails("Jan", "Kowalski")
                    .invoiceRead();
            assertThrows(KsefAuthException.class, () -> permissions.grantPerson(builder));
        }
    }

    @Test
    void revokeCommon_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            var permissions = ksef.permissions();

            assertThrows(IllegalArgumentException.class, () -> permissions.revokeCommon("../../../etc/passwd"));
        }
    }

    @Test
    void getOperationStatus_whenPathTraversal_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            var permissions = ksef.permissions();

            assertThrows(IllegalArgumentException.class, () -> permissions.getOperationStatus("../../../etc/passwd"));
        }
    }

    private static void stubGrantEndpoint(String path) {
        stubFor(post(urlEqualTo(path))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_RESPONSE)));
    }

    private static void stubOperationStatusTerminal() {
        // Used by every *AndAwait test — first status poll reports the
        // operation completed (code 200), so the await loop returns
        // immediately without sleeping.
        stubFor(get(urlEqualTo(OPERATIONS_PATH + TEST_OPERATION_REF))
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
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1234567890");
    }
}
