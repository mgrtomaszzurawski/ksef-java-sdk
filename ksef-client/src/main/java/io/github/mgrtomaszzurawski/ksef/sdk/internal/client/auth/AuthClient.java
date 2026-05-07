/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import io.github.mgrtomaszzurawski.ksef.client.model.AllowedIpsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationListResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenRefreshResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthorizationPolicyRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InitTokenAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.CertificateSubjectIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.signing.SigningService;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF authentication operations.
 * Supports XAdES signature-based and KSeF token-based authentication flows.
 *
 * @since 1.0.0
 */
public final class AuthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthClient.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_CHALLENGE = ApiPaths.AUTH + "/challenge";
    private static final String PATH_XADES_SIGNATURE = ApiPaths.AUTH + "/xades-signature";
    private static final String PATH_KSEF_TOKEN = ApiPaths.AUTH + "/ksef-token";
    private static final String PATH_TOKEN_REDEEM = ApiPaths.AUTH + "/token/redeem";
    private static final String PATH_TOKEN_REFRESH = ApiPaths.AUTH + "/token/refresh";
    private static final String PATH_SESSIONS = ApiPaths.AUTH + "/sessions";
    private static final String PATH_SESSIONS_CURRENT = ApiPaths.AUTH + "/sessions/current";

    private static final String OP_CHALLENGE = "requestChallenge";
    private static final String OP_AUTH_XADES = "authenticateWithXades";
    private static final String OP_AUTH_TOKEN = "authenticateWithToken";
    private static final String OP_REDEEM = "redeemTokens";
    private static final String OP_REFRESH = "refreshToken";
    private static final String OP_STATUS = "getAuthStatus";
    private static final String OP_LIST_SESSIONS = "listSessions";
    private static final String OP_TERMINATE_CURRENT = "terminateCurrentSession";
    private static final String OP_TERMINATE = "terminateSession";

    private static final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String AUTH_TOKEN_REQUEST_OPEN =
            "<AuthTokenRequest xmlns=\"http://ksef.mf.gov.pl/auth/token/2.0\">";
    private static final String AUTH_TOKEN_REQUEST_CLOSE = "</AuthTokenRequest>";
    private static final String CHALLENGE_OPEN = "<Challenge>";
    private static final String CHALLENGE_CLOSE = "</Challenge>";
    private static final String CONTEXT_IDENTIFIER_OPEN = "<ContextIdentifier><";
    private static final String CONTEXT_IDENTIFIER_INNER_CLOSE = "></ContextIdentifier>";
    private static final String SUBJECT_IDENTIFIER_TYPE_OPEN = "<SubjectIdentifierType>";
    private static final String SUBJECT_IDENTIFIER_TYPE_CLOSE = "</SubjectIdentifierType>";
    private static final String XML_CLOSE_BRACKET = ">";
    private static final String XML_END_TAG_PREFIX = "</";

    private static final String XML_ELEMENT_NIP = "Nip";
    private static final String XML_ELEMENT_INTERNAL_ID = "InternalId";
    private static final String XML_ELEMENT_NIP_VAT_UE = "NipVatUe";
    private static final String XML_ELEMENT_PEPPOL_ID = "PeppolId";

    private static final String XML_ESCAPE_AMP_FROM = "&";
    private static final String XML_ESCAPE_AMP_TO = "&amp;";
    private static final String XML_ESCAPE_LT_FROM = "<";
    private static final String XML_ESCAPE_LT_TO = "&lt;";
    private static final String XML_ESCAPE_GT_FROM = ">";
    private static final String XML_ESCAPE_GT_TO = "&gt;";
    private static final String XML_ESCAPE_QUOT_FROM = "\"";
    private static final String XML_ESCAPE_QUOT_TO = "&quot;";
    private static final String XML_ESCAPE_APOS_FROM = "'";
    private static final String XML_ESCAPE_APOS_TO = "&apos;";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public AuthClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.sessionContext = runtime.sessionContext();
    }

    /**
     * Request a challenge for authentication.
     * The challenge must be signed with XAdES and submitted via
     * {@link #authenticateWithXades(String, X509Certificate, PrivateKey, String)} or
     * encrypted and submitted via {@link #authenticateWithToken(AuthenticationChallenge, String, String, PublicKey)}.
     *
     * @return challenge response containing the challenge string and timestamp
     */
    public AuthenticationChallenge requestChallenge() {
        LOGGER.debug(LOG_CALL, OP_CHALLENGE);
        AuthenticationChallengeResponseRaw raw = http.postNoBody(PATH_CHALLENGE, AuthenticationChallengeResponseRaw.class, OP_CHALLENGE);
        return AuthenticationChallenge.from(raw);
    }

    /**
     * Authenticate using XAdES signature flow.
     * Signs the challenge XML with the provided certificate and private key,
     * submits the signed XML, and activates the session.
     *
     * @param challenge the challenge string from {@link #requestChallenge()}
     * @param certificate the signing X.509 certificate
     * @param privateKey the private key matching the certificate
     * @param nipIdentifier the NIP (Polish tax ID) of the authenticating entity
     * @return authentication response with reference number and operation token
     */
    public AuthenticationInit authenticateWithXades(
            String challenge, X509Certificate certificate, PrivateKey privateKey, String nipIdentifier) {
        return authenticateWithXades(challenge, certificate, privateKey,
                KsefIdentifier.nip(nipIdentifier));
    }

    /**
     * Authenticate using XAdES signature flow with a generic identifier.
     *
     * @param challenge the challenge string from {@link #requestChallenge()}
     * @param certificate the signing X.509 certificate
     * @param privateKey the private key matching the certificate
     * @param identifier authentication context identifier (any of the four KSeF types)
     * @return authentication response with reference number and operation token
     */
    public AuthenticationInit authenticateWithXades(
            String challenge, X509Certificate certificate, PrivateKey privateKey,
            KsefIdentifier identifier) {
        return authenticateWithXades(challenge, certificate, privateKey, identifier,
                CertificateSubjectIdentifier.subject());
    }

    /**
     * Authenticate using XAdES signature flow with a generic identifier and
     * an explicit {@link CertificateSubjectIdentifier} strategy. Closes
     * REQ-AUTH-033 (certificate fingerprint variant of
     * {@code SubjectIdentifierType}).
     */
    public AuthenticationInit authenticateWithXades(
            String challenge, X509Certificate certificate, PrivateKey privateKey,
            KsefIdentifier identifier, CertificateSubjectIdentifier subjectIdentifier) {
        return authenticateWithXades(challenge, certificate, privateKey, identifier, subjectIdentifier,
                io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions.defaults());
    }

    /**
     * Authenticate with explicit {@link io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions}.
     * Closes REQ-AUTH-039/040 to the extent of the documented narrow scope
     * (BASELINE-B + SHA-256). Other combinations throw.
     */
    public AuthenticationInit authenticateWithXades(
            String challenge, X509Certificate certificate, PrivateKey privateKey,
            KsefIdentifier identifier, CertificateSubjectIdentifier subjectIdentifier,
            io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions signingOptions) {
        LOGGER.debug(LOG_CALL, OP_AUTH_XADES);
        String authXml = buildAuthTokenRequestXml(challenge, identifier, subjectIdentifier);
        String signedXml = SigningService.signXml(
                authXml.getBytes(StandardCharsets.UTF_8), certificate, privateKey, signingOptions);
        AuthenticationInitResponseRaw response = http.postXml(
                PATH_XADES_SIGNATURE, signedXml, AuthenticationInitResponseRaw.class, OP_AUTH_XADES);
        activateSession(response);
        return AuthenticationInit.from(response);
    }

    /**
     * Authenticate using KSeF token flow.
     * Encrypts the token with the KSeF public key and submits it with the challenge.
     *
     * @param challenge the challenge response from {@link #requestChallenge()}
     * @param ksefToken the pre-generated KSeF authorization token
     * @param nipIdentifier the NIP of the authenticating entity
     * @param ksefPublicKey the KSeF public key for encrypting the token
     * @return authentication response with reference number and operation token
     */
    public AuthenticationInit authenticateWithToken(
            AuthenticationChallenge challenge,
            String ksefToken, String nipIdentifier, PublicKey ksefPublicKey) {
        return authenticateWithToken(challenge, ksefToken,
                KsefIdentifier.nip(nipIdentifier), ksefPublicKey);
    }

    /**
     * Authenticate using KSeF token flow with a generic identifier.
     *
     * @param challenge the challenge response from {@link #requestChallenge()}
     * @param ksefToken the pre-generated KSeF authorization token
     * @param identifier authentication context identifier (any of the four KSeF types)
     * @param ksefPublicKey the KSeF public key for encrypting the token
     * @return authentication response with reference number and operation token
     */
    public AuthenticationInit authenticateWithToken(
            AuthenticationChallenge challenge,
            String ksefToken, KsefIdentifier identifier, PublicKey ksefPublicKey) {
        return authenticateWithToken(challenge, ksefToken, identifier, ksefPublicKey, null);
    }

    /**
     * Codex 2026-05-05 #7 / F6 — overload accepting a custom
     * {@link AuthorizationPolicy} (IP allow-list with addresses, ranges,
     * and CIDR masks). When {@code policy} is {@code null}, falls back to
     * the legacy single-client-IP behaviour.
     */
    public AuthenticationInit authenticateWithToken(
            AuthenticationChallenge challenge,
            String ksefToken, KsefIdentifier identifier, PublicKey ksefPublicKey,
            @Nullable AuthorizationPolicy policy) {
        LOGGER.debug(LOG_CALL, OP_AUTH_TOKEN);
        Instant challengeTimestamp = Instant.ofEpochMilli(challenge.timestampMs());
        byte[] encryptedToken = CryptoService.encryptKsefToken(ksefToken, challengeTimestamp, ksefPublicKey);
        AllowedIpsRaw allowedIps = toAllowedIpsRaw(policy, challenge.clientIp());
        InitTokenAuthenticationRequestRaw request = new InitTokenAuthenticationRequestRaw()
                .challenge(challenge.challenge())
                .contextIdentifier(new AuthenticationContextIdentifierRaw()
                        .type(toRawType(identifier.type()))
                        .value(identifier.value()))
                .encryptedToken(encryptedToken)
                .authorizationPolicy(new AuthorizationPolicyRaw().allowedIps(allowedIps));
        AuthenticationInitResponseRaw response = http.postJson(
                PATH_KSEF_TOKEN, request, AuthenticationInitResponseRaw.class, OP_AUTH_TOKEN);
        activateSession(response);
        return AuthenticationInit.from(response);
    }

    private static AllowedIpsRaw toAllowedIpsRaw(@Nullable AuthorizationPolicy policy, @Nullable String defaultClientIp) {
        if (policy == null) {
            return new AllowedIpsRaw().addIp4AddressesItem(defaultClientIp);
        }
        AllowedIpsRaw raw = new AllowedIpsRaw();
        for (String addr : policy.ip4Addresses()) {
            raw.addIp4AddressesItem(addr);
        }
        for (String range : policy.ip4Ranges()) {
            raw.addIp4RangesItem(range);
        }
        for (String mask : policy.ip4Masks()) {
            raw.addIp4MasksItem(mask);
        }
        return raw;
    }

    /**
     * Redeem the operation token for access and refresh tokens.
     * Must be called after authentication (XAdES or token flow).
     * Updates the session context with the access token.
     *
     * @return response with access and refresh tokens
     */
    public AuthenticationTokens redeemTokens() {
        LOGGER.debug(LOG_CALL, OP_REDEEM);
        String operationToken = sessionContext.token();
        AuthenticationTokensResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REDEEM, operationToken, AuthenticationTokensResponseRaw.class, OP_REDEEM);
        sessionContext.updateAccessToken(
                response.getAccessToken().getToken(),
                response.getAccessToken().getValidUntil());
        if (response.getRefreshToken() != null && response.getRefreshToken().getToken() != null) {
            sessionContext.storeRefreshToken(response.getRefreshToken().getToken());
        }
        return AuthenticationTokens.from(response);
    }

    /**
     * Refresh the access token using the refresh token.
     * Updates the session context with the new access token.
     *
     * @param refreshToken the refresh token from {@link #redeemTokens()}
     * @return response with the new access token
     */
    public AuthenticationTokenRefresh refreshToken(String refreshToken) {
        LOGGER.debug(LOG_CALL, OP_REFRESH);
        AuthenticationTokenRefreshResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REFRESH, refreshToken, AuthenticationTokenRefreshResponseRaw.class, OP_REFRESH);
        sessionContext.updateAccessToken(
                response.getAccessToken().getToken(),
                response.getAccessToken().getValidUntil());
        return AuthenticationTokenRefresh.from(response);
    }

    /**
     * Get the status of an authentication operation.
     *
     * @param referenceNumber the reference number from authentication
     * @return status response with authentication state details
     */
    public AuthenticationStatus getStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        AuthenticationOperationStatusResponseRaw raw = http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.AUTH, referenceNumber), token,
                AuthenticationOperationStatusResponseRaw.class, OP_STATUS);
        return AuthenticationStatus.from(raw);
    }

    /**
     * List active authentication sessions.
     *
     * @return list response with session items and continuation token
     */
    public AuthenticationList listSessions() {
        LOGGER.debug(LOG_CALL, OP_LIST_SESSIONS);
        String token = sessionContext.token();
        AuthenticationListResponseRaw raw = http.getAuthenticated(PATH_SESSIONS, token,
                AuthenticationListResponseRaw.class, OP_LIST_SESSIONS);
        return AuthenticationList.from(raw);
    }

    /**
     * Terminate the current authentication session.
     * Clears the session context.
     */
    public void terminateCurrentSession() {
        LOGGER.debug(LOG_CALL, OP_TERMINATE_CURRENT);
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SESSIONS_CURRENT, token, OP_TERMINATE_CURRENT);
        sessionContext.clear();
    }

    /**
     * Terminate a specific authentication session by reference number.
     *
     * @param referenceNumber the reference number of the session to terminate
     */
    public void terminateSession(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_TERMINATE, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        http.deleteAuthenticated(ApiPaths.subPath(PATH_SESSIONS, referenceNumber), token, OP_TERMINATE);
    }

    private void activateSession(AuthenticationInitResponseRaw response) {
        sessionContext.activate(
                response.getAuthenticationToken().getToken(),
                response.getReferenceNumber(),
                response.getAuthenticationToken().getValidUntil());
    }

    private static String buildAuthTokenRequestXml(String challenge, KsefIdentifier identifier,
                                                   CertificateSubjectIdentifier subjectIdentifier) {
        String elementName = xmlElementForType(identifier.type());
        return XML_PROLOG
                + AUTH_TOKEN_REQUEST_OPEN
                + CHALLENGE_OPEN + escapeXml(challenge) + CHALLENGE_CLOSE
                + CONTEXT_IDENTIFIER_OPEN + elementName + XML_CLOSE_BRACKET
                + escapeXml(identifier.value())
                + XML_END_TAG_PREFIX + elementName + CONTEXT_IDENTIFIER_INNER_CLOSE
                + SUBJECT_IDENTIFIER_TYPE_OPEN + subjectIdentifier.wireType() + SUBJECT_IDENTIFIER_TYPE_CLOSE
                + AUTH_TOKEN_REQUEST_CLOSE;
    }

    private static AuthenticationContextIdentifierTypeRaw toRawType(KsefIdentifier.Type type) {
        return switch (type) {
            case NIP -> AuthenticationContextIdentifierTypeRaw.NIP;
            case INTERNAL_ID -> AuthenticationContextIdentifierTypeRaw.INTERNAL_ID;
            case NIP_VAT_UE -> AuthenticationContextIdentifierTypeRaw.NIP_VAT_UE;
            case PEPPOL_ID -> AuthenticationContextIdentifierTypeRaw.PEPPOL_ID;
        };
    }

    private static String xmlElementForType(KsefIdentifier.Type type) {
        return switch (type) {
            case NIP -> XML_ELEMENT_NIP;
            case INTERNAL_ID -> XML_ELEMENT_INTERNAL_ID;
            case NIP_VAT_UE -> XML_ELEMENT_NIP_VAT_UE;
            case PEPPOL_ID -> XML_ELEMENT_PEPPOL_ID;
        };
    }

    private static String escapeXml(String input) {
        return input
                .replace(XML_ESCAPE_AMP_FROM, XML_ESCAPE_AMP_TO)
                .replace(XML_ESCAPE_LT_FROM, XML_ESCAPE_LT_TO)
                .replace(XML_ESCAPE_GT_FROM, XML_ESCAPE_GT_TO)
                .replace(XML_ESCAPE_QUOT_FROM, XML_ESCAPE_QUOT_TO)
                .replace(XML_ESCAPE_APOS_FROM, XML_ESCAPE_APOS_TO);
    }
}
