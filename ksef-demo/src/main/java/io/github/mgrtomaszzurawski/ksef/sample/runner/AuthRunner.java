/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
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
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationList;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokenRefresh;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.AuthClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
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
 * bearer token that all subsequent runners use automatically.</p>
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
 * </p>
 */
public final class AuthRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRunner.class);
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

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Authenticate (challenge → encrypt/sign → poll → redeem — all inside SDK)
        runAuthenticate(context, results);

        // 2. Terminate + re-authenticate (verifies session lifecycle works end-to-end)
        runTerminateAndReAuthenticate(context, results);

        // 3. List active auth sessions
        runListSessions(context, results);

        // 4. Refresh access token via refresh token (uses manual auth flow internally
        //    because the SDK auto-redeems the refresh token during authenticate())
        runRefreshToken(context, results);

        // 5. Force a 401 by corrupting the JWT, then perform a GET — SDK should auto-refresh
        runForceReAuthOnExpiredToken(context, results);

        // 6. Terminate the current session by its reference number (alternative to
        //    the /current endpoint exercised by terminateAuth) and re-authenticate
        runTerminateSessionByRefAndReAuthenticate(context, results);

        return results;
    }

    private void runAuthenticate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().authenticate();
            LOG.info("[{}] authenticated as NIP {}", NAME, context.nipIdentifier());
            results.add(RunResult.ok(NAME, OP_AUTHENTICATE, elapsed(start),
                    NIP_PREFIX + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_AUTHENTICATE, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runTerminateAndReAuthenticate(DemoContext context, List<RunResult> results) {
        // Terminate
        long start = System.currentTimeMillis();
        try {
            context.client().terminateAuth();
            LOG.info("[{}] auth session terminated", NAME);
            results.add(RunResult.ok(NAME, OP_TERMINATE, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_TERMINATE, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        // Re-authenticate (proves full lifecycle: auth → terminate → auth again)
        start = System.currentTimeMillis();
        try {
            context.client().authenticate();
            LOG.info("[{}] re-authenticated after terminate", NAME);
            results.add(RunResult.ok(NAME, OP_RE_AUTHENTICATE, elapsed(start),
                    NIP_PREFIX + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_RE_AUTHENTICATE, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runListSessions(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationList list = context.client().auth().listSessions();
            int count = list.items().size();
            LOG.info("[{}] active auth sessions: {}", NAME, count);
            results.add(RunResult.ok(NAME, OP_LIST_SESSIONS, elapsed(start),
                    count + " sessions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST_SESSIONS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runRefreshToken(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        try {
            // Tear down the current session so we can run a manual flow that captures
            // the refresh token (the SDK's authenticate() redeems and discards it).
            client.terminateAuth();

            AuthenticationTokens tokens = manualAuthenticate(context);
            if (tokens.refreshToken() == null || tokens.refreshToken().token() == null) {
                throw new IllegalStateException(ERR_NO_REFRESH_TOKEN);
            }

            AuthenticationTokenRefresh refreshed = client.auth().refreshToken(
                    tokens.refreshToken().token());
            OffsetDateTime validUntil = refreshed.accessToken() != null
                    ? refreshed.accessToken().validUntil() : null;
            LOG.info("[{}] refreshed access token, validUntil={}", NAME, validUntil);
            results.add(RunResult.ok(NAME, OP_REFRESH_TOKEN, elapsed(start),
                    "validUntil=" + validUntil));

            // Tear down the manually-established session and re-authenticate via the
            // SDK so the rest of the demo runs against a normally-managed session.
            client.auth().terminateCurrentSession();
            client.authenticate();
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REFRESH_TOKEN, elapsed(start),
                    errorMessage(exception)));
            // Best-effort recovery so subsequent runners still have a session.
            tryRecoverAuth(client);
        }
    }

    private void runForceReAuthOnExpiredToken(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        try {
            SessionContext sessionContext = client.sessionContext();
            // Swap the JWT for a syntactically-bogus one without changing expiry.
            // The next authenticated request will get HTTP 401 from the server,
            // which the SDK should transparently recover from via reauthenticate().
            OffsetDateTime fakeExpiry = OffsetDateTime.now().plusHours(FUTURE_EXPIRY_HOURS);
            sessionContext.refreshToken(INVALID_JWT, fakeExpiry);

            // Any authenticated GET will do — limits.getContextLimits is cheap.
            ContextLimits limits = client.limits().getContextLimits();
            boolean recovered = limits != null;
            LOG.info("[{}] auto-reauth recovered: contextLimits retrieved={}", NAME, recovered);
            results.add(RunResult.ok(NAME, OP_FORCE_REAUTH, elapsed(start),
                    "auto-refresh succeeded"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_FORCE_REAUTH, elapsed(start),
                    errorMessage(exception)));
            // Best-effort recovery so subsequent runners still have a session.
            tryRecoverAuth(client);
        }
    }

    private void runTerminateSessionByRefAndReAuthenticate(DemoContext context,
                                                           List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        String currentRef;
        try {
            AuthenticationList list = client.auth().listSessions();
            currentRef = findCurrentSessionRef(list);
            client.auth().terminateSession(currentRef);
            LOG.info("[{}] terminated session by ref={}", NAME, currentRef);
            results.add(RunResult.ok(NAME, OP_TERMINATE_BY_REF, elapsed(start),
                    "ref=" + currentRef));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_TERMINATE_BY_REF, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        // Re-authenticate so subsequent runners (if any) have a valid session.
        start = System.currentTimeMillis();
        try {
            // The SDK's `authenticated` flag is still true (we bypassed terminateAuth),
            // so force a clean re-auth via reauthenticate() which clears state first.
            client.reauthenticate();
            LOG.info("[{}] re-authenticated after terminateSessionByRef", NAME);
            results.add(RunResult.ok(NAME, OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF,
                    elapsed(start), NIP_PREFIX + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF,
                    elapsed(start), errorMessage(exception)));
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
        AuthClient authClient = client.auth();

        PublicKey tokenKey = fetchTokenEncryptionKey(client);
        AuthenticationChallenge challenge = authClient.requestChallenge();
        authClient.authenticateWithToken(challenge, context.ksefToken(),
                context.nipIdentifier(), tokenKey);
        pollAuthStatus(authClient, client.sessionContext().referenceNumber());
        return authClient.redeemTokens();
    }

    private static PublicKey fetchTokenEncryptionKey(KsefClient client) {
        PublicKeyCertificate cert = client.security().getPublicKeyCertificates().stream()
                .filter(c -> c.usage().contains(PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_PUBLIC_KEY));
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(cert.certificate()));
            return x509.getPublicKey();
        } catch (java.security.cert.CertificateException ex) {
            throw new IllegalStateException(ERR_CERT_PARSE, ex);
        }
    }

    private static void pollAuthStatus(AuthClient authClient, String referenceNumber) {
        for (int attempt = 0; attempt < AUTH_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(AUTH_POLL_DELAY_MS);
            AuthenticationStatus status = authClient.getStatus(referenceNumber);
            if (status.status() != null && status.status().code() == STATUS_CODE_OK) {
                return;
            }
        }
        throw new IllegalStateException(ERR_AUTH_TIMEOUT);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, ex);
        }
    }

    private static void tryRecoverAuth(KsefClient client) {
        try {
            client.reauthenticate();
        } catch (Exception ignored) {
            // Best-effort — if recovery fails, downstream runners will report their own failures.
        }
    }
}
