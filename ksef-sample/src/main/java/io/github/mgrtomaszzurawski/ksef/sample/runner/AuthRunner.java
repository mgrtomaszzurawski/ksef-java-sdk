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

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for AuthClient operations. Performs the full token-based authentication flow:
 * challenge → authenticateWithToken → redeemTokens → listSessions → getStatus → refreshToken.
 *
 * <p>After this runner completes, the KsefClient session context is populated with a valid
 * bearer token that all subsequent runners use automatically.</p>
 */
public final class AuthRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRunner.class);
    private static final String NAME = "auth";
    private static final String OP_CHALLENGE = "requestChallenge";
    private static final String OP_AUTH_TOKEN = "authenticateWithToken";
    private static final String OP_REDEEM = "redeemTokens";
    private static final String OP_LIST_SESSIONS = "listSessions";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_REFRESH = "refreshToken";
    private static final String OP_AUTH_XADES = "authenticateWithXades";
    private static final String OP_POLL_STATUS = "pollAuthStatus";
    private static final String SKIP_NO_CERT = "requires qualified certificate";
    private static final int AUTH_STATUS_OK = 200;
    private static final int POLL_INITIAL_DELAY_MS = 500;
    private static final int POLL_MAX_DELAY_MS = 5000;
    private static final int POLL_TIMEOUT_MS = 30000;
    private static final int POLL_BACKOFF_MULTIPLIER = 2;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // XAdES — skipped, requires qualified certificate
        results.add(RunResult.skip(NAME, OP_AUTH_XADES, SKIP_NO_CERT));

        // 1. Request challenge
        AuthenticationChallengeResponseRaw challengeResponse = runChallenge(context, results);
        if (challengeResponse == null) {
            return results;
        }

        // 2. Authenticate with token
        String authRef = runAuthenticateWithToken(context, challengeResponse, results);
        if (authRef == null) {
            return results;
        }

        // 3. Poll auth status until ready
        if (!pollAuthStatus(context, authRef, results)) {
            return results;
        }

        // 4. Redeem tokens
        String refreshToken = runRedeemTokens(context, results);
        if (refreshToken == null) {
            return results;
        }

        // 5. List sessions
        runListSessions(context, results);

        // 6. Refresh token
        runRefreshToken(context, refreshToken, results);

        return results;
    }

    private AuthenticationChallengeResponseRaw runChallenge(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationChallengeResponseRaw response = context.client().auth().requestChallenge();
            String challenge = response.getChallenge();
            LOG.info("[{}] challenge: {}, clientIp: {}", NAME, challenge, response.getClientIp());
            results.add(RunResult.ok(NAME, OP_CHALLENGE, elapsed(start), "challenge=" + challenge));
            return response;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CHALLENGE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runAuthenticateWithToken(DemoContext context,
                                            AuthenticationChallengeResponseRaw challengeResponse,
                                            List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationInitResponseRaw response = context.client().auth()
                    .authenticateWithToken(challengeResponse, context.ksefToken(),
                            context.nipIdentifier(), context.ksefPublicKey());
            String refNum = response.getReferenceNumber();
            LOG.info("[{}] authenticated, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_AUTH_TOKEN, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_AUTH_TOKEN, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private boolean pollAuthStatus(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        try {
            while (elapsed(start) < POLL_TIMEOUT_MS) {
                var response = context.client().auth().getStatus(referenceNumber);
                Integer code = response.getStatus() != null ? response.getStatus().getCode() : null;
                LOG.info("[{}] auth status for {}: code={}", NAME, referenceNumber, code);
                if (code != null && code == AUTH_STATUS_OK) {
                    results.add(RunResult.ok(NAME, OP_POLL_STATUS, elapsed(start),
                            "ready after " + elapsed(start) + "ms"));
                    return true;
                }
                Thread.sleep(delay);
                delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
            }
            results.add(RunResult.fail(NAME, OP_POLL_STATUS, elapsed(start),
                    "Timeout waiting for auth status 200"));
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            results.add(RunResult.fail(NAME, OP_POLL_STATUS, elapsed(start), "Interrupted"));
            return false;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_POLL_STATUS, elapsed(start), errorMessage(exception)));
            return false;
        }
    }

    private String runRedeemTokens(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationTokensResponseRaw response = context.client().auth().redeemTokens();
            String accessValid = response.getAccessToken().getValidUntil() != null
                    ? response.getAccessToken().getValidUntil().toString() : "unknown";
            LOG.info("[{}] tokens redeemed, access valid until {}", NAME, accessValid);
            results.add(RunResult.ok(NAME, OP_REDEEM, elapsed(start), "validUntil=" + accessValid));
            return response.getRefreshToken() != null ? response.getRefreshToken().getToken() : null;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REDEEM, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private void runListSessions(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().auth().listSessions();
            LOG.info("[{}] active sessions listed", NAME);
            results.add(RunResult.ok(NAME, OP_LIST_SESSIONS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST_SESSIONS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runRefreshToken(DemoContext context, String refreshToken, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().auth().refreshToken(refreshToken);
            LOG.info("[{}] token refreshed", NAME);
            results.add(RunResult.ok(NAME, OP_REFRESH, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REFRESH, elapsed(start), errorMessage(exception)));
        }
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static String errorMessage(Exception exception) {
        if (exception instanceof KsefException ksefEx && ksefEx.responseBody() != null) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage()
                    + " | body: " + ksefEx.responseBody();
        }
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
