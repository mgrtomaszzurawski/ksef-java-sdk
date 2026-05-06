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
import io.github.mgrtomaszzurawski.ksef.sdk.config.CertificateSubjectIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.PreparedBatchPackage;
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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.IdentifierMasking;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CertificateLoader;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefHttpRuntime;
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
 *     try (KsefSession session = client.openSession(FormCode.FA3)) {
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
    private static final String ERR_CREDENTIALS_NULL = "credentials must not be null";
    private static final String ERR_FORM_CODE_NULL = "formCode must not be null";
    private static final String ERR_BATCH_PACKAGE_NULL = "preparedPackage must not be null";
    private static final String ERR_INVOICES_NULL = "invoices must not be null";
    private static final String ERR_BATCH_OPTIONS_NULL = "options must not be null";
    private static final String ERR_CLOSED = "KsefClient has been closed";
    private static final String ERR_AUTH_TIMEOUT = "Authentication polling timed out";
    private static final String ERR_NO_CERT = "No certificate found with usage: ";
    private static final String ERR_KEY_EXTRACT = "Failed to extract public key from certificate";
    private static final String ERR_INTERRUPTED = "Interrupted while polling";
    private static final String CERT_TYPE_X509 = "X.509";

    private static final String LOG_AUTHENTICATED = "Authenticated with KSeF as {} {}";
    private static final String LOG_OPENED_ONLINE_SESSION = "Opened KSeF session {}, formCode={}";
    private static final String LOG_OPENED_BATCH_SESSION = "Opened KSeF batch session {}, formCode={}";
    private static final String LOG_OPENED_BATCH_SESSION_WITH_INVOICES =
            "Opened KSeF batch session {} with {} invoices, formCode={}";
    private static final String LOG_TERMINATED = "Terminated KSeF auth session";
    private static final String LOG_REAUTHENTICATED = "Re-authenticated with KSeF after 401 (full auth flow)";
    private static final String LOG_REFRESHED = "Refreshed KSeF access token via /auth/token/refresh";
    private static final String LOG_REFRESH_FAILED =
            "Refresh-token endpoint failed ({}); falling back to full re-auth";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
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
    private volatile String lastChallengeClientIp;

    private KsefClient(Builder builder) {
        this.environment = builder.environment;
        this.credentials = builder.credentials;
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
        this.invoiceClient = new InvoiceClientImpl(this.runtime);
        this.tokenClient = new TokenClientImpl(this.runtime);
        this.permissionClient = new PermissionClientImpl(this.runtime);
        this.certificateClient = new CertificateClientImpl(this.runtime);
        this.limitsClient = new LimitsClientImpl(this.runtime);
        this.rateLimitClient = new RateLimitClientImpl(this.runtime);
        this.testDataClient = new TestDataClientImpl(this.runtime);
        this.peppolClient = new PeppolClientImpl(this.runtime);
        // Best-effort recovery — if a previous JVM crashed mid-batch, its
        // encrypted part files are still in /tmp. Delete anything older than
        // the orphan-age cutoff that matches the SDK's exact prefix.
        io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchTempCleanup.purgeOrphans(
                null,
                io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchTempCleanup.DEFAULT_ORPHAN_AGE);
    }

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
        if (LOGGER.isDebugEnabled()) {
            KsefIdentifier identifier = credentials.identifier();
            LOGGER.debug(LOG_AUTHENTICATED, identifier.type(),
                    IdentifierMasking.maskTail(identifier.value()));
        }
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
     * <p><strong>Cooldown after termination.</strong> Per
     * {@code context/RCA/RCA-session-cooldown-consecutive-runs-2026-04-04-2105.md}:
     * after a terminated online session, the server enforces a
     * ~30–60 s cooldown for the same NIP. A new session opened too
     * soon will return a reference number but reject the first
     * {@code send(...)} with HTTP 415. The SDK translates that into
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException}
     * with a {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException#suggestedRetryAfter()}
     * recommendation. Consumers should wait at least
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException#TYPICAL_COOLDOWN}
     * before reopening.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA3})
     * @return an open session — use with try-with-resources
     */
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefSession openSession(FormCode formCode) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        formCode.assertAllowedOn(environment);
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
        LOGGER.debug(LOG_OPENED_ONLINE_SESSION, session.referenceNumber(), formCode);
        guardAgainstCooldown(session.referenceNumber());

        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(
                sessionClient, session.referenceNumber(), aesKey, initVector, session.validUntil());
    }

    /**
     * Codex 2026-05-05 #8b — proactively detect the post-termination
     * cooldown window. KSeF returns a fresh session reference on
     * {@code openOnline}, but the session can immediately enter status
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException#COOLDOWN_STATUS_CODE 415}
     * when reopened too soon after a previous termination for the same
     * NIP. We catch that here and surface it as a typed exception
     * instead of letting the caller hit it on first {@code send(...)}.
     */
    private void guardAgainstCooldown(String referenceNumber) {
        var status = sessionClient.getStatus(referenceNumber);
        if (status.status() != null
                && io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException.isCooldownStatus(
                        status.status().code())) {
            throw new io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException(
                    "openSession returned reference " + referenceNumber
                            + " but immediately reports status 415 — server is in the post-termination"
                            + " cooldown window for this NIP. Wait at least "
                            + io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException
                                    .TYPICAL_COOLDOWN
                            + " before retrying.");
        }
    }

    /**
     * Open a batch KSeF session from a caller-prepared, already-encrypted package.
     *
     * <p>Manual entry point for advanced flows where the consumer streams or
     * externally encrypts the batch parts. The {@link PreparedBatchPackage}
     * <strong>must</strong> contain the AES key and IV that were used to
     * encrypt {@code partBytes} — the SDK uses them verbatim to register the
     * encryption material with KSeF (RSA-wrapped against the KSeF
     * SymmetricKeyEncryption certificate). Mismatched key/IV results in a
     * KSeF-side decryption failure.
     *
     * <p>For the common case where the SDK should build and encrypt the
     * package itself, prefer {@link #openBatchSession(FormCode, List, BatchSessionOptions)}.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA3})
     * @param preparedPackage caller-prepared spec + key/IV + encrypted part bytes
     * @param options batch session options (see {@link BatchSessionOptions})
     * @return an open batch session — use with try-with-resources
     */
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefBatchSession openBatchSession(FormCode formCode,
                                                          PreparedBatchPackage preparedPackage,
                                                          BatchSessionOptions options) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(preparedPackage, ERR_BATCH_PACKAGE_NULL);
        Objects.requireNonNull(options, ERR_BATCH_OPTIONS_NULL);
        formCode.assertAllowedOn(environment);
        // Codex round-9 manual validation A.4.2.3: batch sessions do not
        // exhibit the post-termination 415 cooldown documented for online
        // sessions (see context/RCA/RCA-session-cooldown-consecutive-runs-2026-04-04-2105.md).
        // No guardAgainstCooldown call here.
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = preparedPackage.aesKey();
        byte[] initVector = preparedPackage.initVector();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(preparedPackage.spec()))
                .offlineMode(options.offlineMode());

        BatchSession session = sessionClient.openBatch(request);
        LOGGER.debug(LOG_OPENED_BATCH_SESSION, session.referenceNumber(), formCode);

        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newBatchSession(
                sessionClient, httpClient, session.referenceNumber(),
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
     * @param formCode the invoice form code (e.g. {@link FormCode#FA3})
     * @param invoiceXmls non-empty list of raw invoice XML byte arrays
     * @param options batch session options (see {@link BatchSessionOptions})
     * @return an open batch session pre-loaded with the encrypted part bytes
     */
    @SuppressWarnings("java:S2629") // SLF4J parameterised log args are simple getters; isDebugEnabled() guard would be redundant noise.
    public synchronized KsefBatchSession openBatchSession(FormCode formCode,
                                                          List<byte[]> invoiceXmls,
                                                          BatchSessionOptions options) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(invoiceXmls, ERR_INVOICES_NULL);
        Objects.requireNonNull(options, ERR_BATCH_OPTIONS_NULL);
        formCode.assertAllowedOn(environment);
        // Codex round-9 manual validation A.4.2.3: batch cooldown asymmetry
        // intentional — see openBatchSession(PreparedBatchPackage) overload
        // for the rationale.
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoiceXmls, aesKey, initVector, options.assembly());

        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(pkg.spec()))
                .offlineMode(options.offlineMode());

        BatchSession session = sessionClient.openBatch(request);
        LOGGER.debug(LOG_OPENED_BATCH_SESSION_WITH_INVOICES,
                session.referenceNumber(), invoiceXmls.size(), formCode);

        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newBatchSession(
                sessionClient, httpClient, session.referenceNumber(),
                session.partUploadRequests(), pkg);
    }

    /**
     * File-streaming variant of {@link #openBatchSession(FormCode, List, BatchSessionOptions)}.
     * Each invoice is read straight from disk into the batch ZIP rather than
     * materialised as a {@code byte[]} in heap (Codex round-9
     * manual-validation A.4.2). Use this for large batches — e.g. the spec
     * cap of 10 000 invoices (REQ-SESS-41) — so peak heap stays bounded by
     * the chunk-encryption buffer.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA3})
     * @param invoiceFiles non-empty list of paths to invoice XML files
     * @param options batch session options (see {@link BatchSessionOptions})
     * @return an open batch session pre-loaded with the encrypted part bytes
     */
    @SuppressWarnings("java:S2629")
    public synchronized KsefBatchSession openBatchSessionFromFiles(FormCode formCode,
                                                                     List<java.nio.file.Path> invoiceFiles,
                                                                     BatchSessionOptions options) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(invoiceFiles, ERR_INVOICES_NULL);
        Objects.requireNonNull(options, ERR_BATCH_OPTIONS_NULL);
        formCode.assertAllowedOn(environment);
        // Codex round-9 manual validation A.4.2.3: batch cooldown asymmetry
        // intentional — see openBatchSession(PreparedBatchPackage) overload
        // for the rationale.
        ensureOpen();
        ensureAuthenticated();

        PublicKey encryptionKey = getPublicKey(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, encryptionKey);

        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.buildFromFiles(
                invoiceFiles, aesKey, initVector, options.assembly());

        OpenBatchSessionRequestRaw request = new OpenBatchSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(formCode.systemCode())
                        .schemaVersion(formCode.schemaVersion())
                        .value(formCode.value()))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector))
                .batchFile(toBatchFileInfoRaw(pkg.spec()))
                .offlineMode(options.offlineMode());

        BatchSession session = sessionClient.openBatch(request);
        LOGGER.debug(LOG_OPENED_BATCH_SESSION_WITH_INVOICES,
                session.referenceNumber(), invoiceFiles.size(), formCode);

        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newBatchSession(
                sessionClient, httpClient, session.referenceNumber(),
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
        LOGGER.debug(LOG_TERMINATED);
    }

    /**
     * List active auth sessions for this consumer's KSeF context. Maps the
     * raw {@code GET /auth/sessions} response into the public
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession}
     * records.
     */
    public synchronized java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession> listAuthSessions() {
        ensureOpen();
        ensureAuthenticated();
        return authClient.listSessions().items().stream()
                .map(item -> new io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession(
                        item.referenceNumber(),
                        item.startDate(),
                        item.authenticationMethodInfo() == null
                                ? null : item.authenticationMethodInfo().displayName(),
                        item.status(),
                        Boolean.TRUE.equals(item.tokenRedeemed()),
                        item.lastTokenRefreshDate(),
                        item.refreshTokenValidUntil(),
                        Boolean.TRUE.equals(item.current())))
                .toList();
    }

    /**
     * Terminate a specific auth session by its reference number. Useful
     * for cleaning up orphaned sessions or terminating a session other
     * than the current one. Use {@link #terminateAuth()} for the
     * current session.
     */
    public synchronized void terminateAuthSession(String referenceNumber) {
        ensureOpen();
        ensureAuthenticated();
        authClient.terminateSession(java.util.Objects.requireNonNull(referenceNumber, "referenceNumber must not be null"));
    }

    /**
     * Manually trigger a refresh of the current access token using the
     * stored refresh token (if any). The SDK normally does this
     * automatically on HTTP 401; consumers can call this proactively
     * before issuing a long-running batch to ensure the token won't
     * expire mid-flow.
     *
     * @throws io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException
     *     if no refresh token is available or the refresh endpoint
     *     itself returns a non-success status. In that case the caller
     *     should re-authenticate via {@link #authenticate()}.
     */
    public synchronized void refreshAuthToken() {
        ensureOpen();
        if (!tryRefreshToken()) {
            throw new io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException(
                    "Refresh-token unavailable or refresh endpoint failed; call authenticate() to re-establish session",
                    null, 0, null);
        }
    }

    /**
     * Return the cached KSeF public-key certificates. The SDK fetches
     * these once on first use (for invoice / token encryption) and
     * caches them per JVM. Useful for consumer-side audit or display.
     */
    public synchronized java.util.List<PublicKeyCertificate> publicKeyCertificates() {
        ensureOpen();
        return securityClient.getPublicKeyCertificates();
    }

    /**
     * Return the current bearer access token (the JWT the SDK injects in
     * the {@code Authorization} header) — Tier-3 advanced API per
     * ADR-021. Intended for consumers that need to issue HTTP requests
     * against KSeF outside the SDK's own transport plumbing (for
     * example, probing test/diagnostic endpoints, or feeding tokens
     * into a third-party HTTP framework).
     *
     * <p>Returns {@code null} when the client is not authenticated.
     * Triggers lazy authentication if needed.
     *
     * @apiNote Advanced. Most consumers should not need this — domain
     *     clients ({@code client.invoices()}, {@code client.permissions()},
     *     etc.) handle authentication internally.
     */
    public synchronized String bearerToken() {
        ensureOpen();
        ensureAuthenticated();
        return sessionContext.token();
    }

    /**
     * Force re-authentication: obtain a fresh access token.
     *
     * <p>Used by the SDK internally when an authenticated request returns HTTP 401
     * (token expired). When a refresh token captured from the previous redeem
     * response is still available, the client prefers a single
     * {@code POST /auth/token/refresh} call over the full challenge-response
     * cycle. Any failure on the refresh endpoint falls back to a full
     * re-authentication.
     *
     * <p>Thread-safe: serialized on this {@link KsefClient} so concurrent 401s only
     * trigger one re-auth.
     */
    public synchronized void reauthenticate() {
        ensureOpen();
        if (tryRefreshToken()) {
            return;
        }
        authenticated = false;
        publicKeyCache.clear();
        sessionContext.clear();
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
     * Access invoice query and export operations.
     * Requires authentication (lazy auth if needed).
     */
    public InvoiceClient invoices() {
        ensureOpen();
        return invoiceClient;
    }

    /**
     * Access incremental invoice sync orchestrator. Implements the
     * documented HWM-based pagination algorithm from
     * {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}.
     * Tier 1 workflow API per ADR-021.
     */
    public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient invoiceSync() {
        ensureOpen();
        return new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient(
                invoiceClient, objectMapper);
    }

    /**
     * Stream sessions (online + batch) matching the filter, walking
     * the {@code x-continuation-token} cursor returned by KSeF
     * {@code GET /sessions} lazily. Caller controls memory pressure
     * by limiting / collecting downstream.
     *
     * @param filter required filter (type, status, date ranges, exact ref)
     * @return lazy stream of matching session summary items
     */
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem>
            streamSessions(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        ensureOpen();
        ensureAuthenticated();
        return sessionClient.streamSessions(filter);
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
     * The {@code clientIp} value reported by KSeF in the most recent
     * {@code /auth/challenge} response, or {@code null} if no challenge
     * has been requested yet on this client.
     *
     * <p>Use to autopin {@link io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy}
     * for token authentication: read this after the first {@link #authenticate()}
     * call, then build a fresh policy that whitelists exactly this IP and
     * pass it via {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.KsefTokenCredentials}
     * on subsequent authentications.
     *
     * <p>Provided per Codex round-9 manual validation AUTH-15 — the
     * {@code requestChallenge()} response is internal-only by design,
     * but the {@code clientIp} field it carries is operationally useful.
     *
     * @return the client IP from the latest challenge, or {@code null}
     *
     * @since 1.0.0
     */
    public String lastChallengeClientIp() {
        return lastChallengeClientIp;
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

    /** Configured KSeF environment for this client. */
    public KsefEnvironment environment() { return environment; }


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
    }

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
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private RetryPolicy retryPolicy = RetryPolicy.builder().build();
        private FeaturePolicy featurePolicy = FeaturePolicy.defaults();

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

        public KsefClient build() {
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
                    cert.subjectIdentifier(), cert.signingOptions());
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
                                             io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions signingOptions) {
        AuthenticationChallenge challenge = authClient.requestChallenge();
        this.lastChallengeClientIp = challenge.clientIp();
        authClient.authenticateWithXades(challenge.challenge(), certificate, privateKey, identifier,
                subjectIdentifier, signingOptions);
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
                io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions.defaults());
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
        return extractPublicKey(certificate.certificate());
    }

    private static PublicKey extractPublicKey(byte[] certBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(certBytes));
            return x509.getPublicKey();
        } catch (java.security.cert.CertificateException cause) {
            throw new IllegalStateException(ERR_KEY_EXTRACT, cause);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(AUTH_POLL_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, interrupted);
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
