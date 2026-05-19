/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape pin for 7 stream paginators that previously had only live
 * demo coverage. {@code streamPersonal} is pinned in
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.invoicing.ManualValidationWireShapeTest};
 * the remaining 7 are pinned here. Each test:
 * <ol>
 *   <li>stubs two pages — page 0 with {@code hasMore=true}, page 1
 *       with {@code hasMore=false}.</li>
 *   <li>walks the stream to completion via {@code toList()}.</li>
 *   <li>asserts both requests went out with {@code pageSize=100}
 *       (KSeF-enforced max for permissions endpoints — regression-pinned
 *       2026-05-19 live demo) and {@code pageOffset} advanced 0 → 1.</li>
 * </ol>
 */
@WireMockTest
class PermissionsStreamPaginationTest {

    private static final String PATH_PERSONS_QUERY = "/v2/permissions/query/persons/grants";
    private static final String PATH_SUBUNITS_QUERY = "/v2/permissions/query/subunits/grants";
    private static final String PATH_ENTITIES_QUERY = "/v2/permissions/query/entities/grants";
    private static final String PATH_ENTITY_ROLES_QUERY = "/v2/permissions/query/entities/roles";
    private static final String PATH_SUBORDINATE_ROLES_QUERY = "/v2/permissions/query/subordinate-entities/roles";
    private static final String PATH_AUTHORIZATIONS_QUERY = "/v2/permissions/query/authorizations/grants";
    private static final String PATH_EU_ENTITIES_QUERY = "/v2/permissions/query/eu-entities/grants";

    private static final String EMPTY_PERMISSIONS_PAGE = """
            {"permissions": [], "hasMore": false}
            """;
    private static final String NONEMPTY_PERMISSIONS_PAGE = """
            {"permissions": [], "hasMore": true}
            """;
    private static final String EMPTY_ROLES_PAGE = """
            {"roles": [], "hasMore": false}
            """;
    private static final String NONEMPTY_ROLES_PAGE = """
            {"roles": [], "hasMore": true}
            """;
    private static final String EMPTY_GRANTS_PAGE = """
            {"authorizationGrants": [], "hasMore": false}
            """;
    private static final String NONEMPTY_GRANTS_PAGE = """
            {"authorizationGrants": [], "hasMore": true}
            """;

    private static final String SCENARIO_SUFFIX = "-stream-pagination";
    private static final String PAGE_1_STATE = "page1";

    @Test
    void streamPersons_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_PERSONS_QUERY, NONEMPTY_PERMISSIONS_PAGE, EMPTY_PERMISSIONS_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamPersons(
                    PersonPermissionsQueryBuilder.permissionsGrantedInCurrentContext().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_PERSONS_QUERY);
        }
    }

    @Test
    void streamSubunits_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_SUBUNITS_QUERY, NONEMPTY_PERMISSIONS_PAGE, EMPTY_PERMISSIONS_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamSubunits(
                    SubunitPermissionsQueryBuilder.create().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_SUBUNITS_QUERY);
        }
    }

    @Test
    void streamEntities_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_ENTITIES_QUERY, NONEMPTY_PERMISSIONS_PAGE, EMPTY_PERMISSIONS_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamEntities(
                    EntityPermissionsQueryBuilder.create().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_ENTITIES_QUERY);
        }
    }

    @Test
    void streamEntityRoles_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        // entityRoles uses GET; spec change discovered during live demo run.
        stubTwoPagesGet(PATH_ENTITY_ROLES_QUERY, NONEMPTY_ROLES_PAGE, EMPTY_ROLES_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamEntityRoles(
                    EntityRolesQueryBuilder.create().build()).toList();
            assertTwoGetPagesWithSpecMaxPageSize(PATH_ENTITY_ROLES_QUERY);
        }
    }

    @Test
    void streamSubordinateRoles_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_SUBORDINATE_ROLES_QUERY, NONEMPTY_ROLES_PAGE, EMPTY_ROLES_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamSubordinateRoles(
                    SubordinateEntityRolesQueryBuilder.create().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_SUBORDINATE_ROLES_QUERY);
        }
    }

    @Test
    void streamAuthorizations_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_AUTHORIZATIONS_QUERY, NONEMPTY_GRANTS_PAGE, EMPTY_GRANTS_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamAuthorizations(
                    EntityAuthorizationPermissionsQueryBuilder.granted().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_AUTHORIZATIONS_QUERY);
        }
    }

    @Test
    void streamEuEntities_walksPageOffsetWithSpecMaxPageSize(WireMockRuntimeInfo wmInfo) {
        stubTwoPagesPost(PATH_EU_ENTITIES_QUERY, NONEMPTY_PERMISSIONS_PAGE, EMPTY_PERMISSIONS_PAGE);
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamEuEntities(
                    EuEntityPermissionsQueryBuilder.create().build()).toList();
            assertTwoPostPagesWithSpecMaxPageSize(PATH_EU_ENTITIES_QUERY);
        }
    }

    private static void stubTwoPagesPost(String path, String pageZeroBody, String pageOneBody) {
        stubFor(post(urlPathEqualTo(path))
                .inScenario(path + SCENARIO_SUFFIX)
                .whenScenarioStateIs(Scenario.STARTED)
                .withQueryParam("pageOffset", equalTo("0"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageZeroBody))
                .willSetStateTo(PAGE_1_STATE));
        stubFor(post(urlPathEqualTo(path))
                .inScenario(path + SCENARIO_SUFFIX)
                .whenScenarioStateIs(PAGE_1_STATE)
                .withQueryParam("pageOffset", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody)));
    }

    private static void stubTwoPagesGet(String path, String pageZeroBody, String pageOneBody) {
        stubFor(get(urlPathEqualTo(path))
                .inScenario(path + SCENARIO_SUFFIX)
                .whenScenarioStateIs(Scenario.STARTED)
                .withQueryParam("pageOffset", equalTo("0"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageZeroBody))
                .willSetStateTo(PAGE_1_STATE));
        stubFor(get(urlPathEqualTo(path))
                .inScenario(path + SCENARIO_SUFFIX)
                .whenScenarioStateIs(PAGE_1_STATE)
                .withQueryParam("pageOffset", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody)));
    }

    private static void assertTwoPostPagesWithSpecMaxPageSize(String path) {
        var requests = findAll(postRequestedFor(urlPathEqualTo(path)));
        assertEquals(2, requests.size(), "stream must fetch both pages");
        assertTrue(requests.get(0).getUrl().contains("pageSize=100"),
                "page 0 should use KSeF-enforced max pageSize=100, was: " + requests.get(0).getUrl());
        assertTrue(requests.get(1).getUrl().contains("pageOffset=1"),
                "page 1 should advance pageOffset to 1, was: " + requests.get(1).getUrl());
    }

    private static void assertTwoGetPagesWithSpecMaxPageSize(String path) {
        var requests = findAll(getRequestedFor(urlPathEqualTo(path)));
        assertEquals(2, requests.size(), "stream must fetch both pages");
        assertTrue(requests.get(0).getUrl().contains("pageSize=100"),
                "page 0 should use KSeF-enforced max pageSize=100, was: " + requests.get(0).getUrl());
        assertTrue(requests.get(1).getUrl().contains("pageOffset=1"),
                "page 1 should advance pageOffset to 1, was: " + requests.get(1).getUrl());
    }
}
