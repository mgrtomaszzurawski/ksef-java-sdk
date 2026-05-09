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
 * Runner for authentication. Drives the SDK's lazy-auth lifecycle by
 * issuing operations that require authentication; the SDK runs the full
 * challenge-response flow internally on the first such call (challenge
 * → encrypt/sign → poll → redeem).
 *
 * <p>Additional auth-area operations exercised here:
 * <ul>
 *   <li>{@code listSessions} — query active auth sessions via
 *       {@code client.auth().streamSessions()}</li>
 *   <li>{@code terminateSessionByRef} — terminate a specific session by
 *       reference (vs. the {@code /current} endpoint exercised by
 *       {@code terminateAuth}) via
 *       {@code client.auth().terminateSession(String)}</li>
 *   <li>{@code terminateAuth} — terminate the current session via
 *       {@code client.auth().terminate()}, then trigger lazy re-auth on
 *       the next operation</li>
 * </ul>
 */
public final class AuthRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthRunner.class);
    private static final String NAME = "auth";
    private static final String OP_AUTHENTICATE = "authenticate";
    private static final String OP_TERMINATE = "terminateAuth";
    private static final String OP_RE_AUTHENTICATE = "reAuthenticate";
    private static final String OP_LIST_SESSIONS = "listSessions";
    private static final String OP_TERMINATE_BY_REF = "terminateSessionByRef";
    private static final String OP_RE_AUTHENTICATE_AFTER_TERMINATE_BY_REF = "reAuthAfterTerminateByRef";

    private static final String NIP_PREFIX = "NIP=";
    private static final String SESSIONS_LABEL = " sessions";
    private static final String REF_PREFIX = "ref=";

    private static final String ERR_NO_CURRENT_SESSION = "No current session in listSessions response";

    private static final String LOG_AUTHENTICATED = "[{}] authenticated as NIP {}";
    private static final String LOG_TERMINATED = "[{}] auth session terminated";
    private static final String LOG_RE_AUTHENTICATED = "[{}] re-authenticated after terminate";
    private static final String LOG_ACTIVE_SESSIONS = "[{}] active auth sessions: {}";
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
        runTerminateSessionByRefAndReAuthenticate(context, results);
        return results;
    }

    private void runAuthenticate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            // Drive lazy auth via any authenticated read — streamSessions()
            // hits /v2/auth/sessions which forces the full challenge flow.
            context.client().auth().streamSessions().findAny();
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
            context.client().auth().terminate();
            LOGGER.info(LOG_TERMINATED, NAME);
            results.add(RunResult.ok(NAME, OP_TERMINATE, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_TERMINATE, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        long reAuthStart = System.currentTimeMillis();
        try {
            // Lazy re-auth — any authenticated read after terminate() drives
            // the full challenge-response cycle again.
            context.client().auth().streamSessions().findAny();
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
            List<AuthSession> sessions = context.client().auth().streamSessions().toList();
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
     * Terminates the current session by its reference number (alternative to
     * the {@code /current} endpoint exercised by {@code terminateAuth}) and
     * lets lazy auth bring up a fresh session on the next operation.
     */
    private void runTerminateSessionByRefAndReAuthenticate(DemoContext context,
                                                           List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefClient client = context.client();
        String currentRef;
        try {
            List<AuthSession> sessions = client.auth().streamSessions().toList();
            currentRef = findCurrentSessionRef(sessions);
            client.auth().terminateSession(currentRef);
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
            // Lazy re-auth after terminate-by-ref.
            client.auth().streamSessions().findAny();
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
}
