/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.testdata;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class TestDataClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
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

    @Test
    void createSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/subject"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().createSubject(
                    TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION));

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/subject")));
        }
    }

    @Test
    void removeSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/subject/remove"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().removeSubject(TEST_NIP);

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
                    TestPersonCreateBuilder.create(TEST_NIP, TEST_PESEL, false, TEST_DESCRIPTION));

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
            ksef.testData().removePerson(TEST_NIP);

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
                    .invoiceRead());

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
                    .authorizedNip(TEST_AUTHORIZED_NIP));

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
            ksef.testData().grantAttachment(TEST_NIP);

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/attachment")));
        }
    }

    @Test
    void revokeAttachment_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/attachment/revoke"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().revokeAttachment(TEST_NIP);

            // then
            verify(postRequestedFor(urlEqualTo("/v2/testdata/attachment/revoke")));
        }
    }

    @Test
    void blockContext_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/v2/testdata/context/block"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (KsefClient ksef = createClient(wmInfo)) {

            // when
            ksef.testData().blockContext(TestDataIdentifierType.NIP, TEST_NIP);

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
            ksef.testData().unblockContext(TestDataIdentifierType.NIP, TEST_NIP);

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
                    .batchSession(LIMIT_INVOICE_SIZE_MB, LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB, LIMIT_MAX_INVOICES_BATCH));

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
                    TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP));

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
                    .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR));

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
            ksef.testData().setProductionRateLimits();

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
            var builder = TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION);
            assertThrows(KsefServerException.class, () -> testData.createSubject(builder));
        }
    }

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createClient(wmInfo);
        ksef.activateSessionForTests(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
