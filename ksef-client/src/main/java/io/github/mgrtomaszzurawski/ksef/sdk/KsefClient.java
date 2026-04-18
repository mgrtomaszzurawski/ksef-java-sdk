/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CertificateLoader;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificateUsage;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the KSeF Java SDK.
 *
 * <p>Provides authentication, session management, and access to domain-specific clients
 * for each KSeF API area. Use with try-with-resources.
 *
 * <p>Example:
 * <pre>{@code
 * KsefCredentials credentials = new KsefTokenCredentials("my-token", "1234567890");
 *
 * try (KsefClient client = KsefClient.builder(KsefEnvironment.TEST)
 *         .credentials(credentials)
 *         .build()) {
 *
 *     client.authenticate();
 *
 *     try (KsefSession session = client.openSession(FormCode.FA2)) {
 *         session.send(invoiceXmlBytes);
 *     }
 * }
 * }</pre>
 */
public final class KsefClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KsefClient.class);

    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_CREDENTIALS_NULL = "credentials must not be null";
    private static final String ERR_FORM_CODE_NULL = "formCode must not be null";
    private static final String ERR_CLOSED = "KsefClient has been closed";
    private static final String ERR_AUTH_TIMEOUT = "Authentication polling timed out";
    private static final String ERR_NO_CERT = "No certificate found with usage: ";
    private static final String ERR_KEY_EXTRACT = "Failed to extract public key from certificate";
    private static final String ERR_INTERRUPTED = "Interrupted while polling";
    private static final String CERT_TYPE_X509 = "X.509";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final int AUTH_POLL_DELAY_MS = 2000;
    private static final int AUTH_POLL_MAX_ATTEMPTS = 15;
    private static final int STATUS_CODE_OK = 200;

    private final KsefEnvironment environment;
    private final KsefCredentials credentials;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryHandler retryHandler;
    private final SessionContext sessionContext;
    private final Duration readTimeout;

    // Internal domain clients
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

    private final Map<PublicKeyCertificateUsage, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private volatile boolean authenticated;
    private volatile boolean closed;

    private KsefClient(Builder builder) {
        this.environment = builder.environment;
        this.credentials = builder.credentials;
        this.readTimeout = DEFAULT_READ_TIMEOUT;
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

    // --- Authentication ---

    /**
     * Authenticate with KSeF using the configured credentials.
     *
     * <p>Handles the full authentication flow:
     * <ol>
     *   <li>Request challenge from KSeF</li>
     *   <li>Encrypt token / sign with XAdES (depending on credential type)</li>
     *   <li>Poll authentication status until ready</li>
     *   <li>Redeem operation token for access + refresh tokens</li>
     * </ol>
     *
     * <p>This method is called automatically by {@link #openSession(FormCode)} and other
     * operations that require authentication. Call it explicitly to validate credentials
     * early or to control timing.
     *
     * <p>Thread-safe. If already authenticated, this is a no-op.
     */
    public synchronized void authenticate() {
        ensureOpen();
        if (authenticated) {
            return;
        }
        doAuthenticate();
        authenticated = true;
        LOG.info("Authenticated with KSeF as NIP {}", credentials.nip());
    }

    /**
     * Open an interactive (online) KSeF session for sending invoices.
     *
     * <p>Authenticates lazily if not already authenticated. Generates AES encryption key,
     * encrypts it with the KSeF public key, and opens the session. The returned
     * {@link KsefSession} handles all invoice encryption internally.
     *
     * <p>KSeF allows only one active online session per NIP at a time.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA2})
     * @return an open session — use with try-with-resources
     */
    public KsefSession openSession(FormCode formCode) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector));

        OnlineSession session = sessionClient.openOnline(request);
        LOG.info("Opened KSeF session {}, formCode={}", session.referenceNumber(), formCode);

        return new KsefSession(sessionClient, session.referenceNumber(), aesKey, initVector);
    }

    /**
     * Terminate the current authentication session.
     * Clears all session state. After calling this, {@link #authenticate()} must be
     * called again (explicitly or lazily) before any further operations.
     */
    public void terminateAuth() {
        ensureOpen();
        authClient.terminateCurrentSession();
        authenticated = false;
        publicKeyCache.clear();
        LOG.info("Terminated KSeF auth session");
    }

    // --- Public domain client accessors ---

    /**
     * Access invoice query and export operations.
     * Requires authentication (lazy auth if needed).
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

    // --- Internal client accessors (for advanced use / probe utilities) ---

    /** Low-level auth client. Prefer {@link #authenticate()} for normal use. */
    public AuthClient auth() { return authClient; }

    /** Low-level session client. Prefer {@link #openSession(FormCode)} for normal use. */
    public SessionClient sessions() { return sessionClient; }

    /** Low-level security client. Keys are fetched automatically by the SDK. */
    public SecurityClient security() { return securityClient; }

    // --- Infrastructure accessors (used by internal clients across packages) ---

    /** SDK infrastructure — not part of the consumer-facing API. */
    public KsefEnvironment environment() { return environment; }

    /** SDK infrastructure — not part of the consumer-facing API. */
    public HttpClient httpClient() { return httpClient; }

    /** SDK infrastructure — not part of the consumer-facing API. */
    public ObjectMapper objectMapper() { return objectMapper; }

    /** SDK infrastructure — not part of the consumer-facing API. */
    public RetryHandler retryHandler() { return retryHandler; }

    /** SDK infrastructure — not part of the consumer-facing API. */
    public SessionContext sessionContext() { return sessionContext; }

    /** SDK infrastructure — not part of the consumer-facing API. */
    public Duration readTimeout() { return readTimeout; }

    @Override
    public void close() {
        closed = true;
    }

    // --- Builder ---

    public static Builder builder(KsefEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException(ERR_ENVIRONMENT_NULL);
        }
        return new Builder(environment);
    }

    public static final class Builder {

        private final KsefEnvironment environment;
        private KsefCredentials credentials;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private RetryPolicy retryPolicy = RetryPolicy.builder().build();

        private Builder(KsefEnvironment environment) {
            this.environment = environment;
        }

        /**
         * Set the authentication credentials. Required.
         *
         * @param credentials KSeF credentials (token, certificate, or PKCS#12)
         * @return this builder
         */
        public Builder credentials(KsefCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public KsefClient build() {
            Objects.requireNonNull(credentials, ERR_CREDENTIALS_NULL);
            return new KsefClient(this);
        }
    }

    // --- Private helpers ---

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_CLOSED);
        }
    }

    private void ensureAuthenticated() {
        if (!authenticated) {
            authenticate();
        }
    }

    private void doAuthenticate() {
        if (credentials instanceof KsefTokenCredentials token) {
            authenticateWithToken(token);
        } else if (credentials instanceof KsefCertificateCredentials cert) {
            authenticateWithCertificate(cert.certificate(), cert.privateKey(), cert.nip());
        } else if (credentials instanceof KsefPkcs12Credentials pkcs12) {
            authenticateWithPkcs12(pkcs12);
        }
    }

    private void authenticateWithToken(KsefTokenCredentials credentials) {
        PublicKey tokenKey = getPublicKey(PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION);
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithToken(challenge, credentials.ksefToken(),
                credentials.nip(), tokenKey);
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithCertificate(X509Certificate certificate, PrivateKey privateKey, String nip) {
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithXades(challenge.challenge(), certificate, privateKey, nip);
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithPkcs12(KsefPkcs12Credentials credentials) {
        KeyStore keyStore = CertificateLoader.loadKeyStore(credentials.keystorePath(), credentials.password());
        String alias = CertificateLoader.getFirstAlias(keyStore);
        PrivateKey privateKey = CertificateLoader.getPrivateKey(keyStore, alias, credentials.password());
        X509Certificate certificate = CertificateLoader.getCertificate(keyStore, alias);
        authenticateWithCertificate(certificate, privateKey, credentials.nip());
    }

    private void pollAuthStatus() {
        String refNumber = sessionContext.referenceNumber();
        for (int attempt = 0; attempt < AUTH_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(AUTH_POLL_DELAY_MS);
            AuthenticationStatus status = authClient.getStatus(refNumber);
            if (status.status() != null && status.status().code() == STATUS_CODE_OK) {
                return;
            }
        }
        throw new IllegalStateException(ERR_AUTH_TIMEOUT);
    }

    private PublicKey getPublicKey(PublicKeyCertificateUsage usage) {
        return publicKeyCache.computeIfAbsent(usage, this::fetchPublicKey);
    }

    private PublicKey fetchPublicKey(PublicKeyCertificateUsage usage) {
        PublicKeyCertificate cert = securityClient.getPublicKeyCertificates().stream()
                .filter(c -> c.usage().contains(usage))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_CERT + usage));
        return extractPublicKey(cert.certificate());
    }

    private static PublicKey extractPublicKey(byte[] certBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(certBytes));
            return x509.getPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException(ERR_KEY_EXTRACT, ex);
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, ex);
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
}
