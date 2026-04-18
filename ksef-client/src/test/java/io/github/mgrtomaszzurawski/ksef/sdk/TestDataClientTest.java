/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TestSubjectLimitsBuilder;
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

@WireMockTest
class TestDataClientTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260404-SE-1234567890-ABCDEF1234-01";
    private static final String TEST_NIP = "1234567890";
    private static final String TEST_PESEL = "82060411457";
    private static final String TEST_AUTHORIZED_NIP = "0987654321";
    private static final String TEST_DESCRIPTION = "Test data entry";

    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final int LIMIT_INVOICE_SIZE_MB = 100;
    private static final int LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB = 200;
    private static final int LIMIT_MAX_INVOICES_ONLINE = 500;
    private static final int LIMIT_MAX_INVOICES_BATCH = 1000;
    private static final int RATE_PER_SECOND = 10;
    private static final int RATE_PER_MINUTE = 100;
    private static final int RATE_PER_HOUR = 1000;

    // --- Subject tests (unauthenticated) ---

    @Test
    void createSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/subject"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().createSubject(
                TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/subject")));
    }

    @Test
    void removeSubject_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/subject/remove"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().removeSubject(TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/subject/remove")));
    }

    // --- Person tests (unauthenticated) ---

    @Test
    void createPerson_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/person"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().createPerson(
                TestPersonCreateBuilder.create(TEST_NIP, TEST_PESEL, false, TEST_DESCRIPTION));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/person")));
    }

    @Test
    void removePerson_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/person/remove"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().removePerson(TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/person/remove")));
    }

    // --- Permissions tests (unauthenticated) ---

    @Test
    void grantPermissions_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/permissions"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().grantPermissions(TestPermissionsGrantBuilder.create(TEST_NIP)
                .authorizedNip(TEST_AUTHORIZED_NIP)
                .invoiceRead());

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/permissions")));
    }

    @Test
    void revokePermissions_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/permissions/revoke"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().revokePermissions(TestPermissionsRevokeBuilder.create(TEST_NIP)
                .authorizedNip(TEST_AUTHORIZED_NIP));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/permissions/revoke")));
    }

    // --- Attachment tests (unauthenticated) ---

    @Test
    void grantAttachment_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/attachment"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().grantAttachment(TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/attachment")));
    }

    @Test
    void revokeAttachment_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/attachment/revoke"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().revokeAttachment(TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/attachment/revoke")));
    }

    // --- Context blocking tests (unauthenticated) ---

    @Test
    void blockContext_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/context/block"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().blockContext(TestDataIdentifierType.NIP, TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/context/block")));
    }

    @Test
    void unblockContext_whenCalled_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/context/unblock"))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createClient(wmInfo);

        // when
        ksef.testData().unblockContext(TestDataIdentifierType.NIP, TEST_NIP);

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/context/unblock")));
    }

    // --- Session limits tests (authenticated) ---

    @Test
    void setSessionLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/limits/context/session"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().setSessionLimits(TestSessionLimitsBuilder.create()
                .onlineSession(LIMIT_INVOICE_SIZE_MB, LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB, LIMIT_MAX_INVOICES_ONLINE)
                .batchSession(LIMIT_INVOICE_SIZE_MB, LIMIT_INVOICE_WITH_ATTACHMENT_SIZE_MB, LIMIT_MAX_INVOICES_BATCH));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/limits/context/session"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void resetSessionLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/api/v2/testdata/limits/context/session"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().resetSessionLimits();

        // then
        verify(deleteRequestedFor(urlEqualTo("/api/v2/testdata/limits/context/session"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    // --- Subject limits tests (authenticated) ---

    @Test
    void setSubjectLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/limits/subject/certificate"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().setSubjectLimits(
                TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/limits/subject/certificate"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void resetSubjectLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/api/v2/testdata/limits/subject/certificate"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().resetSubjectLimits();

        // then
        verify(deleteRequestedFor(urlEqualTo("/api/v2/testdata/limits/subject/certificate"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    // --- Rate limits tests (authenticated) ---

    @Test
    void setRateLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/rate-limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().setRateLimits(TestRateLimitsBuilder.create()
                .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR));

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/rate-limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void resetRateLimits_whenAuthenticated_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo("/api/v2/testdata/rate-limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().resetRateLimits();

        // then
        verify(deleteRequestedFor(urlEqualTo("/api/v2/testdata/rate-limits"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    @Test
    void setProductionRateLimits_whenAuthenticated_sendsPostRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/rate-limits/production"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        KsefClient ksef = createAuthenticatedClient(wmInfo);

        // when
        ksef.testData().setProductionRateLimits();

        // then
        verify(postRequestedFor(urlEqualTo("/api/v2/testdata/rate-limits/production"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN)));
    }

    // --- Error tests ---

    @Test
    void createSubject_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo("/api/v2/testdata/subject"))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody("{}")));

        KsefClient ksef = createClient(wmInfo);

        // then
        assertThrows(KsefServerException.class,
                () -> ksef.testData().createSubject(
                        TestSubjectCreateBuilder.create(TEST_NIP, TestSubjectType.JST, TEST_DESCRIPTION)));
    }

    // --- Helpers ---

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }

    private static KsefClient createAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        KsefClient ksef = createClient(wmInfo);
        ksef.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, null);
        return ksef;
    }
}
