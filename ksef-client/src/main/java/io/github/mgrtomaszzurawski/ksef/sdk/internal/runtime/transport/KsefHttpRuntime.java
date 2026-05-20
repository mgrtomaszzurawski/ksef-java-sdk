/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * Default {@link HttpRuntime} adapter used by {@code KsefClient}. Decouples
 * the transport contract from the public facade so {@code KsefClient} no
 * longer needs to {@code implements HttpRuntime} (which would force the
 * runtime accessors onto the consumer-facing API surface).
 *
 * <p>Public for in-module instantiation by {@code KsefClient}; the enclosing
 * package is not exported via JPMS, so this type is invisible to consumers.
 *
 * <p>Constructor takes three grouped parameter records ({@link Transport},
 * {@link AuthHooks}, {@link FeaturePolicy}) instead of nine flat parameters.
 * This closes Sonar S107 without losing the explicit shape of each
 * collaborator (Codex round-9 follow-up).
 *
 * @since 1.0.0
 */
public final class KsefHttpRuntime implements HttpRuntime {

    private final Transport transport;
    private final AuthHooks auth;
    private final FeaturePolicy featurePolicy;

    public KsefHttpRuntime(Transport transport, AuthHooks auth, FeaturePolicy featurePolicy) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
        this.featurePolicy = Objects.requireNonNull(featurePolicy, "featurePolicy must not be null");
    }

    @Override
    public String baseUrl() { return transport.environment().baseUrl(); }

    @Override
    public HttpClient httpClient() { return transport.httpClient(); }

    @Override
    public SessionContext sessionContext() { return auth.sessionContext(); }

    @Override
    public RetryHandler retryHandler() { return transport.retryHandler(); }

    @Override
    public ObjectMapper objectMapper() { return transport.objectMapper(); }

    @Override
    public Duration readTimeout() { return transport.readTimeout(); }

    @Override
    public void reauthenticate() { auth.reauthHook().run(); }

    @Override
    public String requireToken() {
        if (!auth.sessionContext().isActive()) {
            auth.proactiveAuthHook().run();
        }
        return auth.sessionContext().token();
    }

    @Override
    public FeaturePolicy featurePolicy() { return featurePolicy; }

    /**
     * Plain transport-layer collaborators — environment URL, HTTP client,
     * JSON mapper, retry handler, read timeout. No auth/session state.
     *
     * <p>The mutable collaborators ({@link HttpClient}, {@link ObjectMapper},
     * {@link RetryHandler}) are intentionally shared by reference —
     * defensive copy makes no sense for an HTTP client or JSON mapper.
     * SpotBugs' {@code EI_EXPOSE_REP/REP2} on the auto-generated record
     * accessors is silenced via the {@code KsefHttpRuntime$.*} entry in
     * {@code spotbugs-exclude.xml}.
     */
    public record Transport(
            KsefEnvironment environment,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            RetryHandler retryHandler,
            Duration readTimeout) {

        public Transport {
            Objects.requireNonNull(environment, "environment must not be null");
            Objects.requireNonNull(httpClient, "httpClient must not be null");
            Objects.requireNonNull(objectMapper, "objectMapper must not be null");
            Objects.requireNonNull(retryHandler, "retryHandler must not be null");
            Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        }
    }

    /**
     * AuthSessions/session collaborators — session-context store and the two
     * lifecycle callbacks the runtime invokes when auth needs to happen
     * proactively (no token yet) or reactively (401 response).
     *
     * <p>Reference-sharing semantics as {@link Transport}; the
     * {@code EI_EXPOSE_REP/REP2} SpotBugs reports on the auto-generated
     * accessors are suppressed via the same {@code KsefHttpRuntime$.*}
     * exclusion in {@code spotbugs-exclude.xml}.
     */
    public record AuthHooks(
            SessionContext sessionContext,
            Runnable reauthHook,
            Runnable proactiveAuthHook,
            java.util.function.BooleanSupplier certificateAuthCheck) {

        public AuthHooks {
            Objects.requireNonNull(sessionContext, "sessionContext must not be null");
            Objects.requireNonNull(reauthHook, "reauthHook must not be null");
            Objects.requireNonNull(proactiveAuthHook, "proactiveAuthHook must not be null");
            Objects.requireNonNull(certificateAuthCheck, "certificateAuthCheck must not be null");
        }
    }

    @Override
    public boolean isAuthenticatedViaCertificate() {
        return auth.certificateAuthCheck().getAsBoolean();
    }
}
