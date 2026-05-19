/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.testdata;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnsupportedEnvironmentException;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import java.time.LocalDate;

@WireMockTest
class TestDataClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_NIP = "1111111111";
    private static final String TEST_PESEL = "82060411457";
    private static final String TEST_AUTHORIZED_NIP = "0987654321";
    private static final String TEST_DESCRIPTION = "Test data entry";
    private static final int LIMIT_INVOICE_SIZE_MB = 100;
    private static final int LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB = 200;
    private static final int LIMIT_MAX_INVOICES_ONLINE = 500;
    private static final int LIMIT_MAX_INVOICES_BATCH = 1000;
    private static final int RATE_PER_SECOND = 10;
    private static final int RATE_PER_MINUTE = 100;
    private static final int RATE_PER_HOUR = 1000;
    private static final LocalDate REVOKE_END_DATE = LocalDate.of(2026, 6, 30);
    private static final String REVOKE_END_DATE_WIRE = "2026-06-30";
    private static final String SUBUNIT_NIP_ONE = "1111111111";
    private static final String SUBUNIT_NIP_TWO = "2222222222";
    private static final String SUBUNIT_DESCRIPTION_ONE = "VAT group member 1";
    private static final String SUBUNIT_DESCRIPTION_TWO = "VAT group member 2";
    private static final String PATH_TESTDATA_SUBJECT = "/v2/testdata/subject";
    private static final String PATH_TESTDATA_ATTACHMENT_REVOKE = "/v2/testdata/attachment/revoke";

    @Test
    void createSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/subject"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().createSubject(
                    TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION).build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/subject")));
        }
    }

    @Test
    void createSubject_whenVatGroupWithSubunits_postsExpectedJsonShape(WireMockRuntimeInfo wmInfo) {
        // given — VAT_GROUP subject with two subunits. Pins wire shape:
        //   subjectType=VatGroup (wire-encoded value)
        //   subunits[].subjectNip + .description present for both entries
        // KSeF treats VAT_GROUP differently from JST in subunit handling — this
        // contract must not regress silently (e.g. enum rename, subunits dropped).
        stubFor(post(urlEqualTo(PATH_TESTDATA_SUBJECT))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().createSubject(
                    TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.VAT_GROUP, TEST_DESCRIPTION)
                            .addSubunit(SUBUNIT_NIP_ONE, SUBUNIT_DESCRIPTION_ONE)
                            .addSubunit(SUBUNIT_NIP_TWO, SUBUNIT_DESCRIPTION_TWO)
                            .build());

            // then
            verify(postRequestedFor(urlEqualTo(PATH_TESTDATA_SUBJECT))
                    .withRequestBody(matchingJsonPath("$.subjectNip", equalTo(TEST_NIP)))
                    .withRequestBody(matchingJsonPath("$.subjectType", equalTo("VatGroup")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo(TEST_DESCRIPTION)))
                    .withRequestBody(matchingJsonPath("$.subunits[0].subjectNip",
                            equalTo(SUBUNIT_NIP_ONE)))
                    .withRequestBody(matchingJsonPath("$.subunits[0].description",
                            equalTo(SUBUNIT_DESCRIPTION_ONE)))
                    .withRequestBody(matchingJsonPath("$.subunits[1].subjectNip",
                            equalTo(SUBUNIT_NIP_TWO)))
                    .withRequestBody(matchingJsonPath("$.subunits[1].description",
                            equalTo(SUBUNIT_DESCRIPTION_TWO))));
        }
    }

    @Test
    void removeSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/subject/remove"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().removeSubject(KsefIdentifier.nip(TEST_NIP));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/subject/remove")));
        }
    }

    @Test
    void createPerson_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/person"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().createPerson(
                    TestPersonCreateBuilder.create(TEST_NIP, TEST_PESEL, false, TEST_DESCRIPTION).build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/person")));
        }
    }

    @Test
    void removePerson_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/person/remove"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().removePerson(KsefIdentifier.nip(TEST_NIP));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/person/remove")));
        }
    }

    @Test
    void grantPermissions_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/permissions"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().grantPermissions(TestPermissionsGrantBuilder.create(TEST_NIP)
                    .authorizedNip(TEST_AUTHORIZED_NIP)
                    .invoiceRead()
                    .build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/permissions")));
        }
    }

    @Test
    void revokePermissions_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/permissions/revoke"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().revokePermissions(TestPermissionsRevokeBuilder.create(TEST_NIP)
                    .authorizedNip(TEST_AUTHORIZED_NIP)
                    .build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/permissions/revoke")));
        }
    }

    @Test
    void grantAttachment_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/attachment"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().grantAttachment(KsefIdentifier.nip(TEST_NIP));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/attachment")));
        }
    }

    @Test
    void revokeAttachment_withExpectedEndDate_postsBothFieldsInJsonBody(WireMockRuntimeInfo wmInfo) {
        // given — pin the wire shape of the (nip, expectedEndDate) overload:
        // body must carry {"nip":"...","expectedEndDate":"YYYY-MM-DD"} so a future
        // refactor cannot silently drop the date or rewire it under a different key.
        stubFor(post(urlEqualTo(PATH_TESTDATA_ATTACHMENT_REVOKE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().revokeAttachment(
                    io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestAttachmentRevokeRequest
                            .scheduled(KsefIdentifier.nip(TEST_NIP), REVOKE_END_DATE));

            // then
            verify(postRequestedFor(urlEqualTo(PATH_TESTDATA_ATTACHMENT_REVOKE))
                    .withRequestBody(matchingJsonPath("$.nip", equalTo(TEST_NIP)))
                    .withRequestBody(matchingJsonPath("$.expectedEndDate",
                            equalTo(REVOKE_END_DATE_WIRE))));
        }
    }

    @Test
    void blockContext_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/context/block"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().blockContext(KsefIdentifier.nip(TEST_NIP));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/context/block")));
        }
    }

    @Test
    void unblockContext_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/context/unblock"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().unblockContext(KsefIdentifier.nip(TEST_NIP));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/context/unblock")));
        }
    }

    @Test
    void setSessionLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/limits/context/session"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().setSessionLimits(TestSessionLimitsBuilder.create()
                    .onlineSession(LIMIT_INVOICE_SIZE_MB, LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB, LIMIT_MAX_INVOICES_ONLINE)
                    .batchSession(LIMIT_INVOICE_SIZE_MB, LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB, LIMIT_MAX_INVOICES_BATCH)
                    .build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/limits/context/session"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void resetSessionLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/v2/testdata/limits/context/session"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().resetSessionLimits();

            // then
            verify(deleteRequestedFor(urlEqualTo("/v2/testdata/limits/context/session"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void setSubjectLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/limits/subject/certificate"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().setSubjectLimits(
                    TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP).build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/limits/subject/certificate"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void resetSubjectLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/v2/testdata/limits/subject/certificate"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().resetSubjectLimits();

            // then
            verify(deleteRequestedFor(urlEqualTo("/v2/testdata/limits/subject/certificate"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void setRateLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/rate-limits"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().setRateLimits(TestRateLimitsBuilder.create()
                    .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                    .build());

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/rate-limits"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void resetRateLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/v2/testdata/rate-limits"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().resetRateLimits();

            // then
            verify(deleteRequestedFor(urlEqualTo("/v2/testdata/rate-limits"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void setProductionRateLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/rate-limits/production"))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createAuthenticatedClient(wmInfo)) {

            // when
            ksef.testData().applyProductionRateLimitsToTestTenant();

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/rate-limits/production"))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER, equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void createSubject_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/subject"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody("{}")));

        try (KsefClient ksef = createClient(wmInfo)) {

            // then
            var testData = ksef.testData();
            var request = TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION).build();
            assertThrows(KsefServerException.class, () -> testData.createSubject(request));
        }
    }

    @Test
    void resetSessionLimits_whenWiredToProd_throwsKsefUnsupportedEnvironmentException() {
        // given
        try (KsefClient ksef = prodWiredClient()) {

            // when / then
            assertThrows(KsefUnsupportedEnvironmentException.class,
                    ksef.testData()::resetSessionLimits);
        }
    }

    @Test
    void resetSubjectLimits_whenWiredToProd_throwsKsefUnsupportedEnvironmentException() {
        // given
        try (KsefClient ksef = prodWiredClient()) {

            // when / then
            assertThrows(KsefUnsupportedEnvironmentException.class,
                    ksef.testData()::resetSubjectLimits);
        }
    }

    @Test
    void resetRateLimits_whenWiredToProd_throwsKsefUnsupportedEnvironmentException() {
        // given
        try (KsefClient ksef = prodWiredClient()) {

            // when / then
            assertThrows(KsefUnsupportedEnvironmentException.class,
                    ksef.testData()::resetRateLimits);
        }
    }

    private static KsefClient prodWiredClient() {
        return KsefClient.builder()
                .environment(KsefEnvironment.PROD)
                .credentials(new KsefTokenCredentials("test-token", TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder().environment(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials("test-token", "1111111111"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture.newAuthenticatedClient(wmInfo, TEST_TOKEN, "1111111111");
    }
}
