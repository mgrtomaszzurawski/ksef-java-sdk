/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Default {@link HttpRuntime} adapter used by {@code KsefClient}. Decouples
 * the transport contract from the public facade so {@code KsefClient} no
 * longer needs to {@code implements HttpRuntime} (which would force the
 * runtime accessors onto the consumer-facing API surface).
 *
 * <p>Public for in-module instantiation by {@code KsefClient}; the enclosing
 * package is not exported via JPMS, so this type is invisible to consumers.
 */
public final class KsefHttpRuntime implements HttpRuntime {

    private final KsefEnvironment environment;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryHandler retryHandler;
    private final SessionContext sessionContext;
    private final Duration readTimeout;
    private final Runnable reauthHook;
    private final Runnable proactiveAuthHook;

    public KsefHttpRuntime(KsefEnvironment environment,
                           HttpClient httpClient,
                           ObjectMapper objectMapper,
                           RetryHandler retryHandler,
                           SessionContext sessionContext,
                           Duration readTimeout,
                           Runnable reauthHook,
                           Runnable proactiveAuthHook) {
        this.environment = environment;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.retryHandler = retryHandler;
        this.sessionContext = sessionContext;
        this.readTimeout = readTimeout;
        this.reauthHook = reauthHook;
        this.proactiveAuthHook = proactiveAuthHook;
    }

    @Override
    public String baseUrl() { return environment.baseUrl(); }

    @Override
    public HttpClient httpClient() { return httpClient; }

    @Override
    public SessionContext sessionContext() { return sessionContext; }

    @Override
    public RetryHandler retryHandler() { return retryHandler; }

    @Override
    public ObjectMapper objectMapper() { return objectMapper; }

    @Override
    public Duration readTimeout() { return readTimeout; }

    @Override
    public void reauthenticate() { reauthHook.run(); }

    @Override
    public String requireToken() {
        if (!sessionContext.isActive()) {
            proactiveAuthHook.run();
        }
        return sessionContext.token();
    }
}
