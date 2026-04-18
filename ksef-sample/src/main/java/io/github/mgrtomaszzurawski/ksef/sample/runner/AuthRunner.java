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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for authentication. Uses the high-level {@code client.authenticate()} API
 * which handles the full flow internally: challenge → encrypt/sign → poll → redeem.
 *
 * <p>After this runner completes, the KsefClient session context is populated with a valid
 * bearer token that all subsequent runners use automatically.</p>
 */
public final class AuthRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRunner.class);
    private static final String NAME = "auth";
    private static final String OP_AUTHENTICATE = "authenticate";
    private static final String OP_TERMINATE = "terminateAuth";
    private static final String OP_RE_AUTHENTICATE = "reAuthenticate";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Authenticate (challenge → encrypt/sign → poll → redeem — all inside SDK)
        runAuthenticate(context, results);

        // 2. Terminate + re-authenticate (verifies session lifecycle works end-to-end)
        runTerminateAndReAuthenticate(context, results);

        return results;
    }

    private void runAuthenticate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().authenticate();
            LOG.info("[{}] authenticated as NIP {}", NAME, context.nipIdentifier());
            results.add(RunResult.ok(NAME, OP_AUTHENTICATE, elapsed(start),
                    "NIP=" + context.nipIdentifier()));
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
                    "NIP=" + context.nipIdentifier()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_RE_AUTHENTICATE, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
