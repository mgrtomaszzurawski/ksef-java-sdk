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
    private static final String SKIP_NO_CERT = "requires qualified certificate";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // XAdES — skipped, requires qualified certificate
        results.add(RunResult.skip(NAME, OP_AUTH_XADES, SKIP_NO_CERT));

        // 1. Request challenge
        String challenge = runChallenge(context, results);
        if (challenge == null) {
            return results;
        }

        // 2. Authenticate with token
        String authRef = runAuthenticateWithToken(context, challenge, results);
        if (authRef == null) {
            return results;
        }

        // 3. Redeem tokens
        String refreshToken = runRedeemTokens(context, results);
        if (refreshToken == null) {
            return results;
        }

        // 4. List sessions
        runListSessions(context, results);

        // 5. Get auth status
        runGetStatus(context, authRef, results);

        // 6. Refresh token
        runRefreshToken(context, refreshToken, results);

        return results;
    }

    private String runChallenge(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationChallengeResponseRaw response = context.client().auth().requestChallenge();
            String challenge = response.getChallenge();
            LOG.info("[{}] challenge: {}", NAME, challenge);
            results.add(RunResult.ok(NAME, OP_CHALLENGE, elapsed(start), "challenge=" + challenge));
            return challenge;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CHALLENGE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runAuthenticateWithToken(DemoContext context, String challenge, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationInitResponseRaw response = context.client().auth()
                    .authenticateWithToken(challenge, context.ksefToken(),
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

    private void runGetStatus(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().auth().getStatus(referenceNumber);
            LOG.info("[{}] auth status for {}: code={}", NAME, referenceNumber,
                    response.getStatus() != null ? response.getStatus().getCode() : "null");
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start), errorMessage(exception)));
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
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
