/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationContextIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AllowedIpsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationListResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthorizationPolicyRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenRefreshResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InitTokenAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.signing.SigningService;

import static io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport.requireSafePathSegment;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;

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
     * encrypted and submitted via {@link #authenticateWithToken(AuthenticationChallengeResponseRaw, String, String, PublicKey)}.
     *
     * @return challenge response containing the challenge string and timestamp
     */
    public AuthenticationChallengeResponseRaw requestChallenge() {
        return http.postNoBody(PATH_CHALLENGE, AuthenticationChallengeResponseRaw.class, OP_CHALLENGE);
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
    public AuthenticationInitResponseRaw authenticateWithXades(
            String challenge, X509Certificate certificate, PrivateKey privateKey, String nipIdentifier) {
        String authXml = buildAuthTokenRequestXml(challenge, nipIdentifier);
        String signedXml = SigningService.signXml(authXml.getBytes(StandardCharsets.UTF_8), certificate, privateKey);
        AuthenticationInitResponseRaw response = http.postXml(
                PATH_XADES_SIGNATURE, signedXml, AuthenticationInitResponseRaw.class, OP_AUTH_XADES);
        activateSession(response);
        return response;
    }

    /**
     * Authenticate using KSeF token flow.
     * Encrypts the token with the KSeF public key and submits it with the challenge.
     *
     * @param challengeResponse the full challenge response from {@link #requestChallenge()}
     * @param ksefToken the pre-generated KSeF authorization token
     * @param nipIdentifier the NIP of the authenticating entity
     * @param ksefPublicKey the KSeF public key for encrypting the token
     * @return authentication response with reference number and operation token
     */
    public AuthenticationInitResponseRaw authenticateWithToken(
            AuthenticationChallengeResponseRaw challengeResponse,
            String ksefToken, String nipIdentifier, PublicKey ksefPublicKey) {
        Instant challengeTimestamp = Instant.ofEpochMilli(challengeResponse.getTimestampMs());
        byte[] encryptedToken = CryptoService.encryptKsefToken(ksefToken, challengeTimestamp, ksefPublicKey);
        AllowedIpsRaw allowedIps = new AllowedIpsRaw()
                .addIp4AddressesItem(challengeResponse.getClientIp());
        InitTokenAuthenticationRequestRaw request = new InitTokenAuthenticationRequestRaw()
                .challenge(challengeResponse.getChallenge())
                .contextIdentifier(new AuthenticationContextIdentifierRaw()
                        .type(AuthenticationContextIdentifierTypeRaw.NIP)
                        .value(nipIdentifier))
                .encryptedToken(encryptedToken)
                .authorizationPolicy(new AuthorizationPolicyRaw().allowedIps(allowedIps));
        AuthenticationInitResponseRaw response = http.postJson(
                PATH_KSEF_TOKEN, request, AuthenticationInitResponseRaw.class, OP_AUTH_TOKEN);
        activateSession(response);
        return response;
    }

    /**
     * Redeem the operation token for access and refresh tokens.
     * Must be called after authentication (XAdES or token flow).
     * Updates the session context with the access token.
     *
     * @return response with access and refresh tokens
     */
    public AuthenticationTokensResponseRaw redeemTokens() {
        String operationToken = sessionContext.token();
        AuthenticationTokensResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REDEEM, operationToken, AuthenticationTokensResponseRaw.class, OP_REDEEM);
        sessionContext.refreshToken(
                response.getAccessToken().getToken(),
                response.getAccessToken().getValidUntil());
        return response;
    }

    /**
     * Refresh the access token using the refresh token.
     * Updates the session context with the new access token.
     *
     * @param refreshToken the refresh token from {@link #redeemTokens()}
     * @return response with the new access token
     */
    public AuthenticationTokenRefreshResponseRaw refreshToken(String refreshToken) {
        AuthenticationTokenRefreshResponseRaw response = http.postAuthenticated(
                PATH_TOKEN_REFRESH, refreshToken, AuthenticationTokenRefreshResponseRaw.class, OP_REFRESH);
        sessionContext.refreshToken(
                response.getAccessToken().getToken(),
                response.getAccessToken().getValidUntil());
        return response;
    }

    /**
     * Get the status of an authentication operation.
     *
     * @param referenceNumber the reference number from authentication
     * @return status response with authentication state details
     */
    public AuthenticationOperationStatusResponseRaw getStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        return http.getAuthenticated(
                PATH_AUTH_STATUS + referenceNumber, token,
                AuthenticationOperationStatusResponseRaw.class, OP_STATUS);
    }

    /**
     * List active authentication sessions.
     *
     * @return list response with session items and continuation token
     */
    public AuthenticationListResponseRaw listSessions() {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_SESSIONS, token,
                AuthenticationListResponseRaw.class, OP_LIST_SESSIONS);
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

    private static String buildAuthTokenRequestXml(String challenge, String nipIdentifier) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<AuthTokenRequest xmlns=\"http://ksef.mf.gov.pl/auth/token/2.0\">"
                + "<Challenge>" + escapeXml(challenge) + "</Challenge>"
                + "<ContextIdentifier><Nip>" + escapeXml(nipIdentifier) + "</Nip></ContextIdentifier>"
                + "<SubjectIdentifierType>certificateSubject</SubjectIdentifierType>"
                + "</AuthTokenRequest>";
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
