/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.CertificateSubjectIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.AuthSessions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.Certificates;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.Limits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.PeppolProviders;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.Permissions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataAdmin;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.Tokens;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthSessionsImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.CertificatesImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.InvoicesImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.limits.LimitsImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol.PeppolProvidersImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.PermissionsImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.TestDataAdminImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.TokensImpl;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.IdentifierMasking;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CertificateLoader;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefHttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.RetryHandler;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the KSeF Java SDK.
 *
 * <p>Provides lazy authentication and access to domain-specific clients
 * for each KSeF API area. Use with try-with-resources.
 *
 * <p>Example:
 * <pre>{@code
 * KsefCredentials credentials = new KsefTokenCredentials("my-token", "1234567890");
 *
 * try (KsefClient client = KsefClient.builder()
 *         .environment(KsefEnvironment.TEST)
 *         .credentials(credentials)
 *         .build()) {
 *
 *     try (OnlineSession session = client.invoices().sessions().open(FormCode.FA3)) {
 *         session.send(invoiceXmlBytes);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class KsefClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KsefClient.class);

    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_TEST_DATA_ON_PROD =
            "Test-data API is not available on KsefEnvironment.PROD — "
                    + "use TEST or DEMO for test-tenant operations.";
    private static final String ERR_CREDENTIALS_NULL = "credentials must not be null";
    private static final String ERR_CLOSED = "KsefClient has been closed";
    private static final String ERR_AUTH_TIMEOUT = "Authentication polling timed out";
    private static final String ERR_NO_CERT = "No certificate found with usage: ";
    private static final String ERR_INTERRUPTED = "Interrupted while polling";

    private static final String LOG_AUTHENTICATED = "Authenticated with KSeF as {} {}";
    private static final String LOG_TERMINATED = "Terminated KSeF auth session";
    private static final String LOG_REAUTHENTICATED = "Re-authenticated with KSeF after 401 (full auth flow)";
    private static final String LOG_REFRESHED = "Refreshed KSeF access token via /auth/token/refresh";
    private static final String LOG_REFRESH_FAILED =
            "Refresh-token endpoint failed ({}); falling back to full re-auth";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    /** Default deadline for the synchronous invoice verification poll in {@code OnlineSession.sendInvoice}. */
    private static final Duration DEFAULT_INVOICE_VERIFICATION_TIMEOUT = Duration.ofSeconds(60);
    private static final int AUTH_POLL_DELAY_MS = 2000;
    private static final int AUTH_POLL_MAX_ATTEMPTS = 15;
    private static final int STATUS_CODE_OK = 200;
    private static final int AUTH_STATUS_CODE_PROCESSING = 100;
    private static final int AUTH_STATUS_CODE_VERIFYING_CERT = 450;
    private static final String ERR_AUTH_FAILED_PREFIX = "KSeF authentication failed (code=";
    private static final String ERR_AUTH_FAILED_SEPARATOR = ", description=";
    private static final String ERR_AUTH_FAILED_SUFFIX = ")";

    private final KsefEnvironment environment;
    private final KsefCredentials credentials;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryHandler retryHandler;
    private final SessionContext sessionContext;
    private final Duration readTimeout;
    private final HttpRuntime runtime;

    private final AuthClient authClient;
    private final SecurityClient securityClient;
    private final SessionClient sessionClient;
    private final Invoices invoiceClient;
    private final Tokens tokenClient;
    private final Permissions permissionClient;
    private final Certificates certificateClient;
    private final Limits limitsClient;
    private final TestDataAdmin testDataClient;
    private final PeppolProviders peppolClient;
    private final AuthSessions authImpl;
    private final QrCodes qrCodeService;

    private final Map<PublicKeyCertificateUsage, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private volatile boolean authenticated;
    private volatile boolean closed;
    private volatile @Nullable String lastChallengeClientIp;

    private KsefClient(Builder builder) {
        // Builder.build() validates environment + credentials non-null before
        // invoking this constructor, so requireNonNull here is a NullAway
        // hint rather than a guard against a real null.
        this.environment = Objects.requireNonNull(builder.environment, ERR_ENVIRONMENT_NULL);
        this.credentials = Objects.requireNonNull(builder.credentials, ERR_CREDENTIALS_NULL);
        this.readTimeout = builder.readTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .build();
        this.objectMapper = createObjectMapper();
        this.retryHandler = new RetryHandler(builder.retryPolicy);
        this.sessionContext = new SessionContext();
        this.runtime = new KsefHttpRuntime(
                new KsefHttpRuntime.Transport(environment, httpClient, objectMapper, retryHandler, readTimeout),
                new KsefHttpRuntime.AuthHooks(sessionContext, this::reauthenticate, this::ensureAuthenticated,
                        this::isCertificateBackedCredentials),
                builder.featurePolicy);
        this.authClient = new AuthClient(this.runtime);
        this.securityClient = new SecurityClient(this.runtime);
        this.sessionClient = new SessionClient(this.runtime);
        this.invoiceClient = new InvoicesImpl(this.runtime,
                this.sessionClient, this.environment, this::getPublicKey,
                builder.invoiceVerificationTimeout);
        this.tokenClient = new TokensImpl(this.runtime);
        this.permissionClient = new PermissionsImpl(this.runtime);
        this.certificateClient = new CertificatesImpl(this.runtime);
        this.limitsClient = new LimitsImpl(this.runtime);
        this.testDataClient = new TestDataAdminImpl(this.runtime);
        this.peppolClient = new PeppolProvidersImpl(this.runtime);
        this.qrCodeService = new QrCodeService();
        this.authImpl = new AuthSessionsImpl(
                this.authClient,
                this::ensureOpen,
                this::ensureAuthenticated,
                this::terminateAuthInternal,
                () -> Optional.ofNullable(this.lastChallengeClientIp));
        // Best-effort recovery — if a previous JVM crashed mid-batch, its
        // encrypted part files are still in /tmp. Delete anything older than
        // the orphan-age cutoff that matches the SDK's exact prefix. Runs
        // async on a single shared daemon thread (throttled to once per hour)
        // so neither a slow Files.list on a large /tmp nor pathological
        // KsefClient pooling produces thread/IO pressure.
        io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchTempCleanup
                .scheduleAutoCleanup();
    }

    /**
     * Authenticate with KSeF using the configured credentials. Optional —
     * the SDK also triggers authentication implicitly on the first call
     * that requires it (lazy auth). Long-running consumers (batch jobs,
     * scheduled workers, app warm-up) call this once at startup to pay
     * the challenge-response cost outside the critical path.
     *
     * <p>Handles the full authentication flow:
     * <ol>
     *   <li>Request challenge from KSeF</li>
     *   <li>Encrypt token / sign with XAdES (depending on credential type)</li>
     *   <li>Poll authentication status until ready</li>
     *   <li>Redeem operation token for access + refresh tokens</li>
     * </ol>
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
        if (LOGGER.isDebugEnabled()) {
            KsefIdentifier identifier = credentials.identifier();
            LOGGER.debug(LOG_AUTHENTICATED, identifier.type(),
                    IdentifierMasking.maskTail(identifier.value()));
        }
    }

    /**
     * Internal: terminate the current auth session — invoked by
     * {@link AuthSessions#terminate()} via the lifecycle hook supplied to
     * {@link AuthSessionsImpl}. Clears local auth state on success.
     */
    private synchronized void terminateAuthInternal() {
        ensureOpen();
        authenticated = false;
        publicKeyCache.clear();
        lastChallengeClientIp = null;
        LOGGER.debug(LOG_TERMINATED);
    }

    /**
     * Internal: force re-authentication on HTTP 401 (driven by
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime}).
     * Prefers refresh-token over full challenge-response cycle.
     */
    private synchronized void reauthenticate() {
        ensureOpen();
        if (tryRefreshToken()) {
            return;
        }
        authenticated = false;
        publicKeyCache.clear();
        sessionContext.clear();
        lastChallengeClientIp = null;
        authenticate();
        LOGGER.debug(LOG_REAUTHENTICATED);
    }

    private boolean tryRefreshToken() {
        String refreshToken = sessionContext.refreshToken();
        if (refreshToken == null) {
            return false;
        }
        try {
            authClient.refreshToken(refreshToken);
            authenticated = true;
            LOGGER.debug(LOG_REFRESHED);
            return true;
        } catch (RuntimeException refreshFailure) {
            LOGGER.debug(LOG_REFRESH_FAILED, refreshFailure.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Access invoice query, export, online-session, sync, and
     * session-stream operations.
     *
     * <p>Requires authentication (lazy auth if needed).
     */
    public Invoices invoices() {
        ensureOpen();
        return invoiceClient;
    }

    /**
     * Access auth-session management — terminate, list, terminate by
     * reference, and the diagnostic last-challenge client-IP hook.
     *
     * <p>Authentication itself is lazy and handled internally; this
     * accessor exposes the explicit session-management verbs.
     */
    public AuthSessions auth() {
        ensureOpen();
        return authImpl;
    }

    /**
     * Access token management operations (generate, list, get status, revoke).
     */
    public Tokens tokens() {
        ensureOpen();
        return tokenClient;
    }

    /**
     * Access permission management operations (grant, revoke, query permissions).
     */
    public Permissions permissions() {
        ensureOpen();
        return permissionClient;
    }

    /**
     * Access certificate management operations (enroll, retrieve, revoke, query).
     */
    public Certificates certificates() {
        ensureOpen();
        return certificateClient;
    }

    /**
     * Access session, subject, and rate-limit queries.
     */
    public Limits limits() {
        ensureOpen();
        return limitsClient;
    }

    /**
     * Access test environment data management operations.
     *
     * @throws IllegalStateException when the client is configured for
     *     {@link KsefEnvironment#PROD} — the test-data API is not exposed
     *     in production. The check is environment-equality based, so
     *     {@code custom(productionUrl)} bypasses it on purpose; the rule
     *     here exists to stop accidental PROD calls from code paths that
     *     wire {@link KsefEnvironment#PROD} explicitly.
     */
    public TestDataAdmin testData() {
        ensureOpen();
        if (KsefEnvironment.PROD.equals(environment)) {
            throw new IllegalStateException(ERR_TEST_DATA_ON_PROD);
        }
        return testDataClient;
    }

    /**
     * Access Peppol service provider queries.
     * Requires authentication (lazy auth if needed).
     */
    public PeppolProviders peppol() {
        ensureOpen();
        return peppolClient;
    }

    /**
     * Access QR code rendering — KOD I (verification URL QR) and labelled
     * variants. Stateless instance shared across the client lifecycle;
     * the service does not require authentication.
     *
     * @return the shared {@link QrCodes} default implementation
     */
    public QrCodes qrCode() {
        return qrCodeService;
    }

    /** Configured KSeF environment for this client. */
    public KsefEnvironment environment() { return environment; }

    /**
     * Pre-load the per-schema JAXBContext + XSD validation caches for every
     * KSeF-supported invoice form (FA(2), FA(3), PEF(3), PEF_KOR(3)).
     *
     * <p>Strategy A from the F-7 architecture (see ADR-031) pays the
     * JAXBContext + Xerces XSD DFA construction cost lazily, on first use of
     * a given schema. That keeps the FA(3)-only consumer's resident memory
     * around ~30 MB but pushes a transient construction spike onto the first
     * invoice send. Calling {@code warmup()} at application start-up moves
     * the spike to a controlled window so steady-state requests stay flat.
     *
     * <p>Idempotent — subsequent calls hit the existing caches and return
     * immediately. Optional — every invoicing path warms the cache itself
     * on first use; this is purely for consumers who want predictable
     * first-request latency.
     */
    public void warmup() {
        io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceMarshallingPreheat.preheatAll();
    }


    @Override
    public synchronized void close() {
        closed = true;
        // Lifecycle hygiene (Codex round-9 F6) — clear cached public keys and
        // bearer/refresh tokens so they are eligible for GC immediately rather
        // than only when the KsefClient itself becomes unreachable. The
        // close-flag check above already prevents any further protected calls;
        // this just makes the secret material unreachable sooner.
        publicKeyCache.clear();
        sessionContext.clear();
        authenticated = false;
        lastChallengeClientIp = null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private @Nullable KsefEnvironment environment;
        private @Nullable KsefCredentials credentials;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private RetryPolicy retryPolicy = RetryPolicy.builder().build();
        private FeaturePolicy featurePolicy = FeaturePolicy.defaults();
        private Duration invoiceVerificationTimeout = DEFAULT_INVOICE_VERIFICATION_TIMEOUT;

        private Builder() { }

        /**
         * Set the target KSeF API environment. Required.
         *
         * @param environment one of {@link KsefEnvironment#TEST}, {@link KsefEnvironment#DEMO},
         *                    {@link KsefEnvironment#PROD}, or {@link KsefEnvironment#custom(String)}
         * @return this builder
         */
        public Builder environment(KsefEnvironment environment) {
            this.environment = environment;
            return this;
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

        /**
         * Set the per-request read timeout. Bounds how long the SDK waits for a
         * single KSeF response. Default 30s.
         *
         * @param readTimeout the read timeout
         * @return this builder
         */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Set the KSeF feature policy (UPO version, problem-details opt-in).
         * Defaults to {@link FeaturePolicy#defaults()} which preserves
         * pre-1.0 behavior.
         *
         * @param featurePolicy the policy
         * @return this builder
         */
        public Builder features(FeaturePolicy featurePolicy) {
            this.featurePolicy = Objects.requireNonNull(featurePolicy, "featurePolicy must not be null");
            return this;
        }

        /**
         * Set the deadline for synchronous invoice verification inside
         * {@code OnlineSession.sendInvoice(Invoice)} (PR10). When the
         * SDK posts an invoice it polls the per-invoice status until
         * KSeF reports a terminal state (Accepted / Rejected); this
         * duration bounds how long the SDK waits before throwing
         * {@code KsefSessionPollingTimeoutException}. Default 60s.
         *
         * <p>Tune higher for environments where verification is known
         * to spike; lower for tests that want a fast-fail behaviour.
         *
         * @param invoiceVerificationTimeout the deadline (must not be null)
         * @return this builder
         */
        public Builder invoiceVerificationTimeout(Duration invoiceVerificationTimeout) {
            this.invoiceVerificationTimeout = Objects.requireNonNull(invoiceVerificationTimeout,
                    "invoiceVerificationTimeout must not be null");
            return this;
        }

        public KsefClient build() {
            Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
            Objects.requireNonNull(credentials, ERR_CREDENTIALS_NULL);
            return new KsefClient(this);
        }
    }

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
            authenticateWithCertificate(cert.certificate(), cert.privateKey(), cert.identifier(),
                    cert.subjectIdentifier(), cert.signingOptions(),
                    cert.authorizationPolicy().orElse(null));
        } else if (credentials instanceof KsefPkcs12Credentials pkcs12) {
            authenticateWithPkcs12(pkcs12);
        }
    }

    private void authenticateWithToken(KsefTokenCredentials credentials) {
        PublicKey tokenKey = getPublicKey(PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION);
        AuthenticationChallenge challenge = authClient.requestChallenge();
        this.lastChallengeClientIp = challenge.clientIp();
        authClient.authenticateWithToken(challenge, credentials.ksefToken(),
                credentials.identifier(), tokenKey,
                credentials.authorizationPolicy().orElse(null));
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithCertificate(X509Certificate certificate, PrivateKey privateKey,
                                             KsefIdentifier identifier,
                                             CertificateSubjectIdentifier subjectIdentifier,
                                             io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions signingOptions,
                                             @Nullable AuthorizationPolicy policy) {
        AuthenticationChallenge challenge = authClient.requestChallenge();
        this.lastChallengeClientIp = challenge.clientIp();
        authClient.authenticateWithXades(challenge.challenge(), certificate, privateKey, identifier,
                subjectIdentifier, signingOptions, policy, challenge.clientIp());
        pollAuthStatus();
        authClient.redeemTokens();
    }

    private void authenticateWithPkcs12(KsefPkcs12Credentials credentials) {
        KeyStore keyStore = CertificateLoader.loadKeyStore(credentials.keystorePath(), credentials.password());
        String alias = CertificateLoader.getFirstAlias(keyStore);
        PrivateKey privateKey = CertificateLoader.getPrivateKey(keyStore, alias, credentials.password());
        X509Certificate certificate = CertificateLoader.getCertificate(keyStore, alias);
        authenticateWithCertificate(certificate, privateKey, credentials.identifier(),
                credentials.subjectIdentifier(),
                io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions.defaults(),
                credentials.authorizationPolicy().orElse(null));
    }

    private boolean isCertificateBackedCredentials() {
        return credentials instanceof KsefCertificateCredentials
                || credentials instanceof KsefPkcs12Credentials;
    }

    private void pollAuthStatus() {
        String refNumber = sessionContext.referenceNumber();
        for (int attempt = 0; attempt < AUTH_POLL_MAX_ATTEMPTS; attempt++) {
            sleep();
            AuthenticationStatus status = authClient.getStatus(refNumber);
            if (status.status() == null) {
                continue;
            }
            int code = status.status().code();
            if (code == STATUS_CODE_OK) {
                return;
            }
            if (code == AUTH_STATUS_CODE_PROCESSING || code == AUTH_STATUS_CODE_VERIFYING_CERT) {
                continue;
            }
            // Codex round-9 manual validation A.4.2.2: any other terminal
            // status code (e.g. 400 — invalid signature, revoked cert, no
            // permissions for context) is reported with the actual reason
            // instead of being swallowed as a generic poll timeout.
            throw new io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException(
                    ERR_AUTH_FAILED_PREFIX + code + ERR_AUTH_FAILED_SEPARATOR
                            + status.status().description() + ERR_AUTH_FAILED_SUFFIX,
                    null, code, null);
        }
        throw new IllegalStateException(ERR_AUTH_TIMEOUT);
    }

    private PublicKey getPublicKey(PublicKeyCertificateUsage usage) {
        return publicKeyCache.computeIfAbsent(usage, this::fetchPublicKey);
    }

    private PublicKey fetchPublicKey(PublicKeyCertificateUsage usage) {
        PublicKeyCertificate certificate = securityClient.getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().contains(usage))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_CERT + usage));
        return certificate.publicKey();
    }

    private static void sleep() {
        try {
            Thread.sleep(AUTH_POLL_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, interrupted);
        }
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
