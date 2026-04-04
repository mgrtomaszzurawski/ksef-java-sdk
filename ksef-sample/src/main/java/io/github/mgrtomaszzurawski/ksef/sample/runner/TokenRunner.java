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

import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for TokenClient operations. Generates a token, verifies it, lists all tokens,
 * then revokes the generated token — fully self-cleaning.
 */
public final class TokenRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TokenRunner.class);
    private static final String NAME = "token";
    private static final String OP_GENERATE = "generate";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_LIST = "list";
    private static final String OP_REVOKE = "revoke";
    private static final String TOKEN_DESCRIPTION = "SDK Demo Token";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Generate token
        String tokenRef = runGenerate(context, results);
        if (tokenRef == null) {
            return results;
        }

        // 2. Get status
        runGetStatus(context, tokenRef, results);

        // 3. List tokens
        runList(context, results);

        // 4. Revoke (cleanup)
        runRevoke(context, tokenRef, results);

        return results;
    }

    private String runGenerate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            GenerateTokenRequestRaw request = new GenerateTokenRequestRaw()
                    .description(TOKEN_DESCRIPTION);
            GenerateTokenResponseRaw response = context.client().tokens().generate(request);
            String refNum = response.getReferenceNumber();
            LOG.info("[{}] generated token ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GENERATE, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GENERATE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private void runGetStatus(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            TokenStatusResponseRaw response = context.client().tokens().getStatus(referenceNumber);
            LOG.info("[{}] token status: ref={}, status={}", NAME, referenceNumber, response.getStatus());
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runList(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            QueryTokensResponseRaw response = context.client().tokens().list();
            int tokenCount = response.getTokens() != null ? response.getTokens().size() : 0;
            LOG.info("[{}] listed {} tokens", NAME, tokenCount);
            results.add(RunResult.ok(NAME, OP_LIST, elapsed(start), tokenCount + " tokens"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST, elapsed(start), errorMessage(exception)));
        }
    }

    private void runRevoke(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().tokens().revoke(referenceNumber);
            LOG.info("[{}] revoked token ref={}", NAME, referenceNumber);
            results.add(RunResult.ok(NAME, OP_REVOKE, elapsed(start), "revoked ref=" + referenceNumber));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE, elapsed(start), errorMessage(exception)));
        }
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static String errorMessage(Exception exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
