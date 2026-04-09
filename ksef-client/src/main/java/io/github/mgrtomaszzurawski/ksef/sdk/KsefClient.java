/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Main entry point for the KSeF Java SDK.
 * Provides access to domain-specific clients for each KSeF API area.
 */
public final class KsefClient implements AutoCloseable {

    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_CLOSED = "KsefClient has been closed";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private final KsefEnvironment environment;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryHandler retryHandler;
    private final SessionContext sessionContext;
    private final AuthClient authClient;
    private final SecurityClient securityClient;
    private volatile boolean closed;

    private KsefClient(Builder builder) {
        this.environment = builder.environment;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .build();
        this.objectMapper = createObjectMapper();
        this.retryHandler = new RetryHandler(builder.retryPolicy);
        this.sessionContext = new SessionContext();
        this.authClient = new AuthClient(this);
        this.securityClient = new SecurityClient(this);
    }

    public KsefEnvironment environment() {
        ensureOpen();
        return environment;
    }

    public HttpClient httpClient() {
        ensureOpen();
        return httpClient;
    }

    public ObjectMapper objectMapper() {
        ensureOpen();
        return objectMapper;
    }

    public RetryHandler retryHandler() {
        ensureOpen();
        return retryHandler;
    }

    public SessionContext sessionContext() {
        ensureOpen();
        return sessionContext;
    }

    public Duration readTimeout() {
        ensureOpen();
        return DEFAULT_READ_TIMEOUT;
    }

    /**
     * Access authentication operations (challenge, sign, token management).
     */
    public AuthClient auth() {
        ensureOpen();
        return authClient;
    }

    /**
     * Access security operations (public key certificate retrieval).
     */
    public SecurityClient security() {
        ensureOpen();
        return securityClient;
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_CLOSED);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static Builder builder(KsefEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException(ERR_ENVIRONMENT_NULL);
        }
        return new Builder(environment);
    }

    public static final class Builder {

        private final KsefEnvironment environment;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private RetryPolicy retryPolicy = RetryPolicy.builder().build();

        private Builder(KsefEnvironment environment) {
            this.environment = environment;
        }

        public Builder connectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; return this; }
        public Builder retryPolicy(RetryPolicy retryPolicy) { this.retryPolicy = retryPolicy; return this; }

        public KsefClient build() {
            return new KsefClient(this);
        }
    }
}
