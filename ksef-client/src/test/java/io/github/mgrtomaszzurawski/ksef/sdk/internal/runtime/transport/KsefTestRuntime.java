/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import java.net.http.HttpClient;
import java.time.Duration;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Test-only factory that constructs a real {@link KsefHttpRuntime} for
 * SDK-internal unit tests of internal clients (AuthClient, SessionClient,
 * SecurityClient, HttpSupport).
 *
 * <p>Lives in {@code sdk.internal.runtime.transport} which is NOT
 * exported via JPMS, so this class is invisible to consumers. SDK
 * internal tests reach it because their classpath includes the same
 * module's {@code internal} packages at compile-and-test time.
 *
 * <p>Lives in non-exported test infrastructure per ADR-020 — the
 * SDK's main artifact has no public seam exposing {@code HttpRuntime}.
 */
public final class KsefTestRuntime {

    private static final String KSEF_PATH_PREFIX = "/v2";
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private KsefTestRuntime() { }

    /**
     * Construct a fresh {@link HttpRuntime} pointing at the given
     * WireMock instance. Session context is empty and not activated;
     * {@link SessionContext#activate(String, String, java.time.OffsetDateTime)}
     * may be called by the test if it wants to seed an "authenticated"
     * state without driving the full WireMock auth flow.
     */
    public static HttpRuntime forWireMock(WireMockRuntimeInfo wmInfo) {
        return forWireMock(wmInfo, RetryPolicy.builder().enabled(false).build(), FeaturePolicy.defaults());
    }

    /**
     * Construct a fresh {@link HttpRuntime} with a custom retry policy.
     */
    public static HttpRuntime forWireMock(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy) {
        return forWireMock(wmInfo, retryPolicy, FeaturePolicy.defaults());
    }

    /**
     * Construct a fresh {@link HttpRuntime} with custom retry and feature
     * policies. Used by tests asserting feature-header behaviour.
     */
    public static HttpRuntime forWireMock(WireMockRuntimeInfo wmInfo,
                                          RetryPolicy retryPolicy,
                                          FeaturePolicy featurePolicy) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new KsefHttpRuntime(
                KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + KSEF_PATH_PREFIX),
                HttpClient.newHttpClient(),
                mapper,
                new RetryHandler(retryPolicy),
                new SessionContext(),
                DEFAULT_READ_TIMEOUT,
                () -> { /* no reauth in unit tests */ },
                () -> { /* no proactive auth in unit tests */ },
                featurePolicy);
    }
}
