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

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationChallenge;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationInit;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationTokens;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // XAdES — test if certificate available
        if (context.hasCertificate()) {
            runAuthenticateWithXades(context, results);
        } else {
            results.add(RunResult.skip(NAME, OP_AUTH_XADES, SKIP_NO_CERT));
        }

        // 1. Request challenge (for token-based auth — the main session)
        AuthenticationChallenge challengeResponse = runChallenge(context, results);
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

    /**
     * Test XAdES authentication in a separate cycle: challenge → sign → authenticate → terminate.
     * This creates a temporary session that is terminated immediately — it does NOT affect
     * the main token-based session used by other runners.
     */
    private void runAuthenticateWithXades(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            // Get a fresh challenge for XAdES
            AuthenticationChallenge challenge = context.client().auth().requestChallenge();
            LOG.info("[{}] XAdES challenge: {}", NAME, challenge.challenge());

            // Sign and authenticate
            AuthenticationInit response = context.client().auth()
                    .authenticateWithXades(challenge.challenge(),
                            context.certificate(), context.privateKey(), context.nipIdentifier());
            String refNum = response.referenceNumber();
            LOG.info("[{}] XAdES authenticated, ref={}", NAME, refNum);

            // Poll until auth is ready, then redeem + terminate
            List<RunResult> pollResults = new ArrayList<>();
            if (pollAuthStatus(context, refNum, pollResults)) {
                context.client().auth().redeemTokens();
                LOG.info("[{}] XAdES tokens redeemed", NAME);
                context.client().auth().terminateCurrentSession();
                LOG.info("[{}] XAdES session terminated", NAME);
            }

            results.add(RunResult.ok(NAME, OP_AUTH_XADES, elapsed(start), "ref=" + refNum));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_AUTH_XADES, elapsed(start), errorMessage(exception)));
        }
    }

    private AuthenticationChallenge runChallenge(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationChallenge response = context.client().auth().requestChallenge();
            String challenge = response.challenge();
            LOG.info("[{}] challenge received", NAME);
            LOG.debug("[{}] challenge: {}, clientIp: {}", NAME, challenge, response.clientIp());
            results.add(RunResult.ok(NAME, OP_CHALLENGE, elapsed(start), "challenge=" + challenge));
            return response;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CHALLENGE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runAuthenticateWithToken(DemoContext context,
                                            AuthenticationChallenge challengeResponse,
                                            List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationInit response = context.client().auth()
                    .authenticateWithToken(challengeResponse, context.ksefToken(),
                            context.nipIdentifier(), context.ksefPublicKey());
            String refNum = response.referenceNumber();
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
                Integer code = response.status() != null ? response.status().code() : null;
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
            AuthenticationTokens response = context.client().auth().redeemTokens();
            String accessValid = response.accessToken().validUntil() != null
                    ? response.accessToken().validUntil().toString() : "unknown";
            LOG.info("[{}] tokens redeemed, access valid until {}", NAME, accessValid);
            results.add(RunResult.ok(NAME, OP_REDEEM, elapsed(start), "validUntil=" + accessValid));
            return response.refreshToken() != null ? response.refreshToken().token() : null;
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

}
