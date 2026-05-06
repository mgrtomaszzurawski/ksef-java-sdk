/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
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
     * <p>Plain {@code final class} (not a {@code record}) so SpotBugs does
     * not flag the mutable fields ({@link HttpClient}, {@link ObjectMapper},
     * {@link RetryHandler}) as {@code EI_EXPOSE_REP}. These collaborators
     * are intentionally shared by reference — defensive copy makes no
     * sense for an HTTP client or JSON mapper.
     */
    public static final class Transport {
        private final KsefEnvironment environment;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final RetryHandler retryHandler;
        private final Duration readTimeout;

        public Transport(KsefEnvironment environment,
                          HttpClient httpClient,
                          ObjectMapper objectMapper,
                          RetryHandler retryHandler,
                          Duration readTimeout) {
            this.environment = Objects.requireNonNull(environment, "environment must not be null");
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
            this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
            this.retryHandler = Objects.requireNonNull(retryHandler, "retryHandler must not be null");
            this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        }

        KsefEnvironment environment() { return environment; }
        HttpClient httpClient() { return httpClient; }
        ObjectMapper objectMapper() { return objectMapper; }
        RetryHandler retryHandler() { return retryHandler; }
        Duration readTimeout() { return readTimeout; }
    }

    /**
     * Auth/session collaborators — session-context store and the two
     * lifecycle callbacks the runtime invokes when auth needs to happen
     * proactively (no token yet) or reactively (401 response).
     *
     * <p>Same rationale as {@link Transport} for not using a {@code record}.
     */
    public static final class AuthHooks {
        private final SessionContext sessionContext;
        private final Runnable reauthHook;
        private final Runnable proactiveAuthHook;
        private final java.util.function.BooleanSupplier certificateAuthCheck;

        public AuthHooks(SessionContext sessionContext,
                          Runnable reauthHook,
                          Runnable proactiveAuthHook,
                          java.util.function.BooleanSupplier certificateAuthCheck) {
            this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
            this.reauthHook = Objects.requireNonNull(reauthHook, "reauthHook must not be null");
            this.proactiveAuthHook = Objects.requireNonNull(proactiveAuthHook, "proactiveAuthHook must not be null");
            this.certificateAuthCheck = Objects.requireNonNull(certificateAuthCheck,
                    "certificateAuthCheck must not be null");
        }

        SessionContext sessionContext() { return sessionContext; }
        Runnable reauthHook() { return reauthHook; }
        Runnable proactiveAuthHook() { return proactiveAuthHook; }
        java.util.function.BooleanSupplier certificateAuthCheck() { return certificateAuthCheck; }
    }

    @Override
    public boolean isAuthenticatedViaCertificate() {
        return auth.certificateAuthCheck().getAsBoolean();
    }
}
