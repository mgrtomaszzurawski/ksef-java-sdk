/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClientInternals;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for authentication. Uses the high-level {@code client.authenticate()} API
 * which handles the full flow internally: challenge → encrypt/sign → poll → redeem.
 *
 * <p>After this runner completes, the KsefClient session context is populated with a valid
 * bearer token that all subsequent runners use automatically.
 *
 * <p>Additional auth-area operations exercised here:
 * <ul>
 *   <li>{@code listSessions} — query active auth sessions</li>
 *   <li>{@code refreshToken} — renew access token via refresh token (manual flow needed
 *       to capture the refresh token, since the SDK auto-redeems it during authenticate)</li>
 *   <li>{@code forceReauthOnExpiredToken} — corrupt JWT and confirm SDK auto-reauth on 401</li>
 *   <li>{@code terminateSessionByRef} — terminate a specific session by reference (vs.
 *       /current endpoint exercised by {@code terminateAuth})</li>
 * </ul>
 */
public final class AuthRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthRunner.class);
    private static final String NAME = "auth";
    private static final String OP_AUTHENTICATE = "authenticate";
    private static final String OP_TERMINATE = "terminateAuth";
    private static final String OP_RE_AUTHENTICATE = "reAuthenticate";
    private static final String OP_LIST_SESSIONS = "listSessions";
    private static final String OP_REFRESH_TOKEN = "refreshToken";
    private static final String OP_FORCE_REAUTH = "forceReauthOnExpiredToken";
    private static final String OP_TERMINATE_BY_REF = "terminateSessionByRef";
    private static final String OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF = "reAuthAfterTerminateByRef";

    private static final String CERT_TYPE_X509 = "X.509";
    private static final String INVALID_JWT = "invalid-jwt";
    private static final String NIP_PREFIX = "NIP=";
    private static final String SESSIONS_LABEL = " sessions";
    private static final String VALID_UNTIL_PREFIX = "validUntil=";
    private static final String REF_PREFIX = "ref=";
    private static final String AUTO_REFRESH_SUCCEEDED = "auto-refresh succeeded";

    private static final int AUTH_POLL_DELAY_MS = 2000;
    private static final int AUTH_POLL_MAX_ATTEMPTS = 15;
    private static final int STATUS_CODE_OK = 200;
    private static final long FUTURE_EXPIRY_HOURS = 1L;

    private static final String ERR_NO_REFRESH_TOKEN = "Manual auth flow did not return a refresh token";
    private static final String ERR_AUTH_TIMEOUT = "Manual auth polling timed out";
    private static final String ERR_NO_PUBLIC_KEY = "No KSeF public key for token encryption";
    private static final String ERR_NO_CURRENT_SESSION = "No current session in listSessions response";
    private static final String ERR_INTERRUPTED = "Interrupted while polling auth status";
    private static final String ERR_CERT_PARSE = "Failed to extract public key from KSeF certificate";

    private static final String LOG_AUTHENTICATED = "[{}] authenticated as NIP {}";
    private static final String LOG_TERMINATED = "[{}] auth session terminated";
    private static final String LOG_RE_AUTHENTICATED = "[{}] re-authenticated after terminate";
    private static final String LOG_ACTIVE_SESSIONS = "[{}] active auth sessions: {}";
    private static final String LOG_REFRESH_OK = "[{}] refreshed access token, validUntil={}";
    private static final String LOG_FORCE_REAUTH_OK = "[{}] auto-reauth recovered: contextLimits retrieved={}";
    private static final String LOG_TERMINATED_BY_REF = "[{}] terminated session by ref={}";
    private static final String LOG_RE_AUTHENTICATED_AFTER_BY_REF = "[{}] re-authenticated after terminateSessionByRef";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        runAuthenticate(context, results);
        runTerminateAndReAuthenticate(context, results);
        runListSessions(context, results);
        runRefreshToken(context, results);
        runForceReAuthOnExpiredToken(context, results);
        runTerminateSessionByRefAndReAuthenticate(context, results);
        return results;
    }

    private void runAuthenticate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().authenticate();
            String nip = context.nipIdentifier();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_AUTHENTICATED, NAME, nip);
            }
            results.add(RunResult.ok(NAME, OP_AUTHENTICATE, elapsed(start), NIP_PREFIX + nip));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_AUTHENTICATE, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runTerminateAndReAuthenticate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().terminateAuth();
            LOGGER.info(LOG_TERMINATED, NAME);
            results.add(RunResult.ok(NAME, OP_TERMINATE, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_TERMINATE, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        long reAuthStart = System.currentTimeMillis();
        try {
            context.client().authenticate();
            LOGGER.info(LOG_RE_AUTHENTICATED, NAME);
            results.add(RunResult.ok(NAME, OP_RE_AUTHENTICATE, elapsed(reAuthStart),
                    NIP_PREFIX + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_RE_AUTHENTICATE, elapsed(reAuthStart),
                    errorMessage(exception)));
        }
    }

    private void runListSessions(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationList list = new AuthClient(KsefClientInternals.runtime(context.client())).listSessions();
            int count = list.items().size();
            LOGGER.info(LOG_ACTIVE_SESSIONS, NAME, count);
            results.add(RunResult.ok(NAME, OP_LIST_SESSIONS, elapsed(start),
                    count + SESSIONS_LABEL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST_SESSIONS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    /**
     * Tears down the current SDK-managed session, runs a manual flow that
     * captures the refresh token, refreshes it, then restores a normally-
     * managed session for subsequent runners. The manual flow is needed because
     * {@code KsefClient.authenticate()} auto-redeems and discards the refresh
     * token internally.
     */
    private void runRefreshToken(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        try {
            client.terminateAuth();

            AuthenticationTokens tokens = manualAuthenticate(context);
            if (tokens.refreshToken() == null || tokens.refreshToken().token() == null) {
                throw new IllegalStateException(ERR_NO_REFRESH_TOKEN);
            }

            AuthenticationTokenRefresh refreshed = new AuthClient(KsefClientInternals.runtime(client)).refreshToken(
                    tokens.refreshToken().token());
            OffsetDateTime validUntil = refreshed.accessToken() != null
                    ? refreshed.accessToken().validUntil() : null;
            LOGGER.info(LOG_REFRESH_OK, NAME, validUntil);
            results.add(RunResult.ok(NAME, OP_REFRESH_TOKEN, elapsed(start),
                    VALID_UNTIL_PREFIX + validUntil));

            new AuthClient(KsefClientInternals.runtime(client)).terminateCurrentSession();
            client.authenticate();
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REFRESH_TOKEN, elapsed(start),
                    errorMessage(exception)));
            tryRecoverAuth(client);
        }
    }

    /**
     * Swaps the JWT for a syntactically-bogus one (without changing expiry) and
     * issues an authenticated GET. The next request will get HTTP 401 from the
     * server, which the SDK should transparently recover from via
     * {@code reauthenticate()}.
     */
    private void runForceReAuthOnExpiredToken(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        try {
            SessionContext sessionContext = KsefClientInternals.runtime(client).sessionContext();
            OffsetDateTime fakeExpiry = OffsetDateTime.now().plusHours(FUTURE_EXPIRY_HOURS);
            sessionContext.updateAccessToken(INVALID_JWT, fakeExpiry);

            ContextLimits limits = client.limits().getContextLimits();
            boolean recovered = limits != null;
            LOGGER.info(LOG_FORCE_REAUTH_OK, NAME, recovered);
            results.add(RunResult.ok(NAME, OP_FORCE_REAUTH, elapsed(start),
                    AUTO_REFRESH_SUCCEEDED));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_FORCE_REAUTH, elapsed(start),
                    errorMessage(exception)));
            tryRecoverAuth(client);
        }
    }

    /**
     * Terminates the current session by its reference number (alternative to
     * the {@code /current} endpoint exercised by {@code terminateAuth}) and
     * re-authenticates. Uses {@code reauthenticate()} (not {@code authenticate()})
     * because the SDK's {@code authenticated} flag is still true after a
     * terminate-by-ref bypass; {@code reauthenticate()} clears state first.
     */
    private void runTerminateSessionByRefAndReAuthenticate(DemoContext context,
                                                           List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        String currentRef;
        try {
            AuthenticationList list = new AuthClient(KsefClientInternals.runtime(client)).listSessions();
            currentRef = findCurrentSessionRef(list);
            new AuthClient(KsefClientInternals.runtime(client)).terminateSession(currentRef);
            LOGGER.info(LOG_TERMINATED_BY_REF, NAME, currentRef);
            results.add(RunResult.ok(NAME, OP_TERMINATE_BY_REF, elapsed(start),
                    REF_PREFIX + currentRef));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_TERMINATE_BY_REF, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        long reAuthStart = System.currentTimeMillis();
        try {
            client.reauthenticate();
            LOGGER.info(LOG_RE_AUTHENTICATED_AFTER_BY_REF, NAME);
            results.add(RunResult.ok(NAME, OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF,
                    elapsed(reAuthStart), NIP_PREFIX + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF,
                    elapsed(reAuthStart), errorMessage(exception)));
        }
    }

    private static String findCurrentSessionRef(AuthenticationList list) {
        for (AuthenticationListItem item : list.items()) {
            if (Boolean.TRUE.equals(item.current())) {
                return item.referenceNumber();
            }
        }
        throw new IllegalStateException(ERR_NO_CURRENT_SESSION);
    }

    private static AuthenticationTokens manualAuthenticate(DemoContext context) {
        KsefClient client = context.client();
        AuthClient authClient = new AuthClient(KsefClientInternals.runtime(client));

        PublicKey tokenKey = fetchTokenEncryptionKey(client);
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithToken(challenge, context.ksefToken(),
                context.nipIdentifier(), tokenKey);
        pollAuthStatus(authClient, KsefClientInternals.runtime(client).sessionContext().referenceNumber());
        return authClient.redeemTokens();
    }

    private static PublicKey fetchTokenEncryptionKey(KsefClient client) {
        PublicKeyCertificate certificate = new SecurityClient(KsefClientInternals.runtime(client)).getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().contains(PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_PUBLIC_KEY));
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(certificate.certificate()));
            return x509.getPublicKey();
        } catch (java.security.cert.CertificateException exception) {
            throw new IllegalStateException(ERR_CERT_PARSE, exception);
        }
    }

    private static void pollAuthStatus(AuthClient authClient, String referenceNumber) {
        for (int attempt = 0; attempt < AUTH_POLL_MAX_ATTEMPTS; attempt++) {
            sleep();
            AuthenticationStatus status = authClient.getStatus(referenceNumber);
            if (status.status() != null && status.status().code() == STATUS_CODE_OK) {
                return;
            }
        }
        throw new IllegalStateException(ERR_AUTH_TIMEOUT);
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
     * Best-effort recovery so subsequent runners still have a session.
     * Swallows failures — downstream runners will report their own.
     */
    private static void tryRecoverAuth(KsefClient client) {
        try {
            client.reauthenticate();
        } catch (Exception ignored) {
            // intentionally ignored
        }
    }
}
