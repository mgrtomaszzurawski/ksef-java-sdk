/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.client.model.BatchFileInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BatchFilePartInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.CertificateClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.LimitsClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.RateLimitClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.PeppolClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.PermissionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.TokenClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.CertificateClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.InvoiceClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.LimitsClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.RateLimitClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol.PeppolClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.PermissionClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.TestDataClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.TokenClientImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CertificateLoader;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.RetryHandler;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class KsefClient implements AutoCloseable, HttpRuntime {

    private static final Logger LOG = LoggerFactory.getLogger(KsefClient.class);

    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_CREDENTIALS_NULL = "credentials must not be null";
    private static final String ERR_FORM_CODE_NULL = "formCode must not be null";
    private static final String ERR_BATCH_FILE_SPEC_NULL = "batchFileSpec must not be null";
    private static final String ERR_INVOICES_NULL = "invoices must not be null";
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
    private final PeppolClient peppolClient;

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
        this.invoiceClient = new InvoiceClientImpl(this);
        this.tokenClient = new TokenClientImpl(this);
        this.permissionClient = new PermissionClientImpl(this);
        this.certificateClient = new CertificateClientImpl(this);
        this.limitsClient = new LimitsClientImpl(this);
        this.rateLimitClient = new RateLimitClientImpl(this);
        this.testDataClient = new TestDataClientImpl(this);
        this.peppolClient = new PeppolClientImpl(this);
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
        LOG.info("Authenticated with KSeF as {}", credentials.identifier());
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
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefSession openSession(FormCode formCode) {
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
        LOG.debug("Opened KSeF session {}, formCode={}", session.referenceNumber(), formCode);

        return new KsefSession(sessionClient, session.referenceNumber(), aesKey, initVector);
    }

    /**
     * Open a batch KSeF session for bulk invoice submission via ZIP package.
     *
     * <p>Authenticates lazily if not already authenticated. Generates AES encryption key,
     * encrypts it with the KSeF public key, and opens the batch session. The returned
     * {@link KsefBatchSession} contains upload URLs for each ZIP part.
     *
     * <p>Batch session flow:
     * <ol>
     *   <li>Open session with this method</li>
     *   <li>Upload encrypted ZIP parts to the URLs from
     *       {@link KsefBatchSession#partUploadRequests()}</li>
     *   <li>Close the session via {@link KsefBatchSession#close()}</li>
     * </ol>
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA2})
     * @param batchFileSpec metadata describing the encrypted ZIP file and its parts
     * @return an open batch session — use with try-with-resources
     */
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefBatchSession openBatchSession(FormCode formCode,
                                                          BatchFileSpec batchFileSpec) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(batchFileSpec, ERR_BATCH_FILE_SPEC_NULL);
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(batchFileSpec));

        BatchSession session = sessionClient.openBatch(request);
        LOG.debug("Opened KSeF batch session {}, formCode={}", session.referenceNumber(), formCode);

        return new KsefBatchSession(sessionClient, httpClient, session.referenceNumber(),
                session.partUploadRequests(), null);
    }

    /**
     * Open a batch KSeF session for a list of raw invoice XML byte arrays.
     *
     * <p>Convenience overload that fully automates the batch package construction:
     * <ol>
     *   <li>Wrap each invoice as a separate entry in an in-memory ZIP</li>
     *   <li>Encrypt the ZIP with a session AES-256 key</li>
     *   <li>Split the encrypted bytes into upload parts (max 100 MB each)</li>
     *   <li>Compute SHA-256 hashes for the whole file and each part</li>
     *   <li>Open the batch session</li>
     * </ol>
     *
     * <p>Authenticates lazily if not already authenticated. After this call, invoke
     * {@link KsefBatchSession#uploadParts()} to push the encrypted bytes, then
     * {@link KsefBatchSession#close()} to finalise the session.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA2})
     * @param invoiceXmls non-empty list of raw invoice XML byte arrays
     * @return an open batch session pre-loaded with the encrypted part bytes
     */
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefBatchSession openBatchSession(FormCode formCode,
                                                          List<byte[]> invoiceXmls) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(invoiceXmls, ERR_INVOICES_NULL);
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoiceXmls, aesKey, initVector);

        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(pkg.spec()));

        BatchSession session = sessionClient.openBatch(request);
        LOG.debug("Opened KSeF batch session {} with {} invoices, formCode={}",
                session.referenceNumber(), invoiceXmls.size(), formCode);

        return new KsefBatchSession(sessionClient, httpClient, session.referenceNumber(),
                session.partUploadRequests(), pkg);
    }

    /**
     * Terminate the current authentication session.
     * Clears all session state. After calling this, {@link #authenticate()} must be
     * called again (explicitly or lazily) before any further operations.
     */
    public synchronized void terminateAuth() {
        ensureOpen();
        authClient.terminateCurrentSession();
        authenticated = false;
        publicKeyCache.clear();
        LOG.info("Terminated KSeF auth session");
    }

    /**
     * Force re-authentication: clear the current JWT and obtain a fresh access token.
     *
     * <p>Used by the SDK internally when an authenticated request returns HTTP 401
     * (token expired). Resets the authenticated flag and the public key cache (the
     * KSeF certificate may have rotated), clears the session JWT, and re-runs the
     * full auth flow.
     *
     * <p>Thread-safe: serialized on this {@link KsefClient} so concurrent 401s only
     * trigger one re-auth.
     */
    public synchronized void reauthenticate() {
        ensureOpen();
        authenticated = false;
        publicKeyCache.clear();
        sessionContext.clear();
        authenticate();
        LOG.info("Re-authenticated with KSeF after 401 (token refresh)");
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

    /**
     * Access Peppol service provider queries.
     * Requires authentication (lazy auth if needed).
     */
    public PeppolClient peppol() {
        ensureOpen();
        return peppolClient;
    }

    // --- Infrastructure accessors (used by internal clients across packages; ADR-013 HttpRuntime contract) ---

    /** SDK infrastructure — not part of the consumer-facing API. */
    public KsefEnvironment environment() { return environment; }

    @Override
    public String baseUrl() { return environment.baseUrl(); }

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
            authenticateWithCertificate(cert.certificate(), cert.privateKey(), cert.identifier());
        } else if (credentials instanceof KsefPkcs12Credentials pkcs12) {
            authenticateWithPkcs12(pkcs12);
        }
    }

    private void authenticateWithToken(KsefTokenCredentials credentials) {
        PublicKey tokenKey = getPublicKey(PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION);
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithToken(challenge, credentials.ksefToken(),
                credentials.identifier(), tokenKey);
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithCertificate(X509Certificate certificate, PrivateKey privateKey,
                                             KsefIdentifier identifier) {
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithXades(challenge.challenge(), certificate, privateKey, identifier);
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithPkcs12(KsefPkcs12Credentials credentials) {
        KeyStore keyStore = CertificateLoader.loadKeyStore(credentials.keystorePath(), credentials.password());
        String alias = CertificateLoader.getFirstAlias(keyStore);
        PrivateKey privateKey = CertificateLoader.getPrivateKey(keyStore, alias, credentials.password());
        X509Certificate certificate = CertificateLoader.getCertificate(keyStore, alias);
        authenticateWithCertificate(certificate, privateKey, credentials.identifier());
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
        } catch (java.security.cert.CertificateException ex) {
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

    private static BatchFileInfoRaw toBatchFileInfoRaw(BatchFileSpec spec) {
        List<BatchFilePartInfoRaw> parts = spec.parts().stream()
                .map(part -> new BatchFilePartInfoRaw()
                        .ordinalNumber(part.ordinalNumber())
                        .fileSize(part.fileSize())
                        .fileHash(part.fileHash()))
                .toList();
        return new BatchFileInfoRaw()
                .fileSize(spec.fileSize())
                .fileHash(spec.fileHash())
                .fileParts(parts);
    }

    /**
     * Builds the {@link ObjectMapper} used for both request serialization and response
     * deserialization.
     *
     * <p>{@code Include.NON_NULL} is set deliberately — KSeF's .NET backend rejects
     * explicit JSON nulls for typed fields (e.g. an unset {@code isDeceased} surfaced
     * as {@code "isDeceased":null} fails to deserialize as {@code System.Boolean}).
     * Plain Java {@code null} fields are dropped from output; {@code JsonNullable.undefined()}
     * also stays absent, while {@code JsonNullable.of(null)} still serializes as an
     * explicit null where the spec requires it.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JsonNullableModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
