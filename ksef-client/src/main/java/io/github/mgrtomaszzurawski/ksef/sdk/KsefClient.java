/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;

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
    private final SessionClient sessionClient;
    private final InvoiceClient invoiceClient;
    private final TokenClient tokenClient;
    private final PermissionClient permissionClient;
    private final CertificateClient certificateClient;
    private final LimitsClient limitsClient;
    private final RateLimitClient rateLimitClient;
    private final TestDataClient testDataClient;
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
        this.sessionClient = new SessionClient(this);
        this.invoiceClient = new InvoiceClient(this);
        this.tokenClient = new TokenClient(this);
        this.permissionClient = new PermissionClient(this);
        this.certificateClient = new CertificateClient(this);
        this.limitsClient = new LimitsClient(this);
        this.rateLimitClient = new RateLimitClient(this);
        this.testDataClient = new TestDataClient(this);
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

    /**
     * Access session operations (online/batch lifecycle, invoice submission, UPO retrieval).
     */
    public SessionClient sessions() {
        ensureOpen();
        return sessionClient;
    }

    /**
     * Access invoice operations (query metadata, retrieve, export).
     */
    public InvoiceClient invoices() {
        ensureOpen();
        return invoiceClient;
    }

    /**
     * Access token management operations (generate, list, get status, revoke).
     */
    public TokenClient tokens() {
        ensureOpen();
        return tokenClient;
    }

    /**
     * Access permission management operations (grant, revoke, query permissions).
     */
    public PermissionClient permissions() {
        ensureOpen();
        return permissionClient;
    }

    /**
     * Access certificate management operations (enroll, retrieve, revoke, query).
     */
    public CertificateClient certificates() {
        ensureOpen();
        return certificateClient;
    }

    /**
     * Access session and subject limit queries.
     */
    public LimitsClient limits() {
        ensureOpen();
        return limitsClient;
    }

    /**
     * Access API rate limit information.
     */
    public RateLimitClient rateLimits() {
        ensureOpen();
        return rateLimitClient;
    }

    /**
     * Access test environment data management operations.
     */
    public TestDataClient testData() {
        ensureOpen();
        return testDataClient;
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
        mapper.registerModule(new JsonNullableModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
