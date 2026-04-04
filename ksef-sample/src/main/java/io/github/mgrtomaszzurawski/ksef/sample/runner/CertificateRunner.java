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

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for CertificateClient operations. Tests getLimits with token auth,
 * then switches to XAdES auth session for operations that require it
 * (enrollmentData, query). Re-authenticates with token afterward.
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String OP_ENROLL = "enroll";
    private static final String SKIP_ENROLL = "requires CSR generation";
    private static final String SKIP_NO_CERT = "no certificate available";
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

        // 1. Get limits (works with token auth)
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getLimits();
            LOG.info("[{}] certificate limits: canRequest={}", NAME, response.getCanRequest());
            results.add(RunResult.ok(NAME, OP_GET_LIMITS, elapsed(start),
                    "canRequest=" + response.getCanRequest()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_LIMITS, elapsed(start), errorMessage(exception)));
        }

        // 2-3. Enrollment data + query — require XAdES auth session
        if (context.hasCertificate()) {
            runWithXadesSession(context, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_ENROLLMENT_DATA, SKIP_NO_CERT));
            results.add(RunResult.skip(NAME, OP_QUERY, SKIP_NO_CERT));
        }

        // 4. Enroll — skipped (needs CSR generation, out of scope for demo)
        results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_ENROLL));

        return results;
    }

    /**
     * Switch to XAdES session, run cert ops, then re-auth with token.
     */
    private void runWithXadesSession(DemoContext context, List<RunResult> results) {
        try {
            // Authenticate with XAdES
            AuthenticationChallengeResponseRaw challenge = context.client().auth().requestChallenge();
            AuthenticationInitResponseRaw authResp = context.client().auth()
                    .authenticateWithXades(challenge.getChallenge(),
                            context.certificate(), context.privateKey(), context.nipIdentifier());
            pollUntilReady(context, authResp.getReferenceNumber());
            context.client().auth().redeemTokens();
            LOG.info("[{}] switched to XAdES session for cert ops", NAME);

            // Run cert ops under XAdES session
            runGetEnrollmentData(context, results);
            runQuery(context, results);

            // Terminate XAdES session
            context.client().auth().terminateCurrentSession();
            LOG.info("[{}] XAdES session terminated", NAME);

            // Re-authenticate with token
            challenge = context.client().auth().requestChallenge();
            context.client().auth().authenticateWithToken(
                    challenge, context.ksefToken(), context.nipIdentifier(), context.ksefPublicKey());
            pollUntilReady(context, context.client().sessionContext().referenceNumber());
            context.client().auth().redeemTokens();
            LOG.info("[{}] re-authenticated with token", NAME);

        } catch (Exception exception) {
            LOG.error("[{}] XAdES session switch failed: {}", NAME, errorMessage(exception));
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, 0, errorMessage(exception)));
            results.add(RunResult.skip(NAME, OP_QUERY, "XAdES session failed"));
        }
    }

    private void runGetEnrollmentData(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getEnrollmentData();
            LOG.info("[{}] enrollment data: commonName={}, country={}", NAME,
                    response.getCommonName(), response.getCountryName());
            results.add(RunResult.ok(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start),
                    "cn=" + response.getCommonName()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQuery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().query(new QueryCertificatesRequestRaw());
            int count = response.getCertificates() != null ? response.getCertificates().size() : 0;
            LOG.info("[{}] queried certificates: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY, elapsed(start), count + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY, elapsed(start), errorMessage(exception)));
        }
    }

    private void pollUntilReady(DemoContext context, String referenceNumber) throws InterruptedException {
        int delay = POLL_INITIAL_DELAY_MS;
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            var response = context.client().auth().getStatus(referenceNumber);
            Integer code = response.getStatus() != null ? response.getStatus().getCode() : null;
            if (code != null && code == AUTH_STATUS_OK) {
                return;
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }
        throw new IllegalStateException("Timeout waiting for auth status 200 for " + referenceNumber);
    }
}
