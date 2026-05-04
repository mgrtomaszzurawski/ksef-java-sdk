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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession;
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
 *   <li>{@code listSessions} — query active auth sessions via
 *       {@link KsefClient#listAuthSessions()}</li>
 *   <li>{@code refreshToken} — renew access token via
 *       {@link KsefClient#refreshAuthToken()}</li>
 *   <li>{@code terminateSessionByRef} — terminate a specific session by reference
 *       (vs. the {@code /current} endpoint exercised by {@code terminateAuth})
 *       via {@link KsefClient#terminateAuthSession(String)}</li>
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
    private static final String OP_TERMINATE_BY_REF = "terminateSessionByRef";
    private static final String OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF = "reAuthAfterTerminateByRef";

    private static final String NIP_PREFIX = "NIP=";
    private static final String SESSIONS_LABEL = " sessions";
    private static final String REF_PREFIX = "ref=";
    private static final String REFRESH_OK_DETAIL = "refresh-token flow succeeded";

    private static final String ERR_NO_CURRENT_SESSION = "No current session in listSessions response";

    private static final String LOG_AUTHENTICATED = "[{}] authenticated as NIP {}";
    private static final String LOG_TERMINATED = "[{}] auth session terminated";
    private static final String LOG_RE_AUTHENTICATED = "[{}] re-authenticated after terminate";
    private static final String LOG_ACTIVE_SESSIONS = "[{}] active auth sessions: {}";
    private static final String LOG_REFRESH_OK = "[{}] refreshed access token via refresh-token flow";
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
            List<AuthSession> sessions = context.client().listAuthSessions();
            int count = sessions.size();
            LOGGER.info(LOG_ACTIVE_SESSIONS, NAME, count);
            results.add(RunResult.ok(NAME, OP_LIST_SESSIONS, elapsed(start),
                    count + SESSIONS_LABEL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST_SESSIONS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    /**
     * Exercises the public {@link KsefClient#refreshAuthToken()} endpoint. The
     * SDK auto-captured the refresh token during the last {@code authenticate()}
     * redeem, so a single call here proves the refresh-token endpoint works.
     */
    private void runRefreshToken(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        try {
            client.refreshAuthToken();
            LOGGER.info(LOG_REFRESH_OK, NAME);
            results.add(RunResult.ok(NAME, OP_REFRESH_TOKEN, elapsed(start),
                    REFRESH_OK_DETAIL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REFRESH_TOKEN, elapsed(start),
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
            List<AuthSession> sessions = client.listAuthSessions();
            currentRef = findCurrentSessionRef(sessions);
            client.terminateAuthSession(currentRef);
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

    private static String findCurrentSessionRef(List<AuthSession> sessions) {
        for (AuthSession session : sessions) {
            if (session.current()) {
                return session.referenceNumber();
            }
        }
        throw new IllegalStateException(ERR_NO_CURRENT_SESSION);
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
