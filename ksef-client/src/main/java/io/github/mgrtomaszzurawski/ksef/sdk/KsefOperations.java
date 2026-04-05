/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.OnlineSessionBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.OnlineSessionBuilder.SessionOpenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.SendInvoiceBuilder;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level convenience operations that combine multiple SDK calls into
 * single methods for common KSeF workflows.
 * <p>
 * This class is the recommended entry point for most consumers.
 * <p>
 * Usage:
 * <pre>{@code
 * KsefClient client = KsefClient.builder()
 *     .environment(KsefEnvironment.TEST)
 *     .build();
 * KsefOperations ops = new KsefOperations(client);
 *
 * // Authenticate with token
 * ops.authenticateWithToken("my-token", "1234567890", ops.tokenEncryptionKey());
 *
 * // Send an invoice (handles session open, encrypt, send, close, poll)
 * SessionStatus result = ops.sendInvoice(invoiceXmlBytes);
 *
 * // Clean up
 * ops.terminateSession();
 * }</pre>
 */
public final class KsefOperations {

    private static final int AUTH_POLL_DELAY_MS = 2000;
    private static final int AUTH_POLL_MAX_ATTEMPTS = 15;
    private static final int STATUS_CODE_OK = 200;
    private static final int SESSION_POLL_DELAY_MS = 3000;
    private static final int SESSION_POLL_MAX_ATTEMPTS = 20;
    private static final String ERR_AUTH_TIMEOUT = "Authentication polling timed out";
    private static final String ERR_SESSION_POLL_TIMEOUT = "Session status polling timed out";

    private final KsefClient client;
    private final Map<PublicKeyCertificateUsage, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    public KsefOperations(KsefClient client) {
        this.client = Objects.requireNonNull(client, "client is required");
    }

    /**
     * Get the KSeF public key for token encryption. The key is cached after
     * the first fetch — KSeF certificates are long-lived.
     *
     * @return the RSA public key for encrypting KSeF tokens
     */
    public PublicKey tokenEncryptionKey() {
        return publicKeyCache.computeIfAbsent(
                PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION, this::fetchPublicKeyByUsage);
    }

    /**
     * Get the KSeF public key for symmetric key encryption (used in sessions and exports).
     * The key is cached after the first fetch.
     *
     * @return the RSA public key for encrypting AES keys
     */
    public PublicKey symmetricKeyEncryptionKey() {
        return publicKeyCache.computeIfAbsent(
                PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION, this::fetchPublicKeyByUsage);
    }

    /**
     * Authenticate with a KSeF token. Handles the full flow:
     * challenge → encrypt token → authenticate → poll status → redeem tokens.
     *
     * @param ksefToken the pre-generated KSeF authorization token
     * @param nipIdentifier the NIP of the authenticating entity
     * @param ksefPublicKey the KSeF public key for token encryption (from {@link #tokenEncryptionKey()})
     * @return the redeemed authentication tokens
     */
    public AuthenticationTokens authenticateWithToken(String ksefToken, String nipIdentifier, PublicKey ksefPublicKey) {
        AuthenticationChallenge challenge = client.auth().requestChallenge();
        AuthenticationInit authInit = client.auth().authenticateWithToken(
                challenge, ksefToken, nipIdentifier, ksefPublicKey);

        pollAuthStatus(authInit.referenceNumber());
        return client.auth().redeemTokens();
    }

    /**
     * Send a single invoice through a complete session lifecycle:
     * open session → encrypt and send → close session → poll until closed.
     * <p>
     * The session is always closed, even if sending fails.
     *
     * @param invoiceXml raw invoice XML bytes
     * @return the final session status after closing
     */
    public SessionStatus sendInvoice(byte[] invoiceXml) {
        PublicKey encKey = symmetricKeyEncryptionKey();
        SessionOpenResult sessionOpen = OnlineSessionBuilder.fa2(encKey).build();

        OnlineSession session = client.sessions().openOnline(sessionOpen.request());
        String sessionRef = session.referenceNumber();

        try {
            client.sessions().sendInvoice(
                    sessionRef,
                    SendInvoiceBuilder.create(invoiceXml, sessionOpen.aesKey(), sessionOpen.initVector()).build());

            client.sessions().closeOnline(sessionRef);
        } catch (Exception ex) {
            try {
                client.sessions().closeOnline(sessionRef);
            } catch (Exception closeEx) {
                // Session close failed — will be cleaned up by TTL
            }
            throw ex;
        }

        return pollSessionStatus(sessionRef);
    }

    /**
     * Terminate the current authentication session.
     */
    public void terminateSession() {
        client.auth().terminateCurrentSession();
    }

    private void pollAuthStatus(String referenceNumber) {
        for (int attempt = 0; attempt < AUTH_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(AUTH_POLL_DELAY_MS);
            AuthenticationStatus status = client.auth().getStatus(referenceNumber);
            if (status.status() != null && status.status().code() == STATUS_CODE_OK) {
                return;
            }
        }
        throw new IllegalStateException(ERR_AUTH_TIMEOUT);
    }

    private SessionStatus pollSessionStatus(String referenceNumber) {
        SessionStatus lastStatus = null;
        for (int attempt = 0; attempt < SESSION_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(SESSION_POLL_DELAY_MS);
            lastStatus = client.sessions().getStatus(referenceNumber);
            if (lastStatus.status() != null && lastStatus.status().code() == STATUS_CODE_OK) {
                return lastStatus;
            }
        }
        if (lastStatus != null) {
            return lastStatus;
        }
        throw new IllegalStateException(ERR_SESSION_POLL_TIMEOUT);
    }

    private PublicKey fetchPublicKeyByUsage(PublicKeyCertificateUsage usage) {
        PublicKeyCertificate cert = client.security().getPublicKeyCertificates().stream()
                .filter(c -> c.usage().contains(usage))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No certificate found with usage: " + usage));
        return extractPublicKey(cert.certificate());
    }

    private static PublicKey extractPublicKey(byte[] certBytes) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate x509 = (X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(certBytes));
            return x509.getPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract public key from certificate", ex);
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while polling", ex);
        }
    }
}
