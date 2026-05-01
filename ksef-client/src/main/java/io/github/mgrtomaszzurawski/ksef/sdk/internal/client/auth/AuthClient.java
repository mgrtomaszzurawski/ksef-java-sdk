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
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.signing.SigningService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF authentication operations.
 * Supports XAdES signature-based and KSeF token-based authentication flows.
 */
public final class AuthClient {

    private static final String PATH_CHALLENGE = "/api/v2/auth/challenge";
    private static final String PATH_XADES_SIGNATURE = "/api/v2/auth/xades-signature";
    private static final String PATH_KSEF_TOKEN = "/api/v2/auth/ksef-token";
    private static final String PATH_TOKEN_REDEEM = "/api/v2/auth/token/redeem";
    private static final String PATH_TOKEN_REFRESH = "/api/v2/auth/token/refresh";
    private static final String PATH_SESSIONS = "/api/v2/auth/sessions";
    private static final String PATH_SESSIONS_CURRENT = "/api/v2/auth/sessions/current";
    private static final String PATH_AUTH_STATUS = "/api/v2/auth/";

    private static final String OP_CHALLENGE = "requestChallenge";
    private static final String OP_AUTH_XADES = "authenticateWithXades";
    private static final String OP_AUTH_TOKEN = "authenticateWithToken";
    private static final String OP_REDEEM = "redeemTokens";
    private static final String OP_REFRESH = "refreshToken";
    private static final String OP_STATUS = "getAuthStatus";
    private static final String OP_LIST_SESSIONS = "listSessions";
    private static final String OP_TERMINATE_CURRENT = "terminateCurrentSession";
    private static final String OP_TERMINATE = "terminateSession";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public AuthClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
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
        String authXml = buildAuthTokenRequestXml(challenge, identifier);
        String signedXml = SigningService.signXml(authXml.getBytes(StandardCharsets.UTF_8), certificate, privateKey);
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
        Instant challengeTimestamp = Instant.ofEpochMilli(challenge.timestampMs());
        byte[] encryptedToken = CryptoService.encryptKsefToken(ksefToken, challengeTimestamp, ksefPublicKey);
        AllowedIpsRaw allowedIps = new AllowedIpsRaw()
                .addIp4AddressesItem(challenge.clientIp());
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

    /**
     * Redeem the operation token for access and refresh tokens.
     * Must be called after authentication (XAdES or token flow).
     * Updates the session context with the access token.
     *
     * @return response with access and refresh tokens
     */
    public AuthenticationTokens redeemTokens() {
        String operationToken = sessionContext.token();
        AuthenticationTokensResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REDEEM, operationToken, AuthenticationTokensResponseRaw.class, OP_REDEEM);
        sessionContext.refreshToken(
                response.getAccessToken().getToken(),
                response.getAccessToken().getValidUntil());
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
        AuthenticationTokenRefreshResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REFRESH, refreshToken, AuthenticationTokenRefreshResponseRaw.class, OP_REFRESH);
        sessionContext.refreshToken(
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
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        AuthenticationOperationStatusResponseRaw raw = http.getAuthenticated(
                PATH_AUTH_STATUS + referenceNumber, token,
                AuthenticationOperationStatusResponseRaw.class, OP_STATUS);
        return AuthenticationStatus.from(raw);
    }

    /**
     * List active authentication sessions.
     *
     * @return list response with session items and continuation token
     */
    public AuthenticationList listSessions() {
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
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SESSIONS + "/" + referenceNumber, token, OP_TERMINATE);
    }

    private void activateSession(AuthenticationInitResponseRaw response) {
        sessionContext.activate(
                response.getAuthenticationToken().getToken(),
                response.getReferenceNumber(),
                response.getAuthenticationToken().getValidUntil());
    }

    private static String buildAuthTokenRequestXml(String challenge, KsefIdentifier identifier) {
        String elementName = xmlElementForType(identifier.type());
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<AuthTokenRequest xmlns=\"http://ksef.mf.gov.pl/auth/token/2.0\">"
                + "<Challenge>" + escapeXml(challenge) + "</Challenge>"
                + "<ContextIdentifier><" + elementName + ">" + escapeXml(identifier.value())
                + "</" + elementName + "></ContextIdentifier>"
                + "<SubjectIdentifierType>certificateSubject</SubjectIdentifierType>"
                + "</AuthTokenRequest>";
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
            case NIP -> "Nip";
            case INTERNAL_ID -> "InternalId";
            case NIP_VAT_UE -> "NipVatUe";
            case PEPPOL_ID -> "PeppolId";
        };
    }

    private static String escapeXml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
