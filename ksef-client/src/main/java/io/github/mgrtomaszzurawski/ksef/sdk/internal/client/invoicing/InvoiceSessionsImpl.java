/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceSessions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.OnlineSessionOpenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.security.PublicKey;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Online-session lifecycle implementation of {@link InvoiceSessions}.
 *
 * @since 1.0.0
 */
public final class InvoiceSessionsImpl implements InvoiceSessions {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceSessionsImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_OPENED_ONLINE_SESSION = "Opened KSeF session {}, formCode={}";

    private static final String OP_OPEN_SESSION = "openSession";
    private static final String OP_STREAM_SESSIONS = "streamSessions";

    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_NULL_FILTER = "filter must not be null";

    private final SessionClient sessionClient;
    private final KsefEnvironment environment;
    private final Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver;
    private final java.time.Duration invoiceVerificationTimeout;

    public InvoiceSessionsImpl(SessionClient sessionClient,
                               KsefEnvironment environment,
                               Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver,
                               java.time.Duration invoiceVerificationTimeout) {
        this.sessionClient = Objects.requireNonNull(sessionClient, "sessionClient must not be null");
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
        this.publicKeyResolver = Objects.requireNonNull(publicKeyResolver, "publicKeyResolver must not be null");
        this.invoiceVerificationTimeout = Objects.requireNonNull(invoiceVerificationTimeout,
                "invoiceVerificationTimeout must not be null");
    }

    @Override
    @SuppressWarnings("java:S2629")
    public OnlineSession open(FormCode formCode) {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        LOGGER.debug(LOG_CALL, OP_OPEN_SESSION);
        formCode.assertAllowedOn(environment);

        PublicKey encryptionKey = publicKeyResolver.apply(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION);
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

        OnlineSessionOpenResult openResult = sessionClient.openOnline(request);
        LOGGER.debug(LOG_OPENED_ONLINE_SESSION, openResult.referenceNumber(), formCode);
        guardAgainstCooldown(openResult.referenceNumber());

        return SessionHandleConstructor.newOnlineSession(
                sessionClient, openResult.referenceNumber(), aesKey, initVector,
                openResult.validUntil(), environment, invoiceVerificationTimeout);
    }

    @Override
    public Stream<SessionListItem> stream(SessionsQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_STREAM_SESSIONS);
        return sessionClient.streamSessions(filter);
    }

    private void guardAgainstCooldown(String referenceNumber) {
        var status = sessionClient.getStatus(referenceNumber);
        if (status.status() != null
                && KsefSessionCooldownException.isCooldownStatus(status.status().code())) {
            throw new KsefSessionCooldownException(
                    "openSession returned reference " + referenceNumber
                            + " but immediately reports status 415 — server is in the post-termination"
                            + " cooldown window for this NIP. Wait at least "
                            + KsefSessionCooldownException.TYPICAL_COOLDOWN
                            + " before retrying.");
        }
    }
}
