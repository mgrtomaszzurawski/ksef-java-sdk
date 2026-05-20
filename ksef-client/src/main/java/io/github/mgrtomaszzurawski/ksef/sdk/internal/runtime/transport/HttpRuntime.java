/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Narrow runtime contract that {@link HttpSupport} needs from its host.
 *
 * <p>Introduced to break the layering inversion that had {@code HttpSupport}
 * (transport, low-level) importing {@code KsefClient} (the high-level facade).
 * {@code KsefClient} implements this interface; {@code HttpSupport} depends
 * only on the abstraction.
 *
 * @since 1.0.0
 */
public interface HttpRuntime {

    /** Base URL for the configured KSeF environment (no trailing slash). */
    String baseUrl();

    /** Underlying JDK HTTP client. */
    HttpClient httpClient();

    /** Session context — current JWT, reference number, etc. */
    SessionContext sessionContext();

    /** Retry executor for HTTP calls. */
    RetryHandler retryHandler();

    /** Jackson mapper configured for KSeF response/request shapes. */
    ObjectMapper objectMapper();

    /** Per-request read timeout. */
    Duration readTimeout();

    /** Trigger lazy re-authentication on HTTP 401. */
    void reauthenticate();

    /**
     * Return the current access token, authenticating proactively if no token
     * has been issued yet. Domain clients call this before every protected
     * request so the first call after {@code KsefClient} construction does not
     * leave with {@code Authorization: Bearer null}.
     *
     * <p>Once a token exists the call is cheap (no I/O). Stale-token recovery
     * remains driven by {@link #reauthenticate()} on HTTP 401.
     */
    String requireToken();

    /**
     * Active KSeF feature policy (UPO version, problem-details opt-in).
     * Default {@link FeaturePolicy#defaults()} preserves pre-1.0 behavior.
     */
    FeaturePolicy featurePolicy();

    /**
     * Whether the active session was established with certificate-based
     * authentication (XAdES via PKCS#12 or raw certificate). Certificate
     * domain operations ({@code /certificates/enrollments},
     * {@code /certificates/enrollments/data}) are server-side restricted
     * to certificate-authenticated callers; this hook lets the SDK
     * preflight before a token-authenticated caller hits a 403.
     *
     * <p>Default {@code true} so test runtimes and mocks (which never
     * authenticate) do not spuriously fail the preflight.
     */
    default boolean isAuthenticatedViaCertificate() {
        return true;
    }
}
