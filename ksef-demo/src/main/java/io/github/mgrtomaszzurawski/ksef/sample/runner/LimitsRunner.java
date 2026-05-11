/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for LimitsClient operations.
 */
public final class LimitsRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitsRunner.class);
    private static final String NAME = "limits";
    private static final String OP_CONTEXT = "getContextLimits";
    private static final String OP_SUBJECT = "getSubjectLimits";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        long start = System.currentTimeMillis();
        try {
            context.client().limits().getContextLimits();
            LOGGER.info("[{}] context limits fetched", NAME);
            results.add(RunResult.ok(NAME, OP_CONTEXT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CONTEXT, elapsed(start), errorMessage(exception)));
        }

        start = System.currentTimeMillis();
        try {
            context.client().limits().getSubjectLimits();
            LOGGER.info("[{}] subject limits fetched", NAME);
            results.add(RunResult.ok(NAME, OP_SUBJECT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SUBJECT, elapsed(start), errorMessage(exception)));
        }

        return results;
    }
}
