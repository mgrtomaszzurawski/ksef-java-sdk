/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BuyerIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceFormType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryAmountType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CommonSessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape regression coverage for the manual-validation batch
 * (Codex round-9 manual-validation A.1-A.5 + A.4.1 follow-up).
 *
 * <p>Each test pins one of the new public surface contracts so a future
 * regression in URL construction, JSON serialisation, or pagination flow
 * fails fast in CI.
 */
@WireMockTest
class ManualValidationWireShapeTest {

    private static final String SESSIONS_PATH = "/v2/sessions";
    private static final String QUERY_METADATA_PATH = "/v2/invoices/query/metadata";
    private static final String PERMISSIONS_PERSONAL_PATH = "/v2/permissions/query/personal/grants";

    private static final String EMPTY_QUERY_RESPONSE = """
            {"invoices": [], "hasMore": false, "isTruncated": false}
            """;

    private static final String EMPTY_PERMISSIONS_RESPONSE = """
            {"permissions": [], "hasMore": false}
            """;

    private static final String EMPTY_SESSIONS_RESPONSE = """
            {"sessions": [], "continuationToken": null}
            """;

    @Test
    void invoiceSync_export_carriesRestrictToPermanentStorageHwmDateTrue(WireMockRuntimeInfo wmInfo) {
        // A.1.1 — The key wire-level invariant of the incremental-sync
        // workflow: every export request issued by InvoiceSyncClient MUST
        // carry restrictToPermanentStorageHwmDate=true. Verified by direct
        // mapper round-trip: build a query via the internal-marked builder
        // method, run the SDK's request mapper, assert the JSON.
        InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                .permanentStorageDateFrom(OffsetDateTime.parse("2026-04-01T00:00:00Z"))
                .restrictToPermanentStorageHwm();

        var rawFilters = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers
                .toInvoiceQueryFiltersRaw(query.build());

        // The flag is wrapped as JsonNullable on the Raw — getDateRange()
        // exposes the inner Boolean via the *_JsonNullable accessor.
        var jsonNullable = rawFilters.getDateRange().getRestrictToPermanentStorageHwmDate_JsonNullable();
        assertTrue(jsonNullable.isPresent(),
                "restrictToPermanentStorageHwmDate must be set when builder.restrictToPermanentStorageHwm() called");
        assertEquals(Boolean.TRUE, jsonNullable.get());
    }

    @Test
    void queryMetadata_withAllNewFilters_serializesEachField(WireMockRuntimeInfo wmInfo) {
        // A.3 — five new query-filter fields must serialise into the wire
        // body when set.
        stubFor(post(urlPathEqualTo(QUERY_METADATA_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_QUERY_RESPONSE)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .permanentStorageDateFrom(OffsetDateTime.parse("2026-04-01T00:00:00Z"))
                    .amount(InvoiceQueryAmountType.BRUTTO, new BigDecimal("100.00"), new BigDecimal("999.99"))
                    .buyerIdentifier(BuyerIdentifierType.NIP, "9876543210")
                    .currencyCodes("PLN", "EUR")
                    .formType(InvoiceFormType.FA)
                    .invoiceTypes(InvoiceType.VAT, InvoiceType.KOR);
            client.invoices().queryInvoicesByMetadata(query.build());

            verify(postRequestedFor(urlPathEqualTo(QUERY_METADATA_PATH))
                    .withRequestBody(matchingJsonPath("$.amount.type", equalTo("Brutto")))
                    .withRequestBody(matchingJsonPath("$.amount.from", equalTo("100.0")))
                    .withRequestBody(matchingJsonPath("$.amount.to", equalTo("999.99")))
                    .withRequestBody(matchingJsonPath("$.buyerIdentifier.type", equalTo("Nip")))
                    .withRequestBody(matchingJsonPath("$.buyerIdentifier.value", equalTo("9876543210")))
                    .withRequestBody(matchingJsonPath("$.currencyCodes[0]", equalTo("PLN")))
                    .withRequestBody(matchingJsonPath("$.currencyCodes[1]", equalTo("EUR")))
                    .withRequestBody(matchingJsonPath("$.formType", equalTo("FA")))
                    .withRequestBody(matchingJsonPath("$.invoiceTypes[0]", equalTo("Vat")))
                    .withRequestBody(matchingJsonPath("$.invoiceTypes[1]", equalTo("Kor"))));
        }
    }

    @Test
    void querySessions_withOnlineFilter_buildsExpectedUrl(WireMockRuntimeInfo wmInfo) {
        // A.2.4 — GET /sessions listing must include sessionType (required
        // per OpenAPI), pageSize, and any provided filter axes. Verifies
        // the URL string construction in SessionClient.querySessionsPage.
        stubFor(get(urlPathEqualTo(SESSIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_SESSIONS_RESPONSE)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            SessionsQueryFilter filter = SessionsQueryFilter.forOnline()
                    .statuses(CommonSessionStatus.IN_PROGRESS, CommonSessionStatus.SUCCEEDED)
                    .build();
            List<SessionListItem> result = client.streamSessions(filter).toList();

            assertNotNull(result, "streamSessions returns empty list, not null, on no results");
            verify(getRequestedFor(urlPathEqualTo(SESSIONS_PATH))
                    .withQueryParam("sessionType", equalTo("Online"))
                    .withQueryParam("pageSize", matching("\\d+"))
                    .withQueryParam("statuses", matching("InProgress|Succeeded")));
        }
    }

    @Test
    void querySessions_filterRequiresSessionType() {
        // A.2.4 + reviewer CRITICAL — sessionType is required per OpenAPI;
        // record's compact constructor enforces non-null.
        assertThrows(NullPointerException.class,
                () -> new SessionsQueryFilter(null, null, null, null, null, null, null, null, null));
    }

    @Test
    void permissionsStreamPersonal_walksPageOffsetUntilHasMoreFalse(WireMockRuntimeInfo wmInfo) {
        // A.4.1 — streamPersonal must follow pageOffset until hasMore=false.
        // Stub two pages: page 0 returns hasMore=true; page 1 returns
        // hasMore=false. Verify both requests went out with the spec-max
        // pageSize=250 and pageOffset advanced.
        stubFor(post(urlPathEqualTo(PERMISSIONS_PERSONAL_PATH))
                .inScenario("personal-pagination")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .withQueryParam("pageOffset", equalTo("0"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"permissions": [], "hasMore": true}
                                """))
                .willSetStateTo("page1"));
        stubFor(post(urlPathEqualTo(PERMISSIONS_PERSONAL_PATH))
                .inScenario("personal-pagination")
                .whenScenarioStateIs("page1")
                .withQueryParam("pageOffset", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_PERMISSIONS_RESPONSE)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            client.permissions().streamPersonal(
                    io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder.create().build()).toList();

            var requests = findAll(postRequestedFor(urlPathEqualTo(PERMISSIONS_PERSONAL_PATH)));
            assertEquals(2, requests.size(), "streamPersonal must fetch both pages");
            assertTrue(requests.get(0).getUrl().contains("pageSize=250"),
                    "page 0 should use spec-max pageSize=250, was: " + requests.get(0).getUrl());
            assertTrue(requests.get(1).getUrl().contains("pageOffset=1"),
                    "page 1 should advance pageOffset to 1, was: " + requests.get(1).getUrl());
        }
    }

    @Test
    void streamMetadata_truncatedBranch_advancesDateRangeToLastRecordDate(WireMockRuntimeInfo wmInfo) {
        // A.1.2 + reviewer CRITICAL — on isTruncated=true the algorithm
        // must restart paging from the LAST record's date, not from
        // permanentStorageHwmDate (which is constant for the query).
        String lastRecordDate = "2026-04-15T10:00:00.000+02:00";
        String pageOneBody = """
                {
                  "invoices": [
                    {"ksefNumber": "20260404-FA-1234567890-AAAAAAAAAA-12",
                     "permanentStorageDate": "%s",
                     "invoiceType": "Vat"}
                  ],
                  "hasMore": true,
                  "isTruncated": true,
                  "permanentStorageHwmDate": "2026-05-01T00:00:00Z"
                }
                """.formatted(lastRecordDate);
        String pageTwoBody = """
                {"invoices": [], "hasMore": false, "isTruncated": false}
                """;
        stubFor(post(urlPathEqualTo(QUERY_METADATA_PATH))
                .inScenario("trunc-cursor")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageOneBody))
                .willSetStateTo("page2"));
        stubFor(post(urlPathEqualTo(QUERY_METADATA_PATH))
                .inScenario("trunc-cursor")
                .whenScenarioStateIs("page2")
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(pageTwoBody)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .permanentStorageDateFrom(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
            client.invoices().streamInvoicesByMetadata(query.build()).toList();

            var requests = findAll(postRequestedFor(urlPathEqualTo(QUERY_METADATA_PATH)));
            assertEquals(2, requests.size());
            String pageTwoBodyOut = requests.get(1).getBodyAsString();
            // Cursor must be the LAST record's permanentStorageDate, not the HWM.
            assertTrue(pageTwoBodyOut.contains("2026-04-15"),
                    "page 2 must restart from LAST record's date (2026-04-15), was: " + pageTwoBodyOut);
            assertTrue(requests.get(1).getUrl().contains("pageOffset=0"),
                    "page 2 must reset pageOffset on truncation, was: " + requests.get(1).getUrl());
        }
    }
}
