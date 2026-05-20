/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.UpoVersion;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * Asserts the wire behaviour of {@link FeaturePolicy} against
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport}.
 *
 * <p>Covers TC-FEAT-001..003:
 * <ul>
 *   <li>default policy sends {@code X-Error-Format: problem-details}
 *       and no {@code X-KSeF-Feature} header on non-UPO calls,</li>
 *   <li>{@code UpoVersion.V4_3} injects {@code X-KSeF-Feature: upo-v4-3}
 *       only on UPO endpoints,</li>
 *   <li>{@code problemDetails=false} suppresses {@code X-Error-Format}
 *       on every call.</li>
 * </ul>
 */
@WireMockTest
class FeaturePolicyHeadersTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String TEST_INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String UPO_PATH = SESSIONS_BASE + "/" + TEST_SESSION_REF
            + "/invoices/" + TEST_INVOICE_REF + "/upo";
    private static final String STATUS_PATH = SESSIONS_BASE + "/" + TEST_SESSION_REF;
    private static final String X_ERROR_FORMAT = "X-Error-Format";
    private static final String X_KSEF_FEATURE = "X-KSeF-Feature";
    private static final String PROBLEM_DETAILS = "problem-details";
    private static final String UPO_V4_3 = "upo-v4-3";
    private static final String OCTET_STREAM = "application/octet-stream";

    private static final String STATUS_RESPONSE = """
            {
              "status": {"code": 200, "description": "Active"},
              "dateCreated": "2026-04-18T12:00:00+02:00"
            }
            """;

    private static final byte[] FAKE_UPO_BYTES = "<UPO>x</UPO>".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Test
    void defaultPolicy_addsProblemDetailsHeader_andNoFeatureHeaderOnNonUpoPaths(WireMockRuntimeInfo wmInfo) {
        // given — default policy: problemDetails=true, upoVersion=DEFAULT
        stubFor(get(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_RESPONSE)));

        HttpRuntime runtime = activatedRuntime(wmInfo, FeaturePolicy.defaults());
        SessionClient sessionClient = new SessionClient(runtime);

        // when
        sessionClient.getStatus(TEST_SESSION_REF);

        // then — Problem-Details on; no UPO feature header on a non-/upo path
        verify(getRequestedFor(urlEqualTo(STATUS_PATH))
                .withHeader(X_ERROR_FORMAT, equalTo(PROBLEM_DETAILS))
                .withHeader(X_KSEF_FEATURE, absent()));
    }

    @Test
    void defaultPolicy_doesNotAddUpoFeatureHeader_evenOnUpoEndpointWhenVersionIsDefault(WireMockRuntimeInfo wmInfo) {
        // given — DEFAULT UpoVersion = no header, even on /upo
        stubFor(get(urlEqualTo(UPO_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, OCTET_STREAM)
                        .withBody(FAKE_UPO_BYTES)));

        HttpRuntime runtime = activatedRuntime(wmInfo, FeaturePolicy.defaults());
        SessionClient sessionClient = new SessionClient(runtime);

        // when
        sessionClient.getUpoByInvoiceReference(TEST_SESSION_REF, TEST_INVOICE_REF);

        // then
        verify(getRequestedFor(urlEqualTo(UPO_PATH))
                .withHeader(X_ERROR_FORMAT, equalTo(PROBLEM_DETAILS))
                .withHeader(X_KSEF_FEATURE, absent()));
    }

    @Test
    void v43Policy_addsUpoFeatureHeader_onUpoEndpoints(WireMockRuntimeInfo wmInfo) {
        // given — V4_3 UPO version selected
        stubFor(get(urlEqualTo(UPO_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, OCTET_STREAM)
                        .withBody(FAKE_UPO_BYTES)));

        FeaturePolicy v43 = FeaturePolicy.builder().upoVersion(UpoVersion.V4_3).build();
        HttpRuntime runtime = activatedRuntime(wmInfo, v43);
        SessionClient sessionClient = new SessionClient(runtime);

        // when
        sessionClient.getUpoByInvoiceReference(TEST_SESSION_REF, TEST_INVOICE_REF);

        // then
        verify(getRequestedFor(urlEqualTo(UPO_PATH))
                .withHeader(X_KSEF_FEATURE, equalTo(UPO_V4_3))
                .withHeader(X_ERROR_FORMAT, equalTo(PROBLEM_DETAILS)));
    }

    @Test
    void v43Policy_doesNotAddUpoFeatureHeader_onNonUpoEndpoints(WireMockRuntimeInfo wmInfo) {
        // given — V4_3 selected, but the call is to /sessions/{ref} (no /upo segment)
        stubFor(get(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_RESPONSE)));

        FeaturePolicy v43 = FeaturePolicy.builder().upoVersion(UpoVersion.V4_3).build();
        HttpRuntime runtime = activatedRuntime(wmInfo, v43);
        SessionClient sessionClient = new SessionClient(runtime);

        // when
        sessionClient.getStatus(TEST_SESSION_REF);

        // then — no feature header on non-UPO call
        verify(getRequestedFor(urlEqualTo(STATUS_PATH))
                .withHeader(X_KSEF_FEATURE, absent()));
    }

    @Test
    void problemDetailsDisabled_omitsErrorFormatHeaderOnDelete(WireMockRuntimeInfo wmInfo) {
        // given — opt-out of Problem Details, exercise DELETE path (Codex round-9 F8 —
        // earlier rounds hardcoded the header on DELETE; the fix must hold).
        String authSessionsCurrent = "/v2/auth/sessions/current";
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(
                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo(authSessionsCurrent))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        FeaturePolicy noProblemDetails = FeaturePolicy.builder().problemDetails(false).build();
        HttpRuntime runtime = activatedRuntime(wmInfo, noProblemDetails);
        io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient authClient =
                new io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient(runtime);

        // when
        authClient.terminateCurrentSession();

        // then — DELETE must NOT carry X-Error-Format when policy says no
        com.github.tomakehurst.wiremock.client.WireMock.verify(
                com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(
                        com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo(authSessionsCurrent))
                        .withHeader(X_ERROR_FORMAT, absent()));
    }

    @Test
    void problemDetailsDisabled_omitsErrorFormatHeader(WireMockRuntimeInfo wmInfo) {
        // given — opt-out of Problem Details
        stubFor(get(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_RESPONSE)));

        FeaturePolicy noProblemDetails = FeaturePolicy.builder().problemDetails(false).build();
        HttpRuntime runtime = activatedRuntime(wmInfo, noProblemDetails);
        SessionClient sessionClient = new SessionClient(runtime);

        // when
        sessionClient.getStatus(TEST_SESSION_REF);

        // then
        verify(getRequestedFor(urlEqualTo(STATUS_PATH))
                .withHeader(X_ERROR_FORMAT, absent()));
    }

    @Test
    void featurePolicy_buildersDefault_keepsProblemDetailsTrueAndUpoVersionDefault() {
        FeaturePolicy fresh = FeaturePolicy.builder().build();
        org.junit.jupiter.api.Assertions.assertEquals(UpoVersion.DEFAULT, fresh.upoVersion());
        org.junit.jupiter.api.Assertions.assertTrue(fresh.problemDetails());
    }

    @Test
    void featurePolicy_rejectsNullUpoVersion() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new FeaturePolicy(null, true, false));
    }

    @Test
    void enforceXadesCompliance_default_isFalse() {
        FeaturePolicy fresh = FeaturePolicy.defaults();
        org.junit.jupiter.api.Assertions.assertFalse(fresh.enforceXadesCompliance());
    }

    @Test
    void enforceXadesCompliance_builderSetsTrue() {
        FeaturePolicy strict = FeaturePolicy.builder().enforceXadesCompliance(true).build();
        org.junit.jupiter.api.Assertions.assertTrue(strict.enforceXadesCompliance());
    }

    private static HttpRuntime activatedRuntime(WireMockRuntimeInfo wmInfo, FeaturePolicy featurePolicy) {
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo,
                RetryPolicy.builder().enabled(false).build(), featurePolicy);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        return runtime;
    }
}
