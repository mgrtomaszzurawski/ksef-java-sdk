/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for Tokens operations. Generates a token, verifies it, lists all tokens,
 * then revokes the generated token — fully self-cleaning.
 */
public final class TokenRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRunner.class);
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

        String tokenRef = runGenerate(context, results);
        if (tokenRef == null) {
            return results;
        }

        runGetStatus(context, tokenRef, results);

        runList(context, results);

        runRevoke(context, tokenRef, results);

        return results;
    }

    private String runGenerate(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            TokenGenerateBuilder tokenBuilder = TokenGenerateBuilder.create(TOKEN_DESCRIPTION)
                    .invoiceRead();
            GenerateTokenResult response = context.client().tokens().generate(tokenBuilder.build());
            String refNum = response.referenceNumber();
            LOGGER.info("[{}] generated token ref={}", NAME, refNum);
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
            TokenDetail response = context.client().tokens().getStatus(referenceNumber);
            LOGGER.info("[{}] token status: ref={}, status={}", NAME, referenceNumber, response.status());
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runList(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            TokenList response = context.client().tokens().queryTokens(TokenQueryBuilder.create().build());
            int tokenCount = response.tokens() != null ? response.tokens().size() : 0;
            LOGGER.info("[{}] listed {} tokens", NAME, tokenCount);
            results.add(RunResult.ok(NAME, OP_LIST, elapsed(start), tokenCount + " tokens"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_LIST, elapsed(start), errorMessage(exception)));
        }
    }

    private void runRevoke(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().tokens().revoke(referenceNumber);
            LOGGER.info("[{}] revoked token ref={}", NAME, referenceNumber);
            results.add(RunResult.ok(NAME, OP_REVOKE, elapsed(start), "revoked ref=" + referenceNumber));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE, elapsed(start), errorMessage(exception)));
        }
    }
}
